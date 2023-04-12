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
final class RecordCodec<T> extends Codec<T, In, Out> {
	private final String[] componentNames;
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
		// var componentTypes = new Type[components.length];
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

			// componentTypes[i] = componentType;
			componentRawTypes[i] = c.getType();
			componentCodecs[i] = lookup.get(componentType);
			componentAccessors[i] = c.getAccessor();
		}

		canonicalConstructor = matchConstructor(raw, componentRawTypes);
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
			out.putField(i);

			var value = getValue(componentAccessors[i], instance);
			componentCodecs[i].encode(out, value);
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
			int i = in.takeField();
			if (i >= 0) {
				componentPresent[i] = true;
				componentValues[i] = componentCodecs[i].decode(in);

				componentFailed |= in.clearInstanceFailed();
			} else {

			}
		}

		in.endStruct();

		if (componentFailed) {
			in.failInstance();
		}

		for (int i = 0; i < length; i++) {
			if (!componentPresent[i]) {
				componentValues[i] = componentCodecs[i].defaultInstance();
			}
		}

		return (T) newInstance(canonicalConstructor, componentValues);
	}

	private static Constructor<?> matchConstructor(Class<?> record, Class<?>[] components) {
		for (var c : record.getDeclaredConstructors()) {
			if (Arrays.equals(c.getParameterTypes(), components)) {
				return c;
			}
		}
		throw Unreachable.contractual();
	}

	private static Object newInstance(Constructor<?> constructor, Object[] arguments) {
		try {
			return constructor.newInstance(arguments);
		} catch (IllegalAccessException | InstantiationException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	private static Object getValue(Method accessor, Object instance) {
		try {
			return accessor.invoke(instance);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getCause());
		}
	}
}
