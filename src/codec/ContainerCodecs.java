package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

// TODO handle failed instance
final class ContainerCodecs {
	static final Codec.Factory<In, Out> ArraysFactory = (type, raw, medium, lookup) -> {
		if (raw.isArray()) {
			Class<?> componentType = raw.getComponentType();
			return new ArrayCodec(componentType, lookup.get(componentType));
		}
		return null;
	};

	private final static class OptionalCodec<T> extends DefaultingCodec<Optional<T>, In, Out> {
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

	private static final class ListCodec extends DefaultingCodec<Object, In, Out> {
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

		public Object getDefault() {
			return List.of();
		}

		public boolean providesDefault() {
			return true;
		}
	}

	private static final class SetCodec extends DefaultingCodec<Object, In, Out> {
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
	}

	private static final class ArrayCodec extends Codec<Object, In, Out> {
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

		public @Null Object decode(In in) throws IOException {
			in.beginArray();

			var buffer = new ArrayList<>();
			while (in.hasNext()) {
				buffer.add(componentCodec.decode(in));
			}
			in.endArray();

			Object instance = Array.newInstance(componentType, buffer.size());
			for (int i = 0; i < buffer.size(); i++) {
				Array.set(instance, i, buffer.get(i));
			}
			return instance;
		}

		public Object defaultInstance() {
			return Array.newInstance(componentType, 0);
		}
	}

	public static Class<?>[] classes() {
		return classes.clone();
	}

	private static final Class<?>[] classes = {
		List.class, Set.class,
		Optional.class, OptionalInt.class, OptionalLong.class, OptionalDouble.class
	};

	// In the end to avoid problems with forward references to constant codecs
	static final Codec.Factory<In, Out> GenericFactory = (type, raw, medium, lookup) -> {
		if (raw == List.class) {
			var elementType = Types.getFirstArgument(type);
			return new ListCodec(lookup.get(elementType));
		}
		if (raw == Set.class) {
			var elementType = Types.getFirstArgument(type);
			return new SetCodec(lookup.get(elementType));
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
