package io.immutables.build.java;

import io.immutables.build.ModuleName;
import io.immutables.build.mvn.MvnJar;
import io.immutables.build.BuildModule;
import java.util.List;

public final class JavaVendorModule extends BuildModule implements JavaModule {
  private final List<MvnJar> sourceJars;
  private final List<MvnJar> classJars;
  private final List<ModuleName> transitive;
  private final boolean repackage;

  JavaVendorModule(
      ModuleName name,
      List<MvnJar> classJars,
      List<MvnJar> sourceJars,
      List<ModuleName> transitive,
      boolean repackage) {
    super(name);

    this.transitive = transitive;

    assert classJars.stream().allMatch(j -> j.kind() == MvnJar.Kind.Classes);
    assert sourceJars.stream().allMatch(j -> j.kind() == MvnJar.Kind.Sources);

    this.sourceJars = sourceJars;
    this.classJars = classJars;
    this.repackage = repackage;
  }

  public boolean repackage() {
    return repackage;
  }

  public List<MvnJar> classJars() {
    return classJars;
  }

  public List<MvnJar> sourceJars() {
    return sourceJars;
  }

  public List<ModuleName> transitive() {
    return transitive;
  }
}
