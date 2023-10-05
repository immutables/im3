package io.immutables.codec;

import java.io.IOException;

public abstract class In extends InOutBase {
	public enum At {
		Null,
		Int,
		Long,
		Float,
		True,
		False,
		String,
		Special,
		Struct,
		StructEnd,
		Field,
		Array,
		ArrayEnd,
		End,
		Nope;

		public boolean isScalar() {
			return switch (this) {
				case Null,
					Int,
					Long,
					Float,
					True,
					False,
					String,
					Special -> true;
				default -> false;
			};
		}
	}

	public abstract NameIndex index(String... known);

	public abstract At peek() throws IOException;

	public abstract int takeInt() throws IOException;

	public abstract long takeLong() throws IOException;

	public abstract double takeDouble() throws IOException;

	public abstract boolean takeBoolean() throws IOException;

	public abstract void takeNull() throws IOException;

	public abstract String takeString() throws IOException;

	public abstract int takeString(NameIndex names) throws IOException;

	public abstract int takeField() throws IOException;

	/**
	 * Last name that was read, useful when {@link #takeField()} or {@link #takeString(NameIndex)}
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
}
