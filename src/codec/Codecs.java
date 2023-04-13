package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;

public class Codecs {
	private Codecs() {}

	public static <T, I extends In, O extends Out> Codec<T, I, O> nullSafe(Codec<T, I, O> original) {
		return new DefaultingCodec<>() {
			public void encode(O out, @Null T instance) throws IOException {
				if (instance == null) out.putNull();
				else original.encode(out, instance);
			}

			public @Null T decode(I in) throws IOException {
				if (in.peek() == In.At.Null) return null;
				return original.decode(in);
			}

			public String toString() {
				return "nullSafe(" + original + ")";
			}

			public @Null T getDefault() {
				return null;
			}

			public boolean providesDefault() {
				return true;
			}

			public boolean canSkip(O out, @Null T instance) {
				// TODO add some sort of query to out's context if skipping null is allowed
				return instance == null;
			}
		};
	}
}
