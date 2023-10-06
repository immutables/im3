package io.immutables.codec;

import java.io.IOException;

// TODO complete proper handling of mismatch and validation
// or just inline in
abstract class InBase {
	/** Current path, roughly of JSON-path notation */
	public abstract String path() throws IOException;

	/** Current peeked token. */
	public abstract In.At peek() throws IOException;

	/**
	 * Last name that was read, useful when {@link In#takeField()} or
	 * {@link In#takeString(NameIndex)} return {@link NameIndex#UNKNOWN}.
	 * This should be consulted immediately after mentioned
	 * {@code takeField} and {@code takeString} operations, and it should be
	 * an error/undefined to call it in other contexts.
	 */
	public abstract String name() throws IOException;

	public void missing(String name, Object descriptor) throws IOException {
		throw new IOException(
			"%s: Missing '%s' field of type (%s)".formatted(path(), name, descriptor));
	}

	public void unknown() throws IOException {
		throw new IOException("%s: Unknown '%s' field, %s".formatted(path(), name(), peek()));
	}

	private boolean instanceFailed;

	public boolean wasInstanceFailed() {return instanceFailed;}

	public void failInstance() throws IOException {
		instanceFailed = true;
		throw new IOException("%s: Failed instance".formatted(path()));
	}

	public boolean clearInstanceFailed() {
		boolean b = instanceFailed;
		instanceFailed = false;
		return b;
	}
}
