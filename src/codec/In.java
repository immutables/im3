package io.immutables.codec;

import java.io.IOException;

public abstract class In extends StreamBase {
	public enum At {
		Null,
		Int,
		Long,
		Double,
		Boolean,
		String,
		Struct,
		StructEnd,
		Field,
		Array,
		ArrayEnd,
		End,
	}

	public abstract At peek() throws IOException;

	public abstract int takeInt() throws IOException;

	public abstract long takeLong() throws IOException;

	public abstract double takeDouble() throws IOException;

	public abstract boolean takeBoolean() throws IOException;

	public abstract void takeNull() throws IOException;

	public abstract CharSequence takeString() throws IOException;

	public abstract void skip() throws IOException;

	// Advanced* level
	//public abstract int takeChars(char[] chars, int offset, int length) throws IOException;

	public abstract boolean hasNext() throws IOException;

	public abstract void beginArray() throws IOException;

	public abstract void endArray() throws IOException;

	public abstract void beginStruct() throws IOException;

	public abstract int takeField() throws IOException;

	public abstract void endStruct() throws IOException;
}
