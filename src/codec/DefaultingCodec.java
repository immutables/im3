package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;

/**
 * Abstract subclass for codecs which can provide default value if input is missing.
 * Also, it can help determine if the field can be skipped during output if it's considered
 * a trivial default.
 * However, is not guaranteed that codec will provide such default instance. Important
 * to understand that this behavior of providing default instance is only for certain contexts,
 * like field values in JSON object/struct. It will not work for this {@code Codec<T>.decode},
 * but only if specifically called by the enclosing coded.
 * @param <T> Type which is handled by this codec
 * @param <I> Subclass of {@link In} (or just {@code In}) used for this medium
 * @param <O> Subclass of {@link Out} (or just {@code Out}) used for this medium
 */
public abstract class DefaultingCodec<T, I extends In, O extends Out>
		extends Codec<T, I, O> {
	/**
	 * Can provide a default value for a type when value is absent from the input,
	 * but needed to construct an instance for a component(field) represented by
	 * this codec.
	 * @return default implementation returns {@code null}, override to change this
	 */
	public @Null T getDefault(In in) throws IOException {return null;}

	/**
	 * Checks if this codec will indeed provide a default. Should be quick and lightweight check.
	 * @return if this codec provides getDefault (even {@code null} if it is the default)
	 */
	public boolean providesDefault() {return false;}

	/**
	 * With this hook, codec can signal that it might not need to output the field value
	 * if it thinks it's a kind of empty value. Output stream can be used to by codec,
	 * to consult configuration of the current medium.
	 */
	public boolean canSkip(O out, @Null T instance) {return false;}
}
