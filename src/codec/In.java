package io.immutables.codec;

import java.io.IOException;

public abstract class In extends StreamBase {
	public enum At {
		Null,
		Int,
		Long,
		Float,
		True,
		False,
		String,
		Struct,
		StructEnd,
		Field,
		Array,
		ArrayEnd,
		End,
		Nope,
	}

	public abstract At peek() throws IOException;

	public abstract int takeInt() throws IOException;

	public abstract long takeLong() throws IOException;

	public abstract double takeDouble() throws IOException;

	public abstract boolean takeBoolean() throws IOException;

	public abstract void takeNull() throws IOException;

	public abstract String takeString() throws IOException;

	public abstract int takeName(NameIndex names) throws IOException;

	public abstract int takeField() throws IOException;

	/**
	 * Last name that was read, useful when {@link #takeField()} or {@link #takeName(NameIndex)}
	 * return {@link NameIndex#UNKNOWN}. This should be consulted immediately after mentioned
	 * {@code takeField} and {@code takeName} operations, and it should be an error/undefined to call
	 * it in other contexts.
	 */
	public abstract String name() throws IOException;

	/**
	 * Skips the current value altogether including all nested objects and arrays.
	 * If we're at field name, it will skip both name and the following value.
	 */
	public abstract void skip() throws IOException;

	// Advanced* level
	//public abstract int takeChars(char[] chars, int offset, int length) throws IOException;

	public abstract boolean hasNext() throws IOException;

	public abstract void beginArray() throws IOException;

	public abstract void endArray() throws IOException;

	public abstract void beginStruct(NameIndex names) throws IOException;

	public abstract void endStruct() throws IOException;

	public abstract Buffer takeBuffer() throws IOException;

	public static abstract class Buffer {
		public abstract In in();
	}

	public boolean hasProblems() {return false;}

	private boolean instanceFailed;
	public boolean instanceFailed() {return instanceFailed;}

	public void failInstance() {
		instanceFailed = true;
	}

	public boolean clearInstanceFailed() {
		boolean b = instanceFailed;
		instanceFailed = false;
		return b;
	}
}
