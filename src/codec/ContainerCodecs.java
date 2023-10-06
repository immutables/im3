package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

// TODO handle failed instance
final class ContainerCodecs {
	static final Codec.Factory<In, Out> ArraysFactory = (type, raw, medium, lookup) -> {
		if (raw.isArray()) {
			Class<?> componentType = raw.getComponentType();
			return new ArrayCodec(componentType, lookup.get(componentType));
		}
		return null;
	};

	private final static class OptionalCodec<T>
			extends DefaultingCodec<Optional<T>, In, Out>
			implements RemapContainerCodec {
		private final Codec<T, In, Out> codec;

		OptionalCodec(Codec<T, In, Out> codec) {
			this.codec = codec;
		}

		public void encode(Out out, Optional<T> instance) throws IOException {
			@Null var content = instance.orElse(null);
			if (content == null) out.putNull();
			else codec.encode(out, content);
		}

		public @Null Optional<T> decode(In in) throws IOException {
			if (in.peek() == In.At.Null) return Optional.empty();
			// ofNullable just in case we have construction failed returning null
			@Null T content = codec.decode(in);
			if (in.wasInstanceFailed()) return null;
			return Optional.ofNullable(content);
		}

		public boolean canSkip(Out out, @Null Optional<T> instance) {
			return instance == null || instance.isEmpty();
		}

		public Optional<T> getDefault() {
			return Optional.empty();
		}

		public boolean providesDefault() {
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Codec<Object, In, Out> remap(
				Function<Codec<Object, In, Out>, Codec<Object, In, Out>> replacer) {
			var element = replacer.apply((Codec<Object, In, Out>) codec);
			return (Codec<Object, In, Out>) (Object) new OptionalCodec<Object>(element);
		}
	}

	private static final Codec<OptionalInt, In, Out> optionalIntCodec = new DefaultingCodec<>() {
		public void encode(Out out, OptionalInt instance) throws IOException {
			if (instance.isEmpty()) out.putNull();
			else out.putInt(instance.getAsInt());
		}

		public OptionalInt decode(In in) throws IOException {
			if (in.peek() == In.At.Null) return OptionalInt.empty();
			return OptionalInt.of(in.takeInt());
		}

		public OptionalInt getDefault() {
			return OptionalInt.empty();
		}

		public boolean providesDefault() {
			return true;
		}

		public boolean canSkip(Out out, @Null OptionalInt instance) {
			return instance == null || instance.isEmpty();
		}
	};

	private static final Codec<OptionalLong, In, Out> optionalLongCodec = new DefaultingCodec<>() {
		public void encode(Out out, OptionalLong instance) throws IOException {
			if (instance.isEmpty()) out.putNull();
			else out.putLong(instance.getAsLong());
		}

		public OptionalLong decode(In in) throws IOException {
			if (in.peek() == In.At.Null) return OptionalLong.empty();
			return OptionalLong.of(in.takeInt());
		}

		public OptionalLong getDefault() {
			return OptionalLong.empty();
		}

		public boolean providesDefault() {
			return true;
		}

		public boolean canSkip(Out out, @Null OptionalLong instance) {
			return instance == null || instance.isEmpty();
		}
	};


	private static final Codec<OptionalDouble, In, Out> optionalDoubleCodec = new DefaultingCodec<>() {
		public void encode(Out out, OptionalDouble instance) throws IOException {
			if (instance.isEmpty()) out.putNull();
			else out.putDouble(instance.getAsDouble());
		}

		public OptionalDouble decode(In in) throws IOException {
			if (in.peek() == In.At.Null) return OptionalDouble.empty();
			return OptionalDouble.of(in.takeDouble());
		}

		public OptionalDouble getDefault() {
			return OptionalDouble.empty();
		}

		public boolean providesDefault() {
			return true;
		}

		public boolean canSkip(Out out, @Null OptionalDouble instance) {
			return instance == null || instance.isEmpty();
		}
	};

	private static final class ListCodec
			extends DefaultingCodec<Object, In, Out>
			implements RemapContainerCodec, Expecting {
		private final Codec<Object, In, Out> elementCodec;

		ListCodec(Codec<Object, In, Out> elementCodec) {
			this.elementCodec = elementCodec;
		}

		public void encode(Out out, Object instance) throws IOException {
			out.beginArray();
			for (var e : (List<?>) instance) {
				elementCodec.encode(out, e);
			}
			out.endArray();
		}

		public @Null Object decode(In in) throws IOException {
			var buffer = new ArrayList<>();
			in.beginArray();
			while (in.hasNext()) {
				buffer.add(elementCodec.decode(in));
			}
			in.endArray();
			return List.copyOf(buffer);
		}

		@Override public Object getDefault() {
			return List.of();
		}

		@Override public boolean providesDefault() {
			return true;
		}

		@Override public boolean expects(In.At first) {
			return first == In.At.Array;
		}

		@Override
		public Codec<Object, In, Out> remap(
				Function<Codec<Object, In, Out>, Codec<Object, In, Out>> replacer) {
			return new ListCodec(replacer.apply(elementCodec));
		}
	}

	private static final class SetCodec
			extends DefaultingCodec<Object, In, Out>
	 		implements RemapContainerCodec, Expecting {
		private final Codec<Object, In, Out> elementCodec;

		SetCodec(Codec<Object, In, Out> elementCodec) {
			this.elementCodec = elementCodec;
		}

		public void encode(Out out, Object instance) throws IOException {
			out.beginArray();
			for (var e : (Set<?>) instance) {
				elementCodec.encode(out, e);
			}
			out.endArray();
		}

		public @Null Object decode(In in) throws IOException {
			var buffer = new ArrayList<>();
			in.beginArray();
			while (in.hasNext()) {
				buffer.add(elementCodec.decode(in));
			}
			in.endArray();
			// Insertion order or duplicates will not be preserved
			// If those are important — don't use Set, and resort to a List
			return Set.copyOf(buffer);
		}

		public Object getDefault() {
			return Set.of();
		}

		public boolean providesDefault() {
			return true;
		}

		@Override
		public boolean expects(In.At first) {
			return first == In.At.Array;
		}

		@Override
		public Codec<Object, In, Out> remap(
				Function<Codec<Object, In, Out>, Codec<Object, In, Out>> replacer) {
			return new ListCodec(replacer.apply(elementCodec));
		}
	}

	private static final class ArrayCodec
			extends Codec<Object, In, Out>
			implements Expecting {
		private final Class<?> componentType;
		private final Codec<Object, In, Out> componentCodec;

		ArrayCodec(Class<?> componentType, Codec<Object, In, Out> componentCodec) {
			this.componentType = componentType;
			this.componentCodec = componentCodec;
		}

		public void encode(Out out, Object instance) throws IOException {
			out.beginArray();

			int length = Array.getLength(instance);
			for (int i = 0; i < length; i++) {
				componentCodec.encode(out, Array.get(instance, i));
			}

			out.endArray();
		}

		public Object decode(In in) throws IOException {
			in.beginArray();

			// Growing list collecting objects,
			// primitive types will be passed as wrapper types
			// from component decode anyway
			var buffer = new ArrayList<>();
			while (in.hasNext()) {
				buffer.add(componentCodec.decode(in));
			}
			in.endArray();

			// creating actual array of needed type and size
			Object instance = Array.newInstance(componentType, buffer.size());
			for (int i = 0; i < buffer.size(); i++) {
				// we expect that set will never fail if component codec
				// is valid, so that primitive arrays will never see null elements etc.
				Array.set(instance, i, buffer.get(i));
			}
			return instance;
		}

		public Object defaultInstance() {
			return Array.newInstance(componentType, 0);
		}

		@Override
		public boolean expects(In.At first) {
			return first == In.At.Array;
		}
	}

	private static final class MapCodec
			extends DefaultingCodec<Object, In, Out>
			implements Expecting {
		private final Codec<Object, In, Out> keyCodec;
		private final Codec<Object, In, Out> valueCodec;

		MapCodec(Codec<Object, In, Out> keyCodec, Codec<Object, In, Out> valueCodec) {
			this.keyCodec = keyCodec;
			this.valueCodec = valueCodec;
		}

		public void encode(Out out, Object instance) throws IOException {
			var keyOut = new Codecs.CaptureSimpleOut();
			out.beginStruct(out.index());
			for (var e : ((Map<?, ?>) instance).entrySet()) {
				keyCodec.encode(keyOut, e.getKey());
				out.putField(keyOut.asString());
				valueCodec.encode(out, e.getValue());
			}
			out.endStruct();
		}

		public @Null Object decode(In in) throws IOException {
			var keyIn = new Codecs.RetrieveSimpleIn();
			var buffer = new HashMap<>();
			in.beginStruct(in.index());
			while (in.hasNext()) {
				in.takeField(); // ignore field code
				keyIn.reset(in.name()); // use name read
				var key = keyCodec.decode(keyIn);
				var value = valueCodec.decode(in);
				buffer.put(key, value);
			}
			in.endStruct();
			return Map.copyOf(buffer);
		}

		public Object getDefault() {
			return Map.of();
		}

		public boolean providesDefault() {
			return true;
		}

		@Override
		public boolean expects(In.At first) {
			return first == In.At.Struct;
		}
	}

	public static Class<?>[] classes() {
		return classes.clone();
	}

	private static final Class<?>[] classes = {
		List.class, Set.class, Map.class,
		Optional.class, OptionalInt.class, OptionalLong.class, OptionalDouble.class
	};

	// Placed in the end of class, to avoid forward references
	// to codecs in final fields
	static final Codec.Factory<In, Out> GenericFactory = (type, raw, medium, lookup) -> {
		if (raw == List.class) {
			var elementType = Types.getFirstArgument(type);
			return new ListCodec(lookup.get(elementType));
		}
		if (raw == Set.class) {
			var elementType = Types.getFirstArgument(type);
			return new SetCodec(lookup.get(elementType));
		}
		if (raw == Map.class) {
			var keyType = Types.getFirstArgument(type);
			var valueType = Types.getSecondArgument(type);
			return new MapCodec(lookup.get(keyType), lookup.get(valueType));
		}
		if (raw == Optional.class) {
			var elementType = Types.getFirstArgument(type);
			return new OptionalCodec<>(lookup.get(elementType));
		}
		if (raw == OptionalInt.class) return optionalIntCodec;
		if (raw == OptionalLong.class) return optionalLongCodec;
		if (raw == OptionalDouble.class) return optionalDoubleCodec;
		return null;
	};
}
