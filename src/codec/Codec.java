package io.immutables.codec;

import io.immutables.meta.Null;
import io.immutables.meta.NullUnknown;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * Codec implements per-type routines to encode and decode instances with some sort
 * of {@link Medium}. Codec combines what in some other systems requires 2 separate
 * classes/interfaces like Serializer and Deserializer. Our consideration is as follows:
 * usually we have both encoding and decoding implemented for a particular type,
 * so we have a lot less objects (maybe by half) to reside in the registries, and also,
 * because some codec features (i.e. the ability to describe known fields) are shared
 * between encoding and decoding, this reduces further duplication.
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
	public abstract @NullUnknown T decode(I in) throws IOException;

	/**
	 * General lookup interface for codecs. See {@link Registry} as a standard
	 * implementation which is built from {@link Factory} instances and provide
	 * sophisticated mechanism to pick proper codecs for specific {@link Medium}.
	 * @see Registry
	 * @see Factory
	 * @see Medium
	 */
	public interface Resolver {
		/**
		 * Resolves the codec for a given type and medium.
		 * @param type generic type or specific class (for not parameterized types and primitives).
		 *	parameterized types should have all the type arguments fully specified,
		 *	i.e. no type variables.
		 * @param medium medium against which specific codecs will be selected.
		 * @return optional codec, might be absent
		 * @param <T> target type, should match passed {@code type}, but can be specified as {@link Object} if this can be beneficial in particular place, relying on runtime check for {@link Type}.
		 * @param <I> Subclass of {@link In} (or just {@code In}) used for this medium
		 * @param <O> Subclass of {@link Out} (or just {@code Out}) used for this medium
		 */
		<T, I extends In, O extends Out>
		Optional<Codec<T, I, O>> resolve(Type type, Medium<I, O> medium);
	}

	/**
	 * Factory able to instantiate codecs for specific type and medium. Many factories
	 * can be considered, depending on the kind of factories or if specific supported
	 * raw classes specified, so {@link #tryCreate} method should be quick to return {@code null}
	 * if checks/internal lookups will show that no codecs can be creating, striving for
	 * constant time checks.
	 */
	public interface Factory<I extends In, O extends Out> {
		/**
		 * Try to create codec for given type or return {@code null} if
		 * factory is not applicable and cannot create such codec.
		 * @param type generic type with all arguments fully specified. This would be class instance for non-parameterized types and primitives.
		 * @param raw raw class of the given type. For non parameterized types, {@code class == type}
		 * @param medium medium for which codecs should be picked.
		 * @param lookup delegate for codec to lookup nested codecs. Useful for
		 *  parameterized codecs and compound types (structs and tuples)
		 * @return created codec or {@code null}
		 */
		@Null Codec<?, I, O> tryCreate(
			Type type,
			Class<?> raw,
			Medium<? extends I, ? extends O> medium,
			Codec.Lookup<I, O> lookup);
	}

	/**
	 * Implemented by {@link Factory} to register it for specific types (raw classes).
	 * These will be ignored if list of raw classes supplied with factory during registration
	 * ({@link  Registry.Builder#add(Codec.Factory, Medium, Class...)}).
	 * This improves efficiency of factory registration to target specific types, leaving
	 * smaller number of factories which will be checked against each type
	 * For example, codec factory for records checks each class if it's a record class
	 * or a sealed interface, but factory for specific types will be looked up by class
	 * directly, which will work best for final classes, because no subtyping will be considered
	 * when using set of supported types.
	 */
	public interface SupportedTypes {
		/** Set of supported types (raw classes) */
		Set<Class<?>> supportedRawTypes();
	}

	/**
	 * Special delegate to allow codecs to lookup other codecs
	 * within context of the same medium and any other internal sequencing
	 * (think about handling even recursive types). Also, it fails when codec is not found,
	 * because it considered required. Extending {@link Resolver} allows lookup,
	 * but switching medium and out of this context.
	 */
	public interface Lookup<I extends In, O extends Out> extends Resolver {
		/** Lookup using same medium */
		<T> Codec<T, I, O> get(Type type) throws NoSuchElementException;
	}
}
