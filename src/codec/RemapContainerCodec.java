package io.immutables.codec;

import java.util.function.Function;

public interface RemapContainerCodec {
	// Objects here because Java cannot express higher-kinded types
	// (Codec<C<E>>, Codec<E> -> Codec<N>) -> Codec<C<N>>
	Codec<Object, In, Out> remap(Function<Codec<Object, In, Out>, Codec<Object, In, Out>> replacer);
}
