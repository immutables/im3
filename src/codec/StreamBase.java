package io.immutables.codec;

abstract class StreamBase {
	public abstract NameIndex create(String[] known, NameIndex.Unknown unknown);
}
