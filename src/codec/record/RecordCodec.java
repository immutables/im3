package io.immutables.codec.record;

import io.immutables.codec.*;
import io.immutables.common.Unreachable;
import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked") // TODO explain
final class RecordCodec<T> extends CaseCodec<T, In, Out> {
	private final String[] componentNames;
	private final Type[] componentTypes;
	private final Codec<Object, In, Out>[] componentCodecs;
	private final Method[] componentAccessors;
	private final Constructor<?> canonicalConstructor;

	// we don't impose volatile read barrier on lazy names, we expect it's ok
	// to compute more than once under race conditions, no harm
	// is expected as these instances are to be immutable and suitable
	// for this codec for medium
	private @Null NameIndex names;

	RecordCodec(Type type, Class<?> raw, Codec.Lookup<In, Out> lookup) {
		assert raw.isRecord();

		var arguments = mapTypeArguments(raw, type);
		var components = raw.getRecordComponents();

		var length = components.length;
		componentNames = new String[length];
		componentTypes = new Type[components.length];
		componentCodecs = (Codec<Object, In, Out>[]) new Codec<?, ?, ?>[length];
		componentAccessors = new Method[length];
		var componentRawTypes = new Class<?>[length];

		for (int i = 0; i < length; i++) {
			var c = components[i];
			componentNames[i] = c.getName();
			// currently it will blow up on unsupported type
			// maybe declare special kind of Exception and handle it in a specific way
			// like NonCompliantRecordDefinition
			var componentType = Types.resolveTypeArguments(c.getGenericType(), arguments);

			componentTypes[i] = componentType;
			componentRawTypes[i] = c.getType();
			componentAccessors[i] = c.getAccessor();

			var codec = lookup.get(componentType);
			if (RecordsFactory.metadata.isNullableComponent(c)) {
				codec = Codecs.nullSafe(codec);
			}
			componentCodecs[i] = codec;
		}

		canonicalConstructor = matchConstructor(raw, componentRawTypes);
	}

	public boolean mayConform(In in) throws IOException {
		// only for structs
		if (in.peek() != In.At.Struct) return false;

		if (names == null) names = in.index(componentNames);

		int length = componentNames.length;
		var componentPresent = new boolean[length];

		in.beginStruct(names);

		while (in.hasNext()) {
			int f = in.takeField();
			if (f >= 0) {
				componentPresent[f] = true;
				in.skip();
			} else {
				return false;
			}
		}
		in.endStruct();

		for (int i = 0; i < length; i++) {
			if (!componentPresent[i]) {
				if (componentCodecs[i] instanceof DefaultingCodec<Object, In, Out> defaulting
					&& defaulting.providesDefault()) continue;
				return false;
			}
		}
		// no unknown, all non-default are present
		return true;
	}

	private Map<TypeVariable<?>, Type> mapTypeArguments(Class<?> raw, Type type) {
		if (type instanceof ParameterizedType p) {
			var arguments = p.getActualTypeArguments();
			var parameters = raw.getTypeParameters();

			assert p.getRawType().equals(raw);
			assert parameters.length == arguments.length;

			var map = new HashMap<TypeVariable<?>, Type>(4);
			for (int i = 0; i < arguments.length; i++) {
				map.put(parameters[i], arguments[i]);
			}

			return map; // frankly, no need to wrap in immutable map
		} else if (raw.equals(type)) {
			assert raw.getTypeParameters().length == 0;
			return Map.of();
		} else throw new AssertionError("Unsupported type: " + type);
	}

	public void encode(Out out, T instance) throws IOException {
		if (names == null) names = out.index(componentNames);

		var length = componentNames.length;

		out.beginStruct(names);
		for (int i = 0; i < length; i++) {
			var value = Reflect.getValue(componentAccessors[i], instance);
			var codec = componentCodecs[i];

			if (codec instanceof DefaultingCodec<Object, In, Out> defaulting
				&& defaulting.canSkip(out, value)) continue;

			out.putField(i);
			codec.encode(out, value);
		}
		out.endStruct();
	}

	public @Null T decode(In in) throws IOException {
		if (names == null) names = in.index(componentNames);

		var length = componentNames.length;
		var componentValues = new Object[length];
		var componentPresent = new boolean[length];

		in.beginStruct(names);

		boolean componentFailed = false;
		while (in.hasNext()) {
			int f = in.takeField();
			if (f >= 0) {
				componentPresent[f] = true;
				componentValues[f] = componentCodecs[f].decode(in);

				componentFailed |= in.clearInstanceFailed();
			} else {
				in.unrecognized();
				in.skip();
			}
		}

		in.endStruct();

		// this block handles missing components,
		// either providing default value if possible or admitting that it's
		// indeed missing and current record instance cannot be created
		for (int i = 0; i < length; i++) {
			if (!componentPresent[i]) {
				if (componentCodecs[i] instanceof DefaultingCodec<Object, In, Out> defaulting
					&& defaulting.providesDefault()) {
					componentValues[i] = defaulting.getDefault();
				} else {
					in.missing(componentNames[i], componentTypes[i]);
					componentFailed = true;
				}
			}
		}

		if (componentFailed) {
			in.failInstance();
			return null;
		}

		var instance = Reflect.newInstance(canonicalConstructor, componentValues);

		return (T) instance;
	}

	private static Constructor<?> matchConstructor(Class<?> record, Class<?>[] components) {
		for (var c : record.getDeclaredConstructors()) {
			if (Arrays.equals(c.getParameterTypes(), components)) {
				return c;
			}
		}
		throw Unreachable.contractual();
	}
}
