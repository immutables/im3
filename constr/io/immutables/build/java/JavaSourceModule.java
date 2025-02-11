package io.immutables.build.java;

import io.immutables.build.BuildModule;
import java.nio.file.Path;

public final class JavaSourceModule extends BuildModule implements JavaModule {
  private final Path dir;
  private final Path moduleInfoFile;
  private final ModuleInfo info;

  JavaSourceModule(
      ModuleInfo info,
      Path dir, // relative to root
      Path moduleInfoFile) {
    super(info.name());
    this.info = info;
    this.dir = dir;
    this.moduleInfoFile = moduleInfoFile;
  }

  public ModuleInfo info() {
    return info;
  }

  public Path dir() {
    return dir;
  }

  public Path moduleInfoFile() {
    return moduleInfoFile;
  }
}
