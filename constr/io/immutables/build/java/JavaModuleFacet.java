package io.immutables.build.java;

import io.immutables.build.Facet;
import io.immutables.build.ModuleName;
import io.immutables.build.Target;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface JavaModuleFacet extends Facet {
  Target<Path> modulePath();
  Target<Path> moduleJarFile();

  Target<List<Dependency>> dependencies();

  Target<Set<ModuleName>> allRuntimeModules();
}
