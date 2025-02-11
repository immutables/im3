package io.immutables.build.java;

public class DIBDIBJavaLibraryModules {
/*  public static FacetedModule create(
      JavaVendorModule module,
      Dependencies dependencies,
      ModuleResolver resolver,
      BuildSupport support) {

    var targets = new TargetExecution(module.name().name());

    var libsDir = support.libsDirFor(module.name());
    var downloadsDir = libsDir.resolve("download");
    var moduleDir = libsDir.resolve("module");

    Target<Void> ensureDownloaded = targets.async("ensureDownloaded", () -> {
      for (var j : module.classJars()) ensureDownloaded(downloadsDir, j);
      for (var j : module.sourceJars()) ensureDownloaded(downloadsDir, j);
      return null;
    });

    Target<Path> modulePath = targets.async("modulePath", () -> {
      // make sure downloaded
      ensureDownloaded.get();

      return ensureVendored(module, support, moduleDir, downloadsDir);
    });

    return new BuildModule(module.name())
        .add(new JavaModuleFacet() {
          @Override public Target<Path> modulePath() {
            return modulePath;
          }

          @Override public String toString() {
            return JavaModuleFacet.class.getSimpleName() + '(' + module.name() + ')';
          }
        });
  }

  private static Path ensureVendored(JavaVendorModule module, BuildSupport support, Path moduleDir,
      Path downloadsDir) throws IOException {
    var vendoredFile = vendoredFile(moduleDir, module.name());

    if (Files.exists(vendoredFile)) return vendoredFile;

    Files.createDirectories(vendoredFile.getParent());

    var downloadedJars = module.classJars().stream()
        .map(j -> downloadedFile(downloadsDir, j))
        .toList();

    if (module.repackage()) {
      Repackage.mergeAutomaticModule(support.tmpDir(), vendoredFile, module.name(),
          downloadedJars);
    } else if (downloadedJars.size() == 1) {
      Files.copy(
          downloadedJars.getFirst(),
          vendoredFile,
          StandardCopyOption.REPLACE_EXISTING);
    } else throw new FatalException(
        "Library module `%s` is not exactly 1 jar. Needs one jar or requires repackaging"
            .formatted(module.name()));

    return vendoredFile;
  }

  private static Path downloadedFile(Path downloadsDir, MvnJar jar) {
    return downloadsDir.resolve(jar.gav().group()).resolve(jar.toFilename());
  }

  private static Path vendoredFile(Path libModuleDir, ModuleName module) {
    return libModuleDir.resolve(module + Mvn.EXT_JAR);
  }

  private static void ensureDownloaded(Path libsDir, MvnJar jar)
      throws IOException, InterruptedException {
    var path = downloadedFile(libsDir, jar);

    if (Files.exists(path)) return;

    Files.createDirectories(path.getParent());

    var uri = jar.toMvnUri();

    Http.fetch(uri, path);

    System.out.printf("Fetched %s (%.1fkb)%n", uri, Files.size(path) / 1024f);
  }*/
}
