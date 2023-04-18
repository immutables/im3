package io.immutables.codec.record;

import io.immutables.codec.*;
import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

@SuppressWarnings("unchecked") // TODO explain
final class ProductRecordCodec<T> extends CaseCodec<T, In, Out> implements Expecting {
	private final String[] componentNames;
	private final Type[] componentTypes;
	private final Codec<Object, In, Out>[] componentCodecs;
	private final Method[] componentAccessors;
	private final Constructor<?> canonicalConstructor;
	private final Type type;

	ProductRecordCodec(Type type, Class<?> raw, Codec.Lookup<In, Out> lookup) {
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
			if (RecordsFactory.metadata.isNullableComponent(c)) {
				codec = Codecs.nullSafe(codec);
			}
			componentCodecs[i] = codec;
		}

		canonicalConstructor = Reflect.getCanonicalConstructor(raw, componentRawTypes);
	}

	public boolean mayConform(In in) throws IOException {
		// only for arrays
		if (in.peek() != In.At.Array) return false;

		int length = componentNames.length;
		in.beginArray();

		int i = 0;
		while (in.hasNext()) {
			if (i >= length) {
				// extra components
				return false;
			}
			var c = componentCodecs[i];
			if (c instanceof CaseCodec<Object, In, Out> caseCodec) {
				if (!caseCodec.mayConform(in)) return false;
				in.skip();
			} else if (c instanceof Expecting expects) {
				if (!expects.canExpect(in.peek())) return false;
				in.skip();
			}
			i++;
		}
		in.endArray();
		// check for insufficient components (extra components would return earlier
		return i == length;
	}

	public void encode(Out out, T instance) throws IOException {
		var length = componentNames.length;

		out.beginArray();
		for (int i = 0; i < length; i++) {
			var value = Reflect.getValue(componentAccessors[i], instance);
			componentCodecs[i].encode(out, value);
		}
		out.endArray();
	}

	public @Null T decode(In in) throws IOException {
		var length = componentNames.length;
		var componentValues = new Object[length];

		in.beginArray();

		boolean componentFailed = false;
		int i = 0;
		while (in.hasNext()) {
			if (i < length) {
				componentValues[i] = componentCodecs[i].decode(in);
				componentFailed |= in.clearInstanceFailed();
			} else {
				in.unknown();
				in.skip();
				componentFailed = true;
			}
			i++;
		}

		in.endArray();

		// this block handles missing components, when not enough components read from array
		for (int j = i; j < length; j++) {
			in.missing(componentNames[j], componentTypes[j]);
			componentFailed = true;
		}

		if (componentFailed) {
			in.failInstance();
			return null;
		}

		var instance = Reflect.newInstance(canonicalConstructor, componentValues);

		return (T) instance;
	}

	public boolean canExpect(In.At first) {
		return first == In.At.Array;
	}

	public String toString() {
		return getClass().getSimpleName() + "<" + type.getTypeName() + ">";
	}
}
