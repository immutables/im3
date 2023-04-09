package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Codec implements per-type routines to encode and decode instances with some sort
 * of {@link Medium}. Codec combines what in some other systems requires 2 separate
 * classes/interfaces like Serializer and Deserializer. Our consideration is as follows:
 * usually we have both encoding and decoding implemented for a particular type,
 * so we have a lot less objects (maybe by half) to reside in the registries, and also, because
 * some codec features (i.e. the ability to describe known fields) are shared between encoding
 * and decoding, this reduces further duplication.
 * However, we still able to create and register read-only or write-only codecs
 * if this is really necessary [need to add see/link].
 * @param <T> Type which is handled by this codec
 * @param <I> Subclass of {@link In} (or just {@code In}) used for this medium
 * @param <O> Subclass of {@link Out} (or just {@code Out}) used for this medium
 */
public abstract class Codec<T, I extends In, O extends Out> {

	/**
	 * Encodes an instance to the output medium.
	 */
	public abstract void encode(O out, T instance) throws IOException;

	/**
	 * Decodes an instance {@code null} return value is reserved for those cases.
	 */
	public abstract @Null T decode(I in) throws IOException;

	public interface Resolver {
		<T, I extends In, O extends Out>
		Optional<Codec<T, I, O>> resolve(Type type, Medium<I, O> medium);
	}

	/**
	 * Factory able to instantiate codecs for specific type and medium.
	 */
	public interface Factory<I extends In, O extends Out> {
		@Null Codec<?, I, O> tryCreate(Type type, Medium<I, O> medium);
	}

	/**
	 * Special delegate to allow codecs to lookup other codecs
	 * within context of the same medium and any other internal sequencing
	 * (think about recursive types). Also, it fails when codec is not found,
	 * because it considered required.
	 */
	public interface Lookup<I extends In, O extends Out> {
		<T> Codec<T, I, O> get(Type type) throws NoSuchElementException;
	}
}
