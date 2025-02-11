package io.immutables.build.mvn;

import io.immutables.build.FatalException;
import java.net.URI;

public record Gav(
    String group,
    String artifact,
    String version,
    String classifier) {

  public Gav(String group, String artifact, String version) {
    this(group, artifact, version, "");
  }

  public Gav {
    if (group.isEmpty() || artifact.isEmpty() || version.isEmpty()) throw new AssertionError();
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

  public static Gav from(String groupArtifactClassifier, String version) {
    var parts = groupArtifactClassifier.split(":");
    return switch (parts.length) {
      case 2 -> new Gav(parts[0], parts[1], version);
      case 3 -> new Gav(parts[0], parts[1], version, parts[2]);
      default -> throw new FatalException(
          "wrong format '%s' =/=> group:artifact[:classifier]".formatted(groupArtifactClassifier));
    };
  }
}
