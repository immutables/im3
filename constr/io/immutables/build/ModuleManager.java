package io.immutables.build;

public class ModuleManager {
/*  public final List<JavaSourceModule> sourceModules = new ArrayList<>();
  public final List<JavaVendorModule> vendorModules = new ArrayList<>();
  public final Dependencies dependencies = new Dependencies();
  private final Map<ModuleName, ModuleUnit> modules = new HashMap<>();

  public void scanSources(String modulesPath) {
    var searchPath = Path.of(modulesPath);

    class Visitor extends SimpleFileVisitor<Path> {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return dir.getFileName().startsWith(".")
            ? FileVisitResult.SKIP_SUBTREE
            : FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (attrs.isRegularFile()
            && file.getFileName().toString().equals(JavaPatterns.MODULE_INFO_JAVA)) {
          try {
            readModule(file, searchPath);
          } catch (IOException | ModuleInfo.MalformedModuleException e) {
            throw new FatalException(e.getMessage());
          }
          return FileVisitResult.CONTINUE;
        }
        return FileVisitResult.CONTINUE;
      }
    }

    try {
      Files.walkFileTree(searchPath, Set.of(), MAX_DEPTH, new Visitor());
    } catch (IOException e) {
      throw new FatalException(e.getMessage());
    }
  }

  public FacetedModule get(String name) {
    return get(ModuleName.tryParse(name)
        .orElseThrow(() -> new FatalException("Wrong module syntax: " + name)));
  }

  @Override public FacetedModule get(ModuleName name) {
    @Null var m = modules.get(name);
    if (m == null) throw new FatalException("Module not found " + name);
    return m;
  }

  public void prepareTargets() {
    dependencies.resolve(vendorModules, sourceModules);

    var javac = ToolProvider.findFirst("javac")
        .orElseThrow(() -> new FatalException("No `javac` tool found"));

    var tools = new BuildTools(javac);

    // TODO check for duplicates in a single place,
    // dependencies.resolve also does this
    for (var v : vendorModules) {
      var buildModule = DIBDIBJavaLibraryModules.create(v, dependencies, this, tools);
      @Null var existing = modules.put(v.name(), buildModule);
    }

    for (var m : sourceModules) {
      var buildModule = DIBDIBJavaSourceModules.create(m, dependencies, this, tools);
      @Null var existing = modules.put(m.name(), buildModule);
    }
  }

  public void library(String libraryName, Consumer<Artifacts> collectArtifacts) {
    var moduleName = ModuleName.tryParse(libraryName)
        .orElseThrow(() -> new FatalException(
            "Library declaration has wrong module-name syntax: " + libraryName));

    var classJars = new ArrayList<MvnJar>();
    var sourceJars = new ArrayList<MvnJar>();

    var handle = new Artifacts() {
      boolean repackage;
      boolean noSources;

      @Override public Artifacts classes(String gac, String v) {
        classJars.add(new MvnJar(gav(gac, v), MvnJar.Kind.Classes));
        return this;
      }

      @Override public Artifacts sources(String gac, String v) {
        sourceJars.add(new MvnJar(gav(gac, v), MvnJar.Kind.Sources));
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
    };

    collectArtifacts.accept(handle);

    if (classJars.isEmpty()) throw new FatalException(
        "At least one classes jar have to be specified for module `%s`"
            .formatted(moduleName));

    if (!handle.repackage && classJars.size() != 1) throw new FatalException(
        "If many class artifacts are specified, module `%s` have to be repackages"
            .formatted(moduleName));

    if (handle.noSources && !sourceJars.isEmpty()) throw new FatalException(
        "If noSources(), then sources() should not be included");

    if (sourceJars.isEmpty() && !handle.noSources) {
      for (var j : classJars) {
        sourceJars.add(new MvnJar(j.gav(), MvnJar.Kind.Sources));
      }
    }

    vendorModules.add(new VendorModuleInfo(
        moduleName,
        List.copyOf(classJars),
        List.copyOf(sourceJars),
        handle.repackage));
  }

  private static Gav gav(String gac, String v) {
    var parts = gac.split(":");
    return switch (parts.length) {
      case 2 -> new Gav(parts[0], parts[1], v);
      case 3 -> new Gav(parts[0], parts[1], v, parts[2]);
      default -> throw new FatalException(
          "wrong format '%s' =/=> group:artifact[:classifier]".formatted(gac));
    };
  }

  private void readModule(Path file, Path searchPath)
      throws IOException, ModuleInfo.MalformedModuleException {

    var dir = file.getParent();

    var projectDir = Path.of(".").toAbsolutePath().normalize();
    //FIXME will it work without absolute path
    // var moduleDir = dir.toAbsolutePath();
    var moduleInfo = ModuleInfo.parse(file, dir, projectDir);

    sourceModules.add(new SourceModule(dir, searchPath.relativize(dir), file, moduleInfo));
  }

  public static final ModuleManager Instance = new ModuleManager();

  private static final int MAX_DEPTH = 30;*/
}
