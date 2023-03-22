package build;

import java.net.URI;

record Gav(
	String group,
	String artifact,
	String version,
	String classifier) {

	Gav(String group, String artifact, String version) {
		this(group, artifact, version, "");
	}

	Gav {
		assert !group.isEmpty();
		assert !artifact.isEmpty();
		assert !version.isEmpty();
	}

	URI toMvnUri(String prefix, String suffix) {
		var dirs = "%s/%s/%s/".formatted(group.replace('.', '/'), artifact, version);
		return URI.create(prefix + dirs + toFilenameBase() + suffix);
	}

	String toFilenameBase() {
		return !classifier.isEmpty()
			? "%s-%s-%s".formatted(artifact, classifier, version)
			: "%s-%s".formatted(artifact, version);
	}

	@Override public String toString() {
		return !classifier.isEmpty()
			? "%s:%s:%s:%s".formatted(group, artifact, classifier, version)
			: "%s:%s:%s".formatted(group, artifact, version);
	}
}
