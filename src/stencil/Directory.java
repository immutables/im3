package io.immutables.stencil;

import java.nio.file.Path;

public class Directory extends Current {
	final Path path;

	public Directory(Path path) {
		this.path = path;
	}
}
