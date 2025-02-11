package io.immutables.build.java;

import io.immutables.build.FatalException;
import io.immutables.build.ModuleName;
import io.immutables.build.mvn.Gav;
import io.immutables.build.mvn.MvnJar;
import io.immutables.build.BuildProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class VendoredModules {
  private VendoredModules() {}

  public interface Libraries {
    Artifacts module(String libraryName);
  }

  public interface Artifacts {
    Artifacts classes(String groupArtifactClassifier, String version);
    Artifacts sources(String groupArtifactClassifier, String version);
    Artifacts transitive(String module);
    Artifacts noSources();
    Artifacts repackage();
  }

  public static BuildProvider vendor(Consumer<Libraries> definition) {
    var handles = new ArrayList<LibraryHandle>();

    definition.accept(module -> {
      var name = parseName(module);

      var handle = new LibraryHandle(name);
      handles.add(handle);
      return handle;
    });

    return workspace -> {
      for (var h : handles) {
        workspace.addModule(h.toModule());
      }
    };
  }

  private static final class LibraryHandle implements Artifacts {
    private final ModuleName moduleName;
    private final List<MvnJar> classJars = new ArrayList<>();
    private final List<MvnJar> sourceJars = new ArrayList<>();
    private final List<ModuleName> transitive = new ArrayList<>();
    private boolean repackage;
    private boolean noSources;

    LibraryHandle(ModuleName moduleName) {
      this.moduleName = moduleName;
    }

    @Override public Artifacts classes(String groupArtifactClassifier, String v) {
      classJars.add(new MvnJar(Gav.from(groupArtifactClassifier, v), MvnJar.Kind.Classes));
      return this;
    }

    @Override public Artifacts sources(String gac, String v) {
      sourceJars.add(new MvnJar(Gav.from(gac, v), MvnJar.Kind.Sources));
      return this;
    }

    @Override public Artifacts transitive(String module) {
      transitive.add(parseName(module));
      return this;
    }

    @Override public Artifacts noSources() {
      this.noSources = true;
      return this;
    }

    @Override public Artifacts repackage() {
      this.repackage = true;
      return this;
    }

    private JavaVendorModule toModule() {
      if (classJars.isEmpty()) throw new FatalException(
          "At least one classes jar have to be specified for module `%s`"
              .formatted(moduleName));

      if (!repackage && classJars.size() != 1) throw new FatalException(
          "If many class artifacts are specified, module `%s` have to be repackages"
              .formatted(moduleName));

      if (noSources && !sourceJars.isEmpty()) throw new FatalException(
          "If noSources(), then sources() should not be included");

      if (sourceJars.isEmpty() && !noSources) {
        for (var j : classJars) {
          sourceJars.add(new MvnJar(j.gav(), MvnJar.Kind.Sources));
        }
      }

      return new JavaVendorModule(
          moduleName,
          classJars,
          sourceJars,
          transitive,
          repackage);
    }
  }

  private static ModuleName parseName(String module) {
    return ModuleName.tryParse(module)
        .orElseThrow(() ->
            new FatalException(("Module name '%s' is in wrong format, "
                + "should be java module name with dot separators.").formatted(module)));
  }
}
