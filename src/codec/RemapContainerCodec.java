package io.immutables.codec;

import java.util.function.Function;

public interface RemapContainerCodec {
	// Objects here because Java cannot express higher-kind types
	// and this have to work across different types of containers, not only Collection<E>
	// Anyway, implementation would most likely use unchecked casts, so this should
	// rely on runtime type token checking and valid function implementation.

	// (Codec<E> -> Codec<N>) -> Codec<C<E>> -> Codec<C<N>>
	Codec<Object, In, Out> remap(Function<Codec<Object, In, Out>, Codec<Object, In, Out>> replacer);
}
