package io.immutables.build.idea;

import io.immutables.build.ModuleName;
import io.immutables.build.mvn.MvnJar;
import io.immutables.build.mvn.Mvn;
import java.nio.file.Path;

public interface Dirs {
  Path project = Path.of(".");

  Path lib = Path.of("lib");
  Path build = Path.of(".build");
  Path artifacts = build.resolve("artifacts");
  Path modules = lib.resolve("modules");

  Path idea = Path.of(".idea");
  Path ideaLibraries = idea.resolve("libraries");
  Path ideaModules = idea.resolve("modules");
  Path generatedContent = build.resolve("generated");
  Path generatedTestContent = build.resolve("generated");

  static Path downloaded(MvnJar jar) {
    return artifacts.resolve(jar.gav().group()).resolve(jar.toFilename());
  }

  static Path vendored(ModuleName module) {
    return modules.resolve(module + Mvn.EXT_JAR);
  }
}
