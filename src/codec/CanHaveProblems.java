package io.immutables.codec;

import java.io.IOException;

// TODO complete proper handling of mismatch and validation
abstract class CanHaveProblems {
	public abstract String path() throws IOException;

	public abstract In.At peek() throws IOException;

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

	public boolean hasProblems() {return false;}
}
