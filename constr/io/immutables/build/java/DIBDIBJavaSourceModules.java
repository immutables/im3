package io.immutables.build.java;

@Deprecated
public final class DIBDIBJavaSourceModules {
  private DIBDIBJavaSourceModules() {}
/*
  public static FacetedModule create(
      JavaSourceModule module,
      Dependencies dependencies,
      ModuleResolver resolver,
      BuildSupport support) {

    var targets = new TargetExecution(module.name().name());
    var build = new BuildModule(module.name());

    Target<Path> buildDir = targets.sync("buildDir", () -> {
      var path = support.buildDirFor(module.name());
      try {
        Files.createDirectories(path);
      } catch (IOException e) {
        throw new FatalException(e.getMessage());
      }
      return path;
    });

    Target<List<Path>> sources = targets.sync("sources", () -> {
      var d = module.dir();
      return WithFiles.dir(d)
          .include("** /*.java")
          .forEach(dependencies.nestedOf(module.name()), (files, nested) -> {
            files.exclude(d.relativize(nested.dir()) + "/**");
          })
          .list();
    });

    Target<Path> modulePath = targets.async("modulePath", () -> {
      // here it's ok to sync wait on something which is sync target
      // we don't need parallelism here, we will need it for dependencies, though

      var classesDir = buildDir.get().resolve("classes");
      var sourcePaths = sources.get();

      var declaredDependencies = dependencies.dependenciesOf(module);

      var processorDeps = new HashSet<ModuleName>();
      for (var p : module.info().processors()) {
        processorDeps.addAll(dependencies.treeOf(p.name()).keySet());
      }

      var processorFutures = processorDeps.stream()
          .map(n -> resolver.get(n).as(JavaModuleFacet.class)
              .modulePath().asFuture())
          .toList();

      var requiresFutures = declaredDependencies.stream()
          .map(d -> resolver.get(d.name()).as(JavaModuleFacet.class)
              .modulePath().asFuture())
          .toList();

      var allDependencies = new ArrayList<CompletableFuture<?>>();
      allDependencies.addAll(processorFutures);
      allDependencies.addAll(requiresFutures);

      allDone(allDependencies).get();

      // after this, if there were no problems,
      // we can ".get()" all dependency futures
      // with no wait, as they are done

      var args = new JavacArgs()
          .withParameters()
          .addModuleSourcePath(module.name() + "=" + module.dir())
          .destination(classesDir);

      sourcePaths.forEach(args::addSource);

      module.info().options()
          .forEach(args::addAnnotationOption);

      for (var p : processorFutures) {
        args.addProcessorModulePath(p.get());
      }

      for (var p : requiresFutures) {
        args.addModulePath(p.get());
      }

      int exitCode = support.javac()
          .run(System.out, System.err, args.toArray());

      if (exitCode != 0) throw new FatalException(
          "Failed to compile (%d) module: %s".formatted(exitCode, module.name()));

      return classesDir;
    });

    return build
        .add(new JavaSourcesFacet() {
          @Override public Path dir() {
            return module.dir();
          }

          @Override public Path moduleInfoFile() {
            return module.moduleInfoFile();
          }

          @Override public Target<List<Path>> sources() {
            return sources;
          }

          @Override public String toString() {
            return JavaSourcesFacet.class.getSimpleName() + '(' + module.name() + ')';
          }
        })
        .add(new JavaModuleFacet() {
          @Override public Target<Path> modulePath() {
            return modulePath;
          }

          @Override public String toString() {
            return JavaModuleFacet.class.getSimpleName() + '(' + module.name() + ')';
          }
        });
  }

  private static CompletableFuture<?> allDone(Collection<CompletableFuture<?>> dependencies) {
    return CompletableFuture.allOf(dependencies.toArray(CompletableFuture<?>[]::new));
  }

  public static void main(String[] args) {
    System.out.println(Runtime.getRuntime().availableProcessors());
  }*/
}
