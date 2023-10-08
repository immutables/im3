package io.immutables.codec;

import java.io.IOException;
import java.lang.reflect.Type;

public abstract class In {
	public final Problem.Handler problems;

	protected In(Problem.Handler problems) {this.problems = problems;}

	protected In() {this(Problem.ThrowingHandler);}

	public abstract NameIndex index(String... known);

	public abstract int takeInt() throws IOException;

	public abstract long takeLong() throws IOException;

	public abstract double takeDouble() throws IOException;

	public abstract boolean takeBoolean() throws IOException;

	public abstract void takeNull() throws IOException;

	public abstract String takeString() throws IOException;

	public abstract int takeString(NameIndex names) throws IOException;

	public abstract int takeField() throws IOException;

	/**
	 * Skips the current value altogether including all nested objects and arrays. If we're at field
	 * name, it will skip both name and the following value.
	 */
	public abstract void skip() throws IOException;

	// Advanced* level
	// Current implementations we have (Jackson, Jdbc) probably do not have good ways
	// to get any performance benefits from such optimization, because string objects
	// are already created.
	//public abstract int takeChars(char[] chars, int offset, int length) throws IOException;

	public abstract boolean hasNext() throws IOException;

	public abstract void beginArray() throws IOException;

	public abstract void endArray() throws IOException;

	public abstract void beginStruct(NameIndex names) throws IOException;

	public abstract void endStruct() throws IOException;

	public abstract Buffer takeBuffer() throws IOException;

	/** Current path. */
	public abstract AtPath path();

	/** Current peeked token. */
	public abstract Token peek() throws IOException;

	/**
	 * Last name that was read, useful when {@link In#takeField()} or
	 * {@link In#takeString(NameIndex)}
	 * return {@link NameIndex#UNKNOWN}. This should be consulted immediately after mentioned
	 * {@code takeField} and {@code takeString} operations, and it should be an error/undefined to
	 * call it in other contexts.
	 */
	public abstract String name() throws IOException;

	public void missing(String field, Type componentType, Type recordType) throws IOException {
		problems.enque(new Problem.MissingField(path(), field, componentType, recordType));
	}

	public void cannotInstantiate(Type type, String message) throws IOException {
		problems.enque(new Problem.CannotInstantiate(path(), type));
	}

	public void noMatchingCase(Type sealedType) throws IOException {
		problems.enque(new Problem.NoMatchingCase(path(), sealedType));
	}

	public void unknown(Type type) throws IOException {
		// Maybe not a problem
		//problems.enque(new Problem.UnknownField(path(), name(), peek(), type));
	}

	public static abstract class Buffer {
		public abstract In in();
	}

	protected static final String NOT_A_STRING = "\0";
}
