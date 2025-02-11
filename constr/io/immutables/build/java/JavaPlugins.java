package io.immutables.build.java;

import io.immutables.build.FatalException;
import io.immutables.build.Http;
import io.immutables.build.ModuleName;
import io.immutables.build.ModuleUnit;
import io.immutables.build.Target;
import io.immutables.build.Workspace;
import io.immutables.build.mvn.Mvn;
import io.immutables.build.mvn.MvnJar;
import io.immutables.common.WithFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class JavaPlugins {
  public static void vendor(Workspace workspace) {
    for (var m : workspace.modules()) {
      if (m instanceof JavaVendorModule module) {
        prepare(workspace, module);
      }
    }
  }

  public static void source(Workspace workspace) {
    for (var m : workspace.modules()) {
      if (m instanceof JavaSourceModule module) {
        prepare(workspace, module);
      }
    }
  }

  private static void prepare(Workspace workspace, JavaVendorModule module) {
    var buildOutputDir = workspace.buildOutputDirFor(module.name());

    var targets = workspace.executorFor(module.name());

    var downloadsDir = buildOutputDir.resolve("mvn");
    var moduleDir = buildOutputDir.resolve("module");
    var tmpDir = buildOutputDir.resolve("tmp");

    Target<List<Dependency>> dependencies = targets.sync("dependencies", () ->
        module.transitive().stream()
            .map(t -> new Dependency(t, Dependency.Kind.Transitive))
            .toList());

    Target<Path> moduleJarFile = targets.async("moduleJarFile", () -> {
      var http = workspace.toolOf(Http.class)
          .orElseThrow(/*TODO proper message*/);

      for (var j : module.classJars()) ensureDownloaded(http, downloadsDir, j);
      for (var j : module.sourceJars()) ensureDownloaded(http, downloadsDir, j);

      return ensureVendored(module, tmpDir, moduleDir, downloadsDir);
    });

    Target<Set<ModuleName>> allRuntimeModules = targets.sync("allRuntimeModules", () -> {
      var result = new HashSet<ModuleName>();
      result.add(module.name());
      collectRuntimeModules(workspace, module, result);
      return Set.copyOf(result);
    });

    // For the vendored module, module path can just use module jar
    // When building modules from source, we can have module path without the need
    // to package classes into a module jar, so it will be the other way around
    Target<Path> modulePath = targets.future("modulePath", moduleJarFile::asFuture);

    module.addFacet(new JavaModuleFacet() {
      @Override public Target<List<Dependency>> dependencies() {
        return dependencies;
      }

      @Override public Target<Set<ModuleName>> allRuntimeModules() {
        return allRuntimeModules;
      }

      @Override public Target<Path> modulePath() {
        return modulePath;
      }

      @Override public Target<Path> moduleJarFile() {
        return moduleJarFile;
      }
    });

    module.addAction("vendored", (out, err, arguments) -> {
      err.println("=======================================");
      err.println(module.name());
      err.println("---------------------------------------");
      err.println(moduleJarFile.get());
      return true;
    });
  }

  private static void prepare(Workspace workspace, JavaSourceModule module) {
    var buildOutputDir = workspace.buildOutputDirFor(module.name());

    var targets = workspace.executorFor(module.name());

    var classesDir = buildOutputDir.resolve("classes");

    Target<List<Path>> sources = targets.sync("sources", () -> {
      var allModuleInfos = WithFiles.dir(module.dir())
          .include("**/module-info.java")
          .toList();

      var nestedModulesDirs = allModuleInfos.stream()
          .map(Path::getParent)
          .filter(parent -> !parent.equals(module.dir()))
          .toList();

      return WithFiles.dir(module.dir())
          .include("**/*.java")
          .with(self -> {
            for (var nested : nestedModulesDirs) {
              self.exclude(module.dir().relativize(nested) + "/**");
            }
          })
          .toList();
    });

    Target<List<Dependency>> dependencies = targets.sync("dependencies", () -> {
      var deps = new LinkedHashMap<ModuleName, Dependency>();

      for (var require : module.info().requires()) {
        var name = require.name();

        if (isReservedJavaModule(name)) continue;

        var kind = kindOf(require);
        var existing = deps.put(name, new Dependency(name, kind));

        assert existing == null
            : "Module definition should not allow duplicate [direct] dependencies";
      }

      BiFunction<Dependency, Dependency, Dependency> mergeIndirect = (old, newv) -> {
        assert newv.isIndirect();

        // non-static will win
        if (old.isStatic() && !newv.isStatic()) return newv;

        // in most cases direct dependency always wins, ignoring indirect dependency,
        // but the above we handle a case where direct dependency is static and indirect is not.
        // Not sure if it is a valid use case in a real world.
        // Also, we're ignoring the possibility to replace direct dependency
        // with indirect transitive one.
        if (!old.isIndirect()) return old;

        // transitive variant will win
        if (!old.isTransitive() && newv.isTransitive()) return newv;

        // by default, let old value stick
        return old;
      };

      // Now we go over direct dependencies to pull any transitive dependencies
      // This doesn't require recursion, dependencies list will already contain
      // all indirect dependencies, we will be picking only transitive ones
      for (var directDependency : deps.values()) {
        var propagatedKind = propagatesAs(directDependency.kind());

        var unit = workspace.moduleBy(directDependency.module());
        if (unit.isEmpty()) {
          System.err.println("Missing module: " + directDependency.module());
          continue;
        }

        var dependenciesOfDependency = unit
            .orElseThrow(/*TODO proper message*/)
            .facet(JavaModuleFacet.class)
            .orElseThrow(/*TODO proper message*/)
            .dependencies()
            .get(); // <-- computing, ok to not parallelize

        for (var d : dependenciesOfDependency) {
          if (d.isTransitive()) {
            deps.merge(d.module(), new Dependency(d.module(), propagatedKind), mergeIndirect);
          }
        }
      }

      return List.copyOf(deps.values());
    });

    module.addFacet(new JavaSourcesFacet() {

      @Override public Path dir() {
        return module.dir();
      }

      @Override public Path moduleInfoFile() {
        return module.moduleInfoFile();
      }

      @Override public Target<List<Path>> sources() {
        return sources;
      }
    });

    // This is actual compilation
    Target<Path> modulePath = targets.future("modulePath", () -> {
      var javaTools = workspace.toolOf(JavaTools.class).orElseThrow();

      return JavacCompile.compile(
          workspace, module, dependencies, javaTools, classesDir, sources);
    });

    Target<Set<ModuleName>> allRuntimeModules = targets.sync("allRuntimeModules", () -> {
      var result = new HashSet<ModuleName>();
      result.add(module.name());
      collectRuntimeModules(workspace, module, result);
      return Set.copyOf(result);
    });

    module.addFacet(new JavaModuleFacet() {
      @Override public Target<List<Dependency>> dependencies() {
        return dependencies;
      }

      @Override public Target<Set<ModuleName>> allRuntimeModules() {
        return allRuntimeModules;
      }

      @Override public Target<Path> modulePath() {
        return modulePath;
      }

      @Override public Target<Path> moduleJarFile() {
        return null;
      }
    });

    module.addAction("sources", (out, err, arguments) -> {
      err.println("=======================================");
      err.println(module.name());
      err.println("---------------------------------------");
      for (var p : sources.get()) {
        err.println(p);
      }
      return true;
    });

    module.addAction("dependencies", (out, err, arguments) -> {
      err.println("=======================================");
      err.println(module.name());
      err.println("---------------------------------------");
      for (var p : dependencies.get()) {
        err.println(p);
      }
      return true;
    });

    module.addAction("compile", (out, err, arguments) -> {
      err.println("=======================================");
      err.println(module.name());
      err.println("---------------------------------------");
      err.println(modulePath.get());
      return true;
    });
  }

  private static boolean isReservedJavaModule(ModuleName name) {
    return name.name().startsWith("java.");
  }

  private static void collectRuntimeModules(
      Workspace workspace, ModuleUnit from, Set<ModuleName> result) {
    var dependencies = from.facet(JavaModuleFacet.class)
        .orElseThrow(/*TODO message*/)
        .dependencies()
        .get(); // <- computing here, it's ok expected to be sync

    for (var d : dependencies) {
      if (d.isStatic()) continue;
      if (result.add(d.module())) {
        var module = workspace.moduleBy(d.module()).orElseThrow(/*TODO message*/);
        collectRuntimeModules(workspace, module, result);
      }
    }
  }

  private static Dependency.Kind propagatesAs(Dependency.Kind kind) {
    return switch (kind) {
      case Transitive -> Dependency.Kind.IndirectTransitive;
      case Static -> Dependency.Kind.IndirectStatic;
      case Direct -> Dependency.Kind.Indirect;
      case Indirect, IndirectStatic, IndirectTransitive -> throw new AssertionError(kind);
    };
  }

  private static Dependency.Kind kindOf(ModuleInfo.Require require) {
    if (require.isStatic()) return Dependency.Kind.Static;
    if (require.isTransitive()) return Dependency.Kind.Transitive;
    return Dependency.Kind.Direct;
  }

  private static Path downloadedFile(Path downloadsDir, MvnJar jar) {
    return downloadsDir.resolve(jar.gav().group()).resolve(jar.toFilename());
  }

  private static Path vendoredFile(Path modulesDir, ModuleName module) {
    return modulesDir.resolve(module + Mvn.EXT_JAR);
  }

  private static void ensureDownloaded(Http http, Path downloadsDir, MvnJar jar)
      throws IOException, InterruptedException {
    var targetFile = downloadedFile(downloadsDir, jar);

    if (Files.exists(targetFile)) return;

    Files.createDirectories(targetFile.getParent());

    var uri = jar.toMvnUri();

    http.fetch(uri, targetFile);

    System.err.printf("Fetched %s (%.1fkb)%n", uri, Files.size(targetFile) / 1024f);
  }

  private static Path ensureVendored(
      JavaVendorModule module, Path tmpDir, Path moduleDir, Path downloadsDir) throws IOException {
    var vendoredFile = vendoredFile(moduleDir, module.name());

    if (Files.exists(vendoredFile)) return vendoredFile;

    Files.createDirectories(vendoredFile.getParent());

    var downloadedJars = module.classJars().stream()
        .map(j -> downloadedFile(downloadsDir, j))
        .toList();

    if (module.repackage()) {
      Repackage.mergeAutomaticModule(tmpDir, vendoredFile, module.name(),
          downloadedJars);
    } else if (downloadedJars.size() == 1) {
      Files.copy(
          downloadedJars.getFirst(),
          vendoredFile,
          StandardCopyOption.REPLACE_EXISTING);
    } else throw new FatalException(
        ("Library module `%s` is not exactly 1 jar. Needs one jar or requires repackaging (" +
            ".repackage())")
            .formatted(module.name()));

    return vendoredFile;
  }
}
