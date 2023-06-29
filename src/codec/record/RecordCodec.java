package io.immutables.codec.record;

import io.immutables.codec.*;
import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.*;

@SuppressWarnings("unchecked") // TODO explain
final class RecordCodec<T> extends CaseCodec<T, In, Out> implements Expecting {
	private final String[] componentNames;
	private final Type[] componentTypes;
	private final Codec<Object, In, Out>[] componentCodecs;
	private final Method[] componentAccessors;
	private final Constructor<?> canonicalConstructor;
	private final Type type;

	// we don't impose volatile read barrier on lazy names, we expect it's ok
	// to compute more than once under race conditions, no harm
	// is expected as these instances are to be immutable and suitable
	// for this codec for medium
	private @Null NameIndex names;

	RecordCodec(Type type, Class<?> raw, Codec.Lookup<In, Out> lookup) {
		this.type = type;
		assert raw.isRecord();

		var arguments = Types.mapArguments(raw, type);
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
			var componentType = Types.resolveArguments(c.getGenericType(), arguments);

			componentTypes[i] = componentType;
			componentRawTypes[i] = c.getType();
			componentAccessors[i] = c.getAccessor();

			var codec = lookup.get(componentType);
			if (Providers.metadata().isNullableComponent(c)) {
				codec = Codecs.nullSafe(codec);
			}
			componentCodecs[i] = codec;
		}

		canonicalConstructor = Reflect.getCanonicalConstructor(raw);
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
				in.unknown();
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
					in.missing(componentNames[i], componentTypes[i].getTypeName());
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

	public boolean expects(In.At first) {
		return first == In.At.Struct;
	}

	public String toString() {
		return getClass().getSimpleName() + "<" + type.getTypeName() + ">";
	}
}
