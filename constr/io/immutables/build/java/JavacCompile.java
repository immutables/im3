package io.immutables.build.java;

import io.immutables.build.FatalException;
import io.immutables.build.ModuleName;
import io.immutables.build.Target;
import io.immutables.build.Workspace;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

final class JavacCompile {
  private JavacCompile() {}

  static CompletableFuture<Path> compile(
      Workspace workspace, JavaSourceModule module,
      Target<List<Dependency>> dependencies,
      JavaTools tools, Path classesDir, Target<List<Path>> sources)
      throws ExecutionException, InterruptedException {

    // this one is simple sync target
    var declaredDeps = dependencies.get();
    // simple sync target too
    var sourcePaths = sources.get();

    var processorFutures = processorDeps(workspace, module).stream()
        .map(name -> moduleFacet(workspace, name)
            .modulePath().asFuture())
        .toList();

    var requiresFutures = declaredDeps.stream()
        .map(d -> moduleFacet(workspace, d.module())
            .modulePath().asFuture())
        .toList();

    var allDepsFutures = new ArrayList<CompletableFuture<?>>();
    allDepsFutures.addAll(processorFutures);
    allDepsFutures.addAll(requiresFutures);

    return CompletableFuture.allOf(
        allDepsFutures.toArray(CompletableFuture<?>[]::new)
    ).thenApply(_ -> {
      // here all futures will be resolved, so we can "get" them
      var args = new JavacArgs()
          .withParameters()
          .addModuleSourcePath(module.name() + "=" + module.dir())
          .destination(classesDir);

      sourcePaths.forEach(args::addSource);

      module.info().options()
          .forEach(args::addAnnotationOption);

      for (var p : processorFutures) {
        args.addProcessorModulePath(p.resultNow());
      }

      for (var p : requiresFutures) {
        args.addModulePath(p.resultNow());
      }

      int exitCode = tools.javac()
          .run(System.out, System.err, args.toArray());

      if (exitCode != 0) throw new FatalException(
          "Failed to compile (%d) module: %s".formatted(exitCode, module.name()));

      return classesDir;
    });
  }

  private static Set<ModuleName> processorDeps(Workspace workspace, JavaSourceModule module) {
    var processorDeps = new HashSet<ModuleName>();
    for (var p : module.info().processors()) {
      var runtimeDeps = moduleFacet(workspace, p.name())
          .allRuntimeModules()
          .get(); //<- sync computing

      processorDeps.addAll(runtimeDeps);
    }
    return processorDeps;
  }

  private static JavaModuleFacet moduleFacet(Workspace workspace, ModuleName name) {
    return workspace.moduleBy(name)
        .orElseThrow(/*TODO message*/)
        .facet(JavaModuleFacet.class)
        .orElseThrow(/*TODO message*/);
  }
}
