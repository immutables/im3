package io.immutables.build;

import io.immutables.build.java.JavaPatterns;
import java.util.Optional;

public record ModuleName(String name) {

  @Override public String toString() {
    return name;
  }

  public static Optional<ModuleName> tryParse(String name) {
    if (JavaPatterns.QualifiedName.matcher(name).matches()) {
      return Optional.of(new ModuleName(name));
    }
    return Optional.empty();
  }
}
