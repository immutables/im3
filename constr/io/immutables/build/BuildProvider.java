package io.immutables.build;

public interface BuildProvider {
  void apply(Workspace workspace);
}
