package io.immutables.codec;

/**
 * Interface for codec to signal what it expects, which is important in some contexts,
 * like matching of polymorphic subclasses. Another important use case is determining if
 * codec can be used in contexts like decoding object keys for {@link java.util.Map},
 * where only strings can be used.
 */
public interface Expecting {

	boolean expects(In.At first);
}
