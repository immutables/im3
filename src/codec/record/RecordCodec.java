package io.immutables.codec.record;

import io.immutables.codec.*;
import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.*;

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

	@SuppressWarnings("unchecked") // for private generic array
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
		//var componentRawTypes = new Class<?>[length];

		for (int i = 0; i < length; i++) {
			var c = components[i];
			componentNames[i] = c.getName();
			// currently it will blow up on unsupported type
			// maybe declare special kind of Exception and handle it in a specific way
			// like NonCompliantRecordDefinition
			var componentType = Types.resolveArguments(c.getGenericType(), arguments);

			componentTypes[i] = componentType;
			//componentRawTypes[i] = c.getType();
			componentAccessors[i] = c.getAccessor();

			var codec = lookup.get(componentType);
			if (Providers.metadata().isNullableComponent(c)) {
				codec = Codecs.nullSafe(codec);
			}
			componentCodecs[i] = codec;
		}

		canonicalConstructor = Reflect.getCanonicalConstructor(raw);
	}

	@Override public boolean mayConform(In in) throws IOException {
		return mayConform(in, null);
	}

	@Override public boolean mayConform(In in, @Null CaseTag tag) throws IOException {
		// only for structs
		if (in.peek() != Token.Struct) return false;

		// If we have tag, we will just go over all properties to find it and match,
		// ignoring everything else
		if (tag != null) return hasMatchingTag(in, tag);

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

	private static boolean hasMatchingTag(In in, CaseTag tag) throws IOException {
		in.beginStruct(in.index(tag.field()));
		while (in.hasNext()) {
			// found our single indexed field
			if (in.takeField() == 0) {
				// if we matched field, then we match on value, which
				// will definitely tell use if this can be parsed with this codec
				// then other field/type mismatches will have a better diagnostics
				// because codec selection will be pre-made
				if (in.peek() == Token.String) {
					return in.takeString().equals(tag.value());
				}
				in.skip(); // not strictly necessary as buffered 'in' supposed to be throw-away
				return false;
			}
			// if we haven't matched field, just skip value and go to a next field
			// it is necessary
			in.skip();
		}
		in.endStruct(); // not strictly necessary as buffered 'in' supposed to be throw-away
		return false;
	}

	@Override void encode(Out out, T instance, @Null CaseTag tag) throws IOException {
		if (names == null) names = out.index(componentNames);

		var length = componentNames.length;

		out.beginStruct(names);

		if (tag != null) {
			// first we will output tag if requested
			out.putField(tag.field());
			out.putString(tag.value());
		}

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

	@Override public void encode(Out out, T instance) throws IOException {
		encode(out, instance, null);
	}

	@SuppressWarnings("unchecked") // constructor picked by runtime reflection, matches T
	@Override public @Null T decode(In in) throws IOException {
		if (names == null) names = in.index(componentNames);

		var length = componentNames.length;
		var componentValues = new Object[length];
		var componentPresent = new boolean[length];

		in.beginStruct(names);
		if (in.problems.raised()) {
			// failed if not a struct
			assert !in.hasNext();
			in.endStruct();
			return in.problems.unreachable();
		}

		boolean componentFailed = false;

		while (in.hasNext()) {
			int f = in.takeField();
			if (f >= 0) {
				componentPresent[f] = true;
				componentValues[f] = componentCodecs[f].decode(in);
				componentFailed |= in.problems.raised();
			} else {
				in.unknown(type);
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
					componentValues[i] = defaulting.getDefault(in);
					componentFailed |= in.problems.raised();
				} else {
					in.missing(componentNames[i], componentTypes[i], type);
					componentFailed = true;
				}
			}
		}

		// if components missing or failed to instantiate
		// we should not even try to instantiate our record
		if (componentFailed) return in.problems.unreachable();

		// all components ok, we try to instantiate record
		// it's unlikely to fail, but still it can have precondition
		// checks or runtime problems in constructor
		try {
			return (T) Reflect.newInstance(canonicalConstructor, componentValues);
		} catch (RuntimeException exception) {
			in.cannotInstantiate(type, exception.getMessage());
			return in.problems.unreachable();
		}
	}

	@Override public boolean expects(Token first) {
		return first == Token.Struct;
	}

	@Override public String toString() {
		return getClass().getSimpleName() + "<" + type.getTypeName() + ">";
	}
}
