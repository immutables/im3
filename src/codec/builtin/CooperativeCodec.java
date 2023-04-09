package io.immutables.codec.builtin;

import io.immutables.codec.Codec;
import io.immutables.codec.In;
import io.immutables.codec.Out;
import io.immutables.meta.Null;
import java.io.IOException;

public abstract class CooperativeCodec<T, I extends In, O extends Out>
	extends Codec<T, I, O> {
	public @Null T decode(I in) throws IOException {
		return null;
	}
}
