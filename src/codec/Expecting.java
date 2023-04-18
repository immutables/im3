package io.immutables.codec;

/**
 * Interface for codecs to signal what it expects, which is important in some contexts,
 * like matching of polymorphic subclass codecs. Another important use case is determining if
 * codec can be used in contexts like decoding object keys for {@link java.util.Map}, where
 * only strings can be used.
 */
public interface Expecting {

	boolean canExpect(In.At first);
}
