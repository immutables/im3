package io.immutables.build.mvn;

import java.net.URI;

public record MvnJar(Gav gav, Kind kind) {

	public enum Kind { Classes, Sources }

	public String toFilename() {
		return gav.toFilenameBase() + suffix();
	}

	public URI toMvnUri() {
		return gav.toMvnUri(Mvn.REPO, suffix());
	}

	private String suffix() {
		return switch (kind) {
			case Classes -> Mvn.EXT_JAR;
			case Sources -> Mvn.EXT_SRC;
		};
	}
}
