package io.immutables.build;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public interface Hashed {

  HashCode hash();

  HashFunction Function = Hashing.murmur3_128(0xCAFEBABE);
}
