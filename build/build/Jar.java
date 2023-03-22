package build;

import java.net.URI;

record Jar(Gav gav, Kind kind) {

	enum Kind { Classes, Sources }

	String toFilename() {
		return gav.toFilenameBase() + suffix();
	}

	URI toMvnUri() {
		return gav.toMvnUri(Mvn.repo, suffix());
	}

	private String suffix() {
		return switch (kind) {
			case Classes -> Mvn.ext_jar;
			case Sources -> Mvn.ext_src;
		};
	}
}
