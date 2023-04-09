package io.immutables.codec;

/**
 * Represent medium format or technology to which codecs are bridged.
 * While most codecs will work with {@link In} and {@link Out} regardless of
 * implementation subclasses, some codecs can be written and registered to specifically
 * support extensions provided by a particular format.
 * An alternative design could be just trying to unwrap delegate/downcast In/Out instances
 * inside codecs, that would not only be less civilized, but would not allow to match
 * registered codecs against medium during lookup. With mediums, we can have codecs
 * implemented and registered against any medium or only for a particular medium,
 * and having legitimate access to the In/Out subclasses if needed.
 * <p>
 * Intended usage: create a static constant of a {@code Medium} with intended
 * @param <I> {@link In} or its subclass used for this medium
 * @param <O> {@link Out} or its subclass used for this medium
 * @see Registry
 * @see Codec
 */
public abstract class Medium<I extends In, O extends Out> {
	protected Medium() {}

	/**
	 * Any: catch-all, lowest common denominator medium.
	 */
	public static final Medium<In, Out> Any = new Medium<>() {
		public String toString() {
			return Medium.class.getSimpleName() + ".Any";
		}
	};

	/**
	 * Json: medium can be used when codec specifically registered only for JSON,
	 * and so will not be available to other mediums.
	 */
	public static final Medium<In, Out> Json = new Medium<>() {
		public String toString() {
			return Medium.class.getSimpleName() + ".Json";
		}
	};

	/** Force object identity equality, prohibit override of equals. */
	public final boolean equals(Object obj) {
		return this == obj;
	}

	/** Force object identity hashCode, prohibit override of hashCode. */
	public final int hashCode() {
		return super.hashCode();
	}
}
