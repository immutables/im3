package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Optional;

final class OptionalCodecs implements Codec.Factory<In, Out> {

	public @Null Codec<?, In, Out> tryCreate(
		Type type, Class<?> raw,
		Medium<? extends In, ? extends Out> medium,
		Codec.Lookup<In, Out> lookup) {

		if (raw.isAssignableFrom(Optional.class)) {
			return new OptionalCodec<>(
				lookup.get(Types.getFirstArgument(type)));
		}
		return null;
	}

	private final static class OptionalCodec<T> extends Codec<Optional<T>, In, Out> {
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
			if (in.instanceFailed()) return null;
			return Optional.ofNullable(content);
		}

		public Optional<T> defaultInstance() {
			return Optional.empty();
		}
	}
}
