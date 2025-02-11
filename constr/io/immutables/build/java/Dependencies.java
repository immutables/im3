package io.immutables.build.java;

import io.immutables.build.ModuleName;
import io.immutables.meta.Null;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import static java.util.stream.Collectors.toUnmodifiableSet;

public final class Dependencies {
  static final Set<ModuleName> reservedJdkModules = Stream.of(
          "java.base",
          "java.sql",
          "java.compiler",
          "java.net.http")
      .map(ModuleName::new)
      .collect(toUnmodifiableSet());

  final Map<ModuleName, JavaModule> definitions = new HashMap<>();
  final Map<ModuleName, Map<ModuleName, Dependency>> dependencies = new HashMap<>();
  final Map<ModuleName, Set<JavaSourceModule>> nesting = new HashMap<>();

  public JavaModule get(ModuleName name) {
    var module = definitions.get(name);
    if (module != null) return module;
    throw new NoSuchElementException("No module named '" + name + "'");
  }

  public JavaModule get(ModuleName name, ModuleInfo forModule) {
    var module = definitions.get(name);
    if (module != null) return module;

    throw new NoSuchElementException(
        "Dependency module '%s' cannot be found for '%s'".formatted(name, forModule.name()));
  }

  public Set<JavaSourceModule> nestedOf(ModuleName name) {
    @Null var n = nesting.get(name);
    return n != null ? n : Set.of();
  }

  /**
   * @deprecated Based on logical name nesting, and should be about folder nesting
   *     or at least we need to make sure these contexts are different.
   */
  @Deprecated
  public Set<JavaSourceModule> nestedOf(JavaSourceModule module) {
    return nestedOf(module.name());
  }

  public Collection<Dependency> dependenciesOf(JavaSourceModule module) {
    return dependenciesOf(module.name());
  }

  public Collection<Dependency> dependenciesOf(ModuleName name) {
    return dependencies.getOrDefault(name, Map.of()).values();
  }

  public Map<ModuleName, Dependency> treeOf(ModuleName name) {
    var all = new HashMap<ModuleName, Dependency>();
    var module = get(name);
    all.put(name, new Dependency(name, Dependency.Kind.Direct));//*static* false/, /*module*//*is transitive*/false, false
    collectDependencies(module, false, false, all);
    return all;
  }

  public void resolve(List<JavaVendorModule> vendorModules, List<JavaSourceModule> JavaSourceModules) {
    // TODO don't care about duplicates yet
    for (var v : vendorModules) definitions.put(v.name(), v);
    for (var s : JavaSourceModules) definitions.put(s.name(), s);

    // collect dependency sets including transitives
    for (var def : definitions.values()) {
      var deps = new HashMap<ModuleName, Dependency>();
      collectDependencies(def, true, false, deps);
      @Null var duplicate = dependencies.put(def.name(), deps);
      // TODO duplicate check
      assert duplicate == null;
    }

    computeNesting(JavaSourceModules);
  }

  private void computeNesting(List<JavaSourceModule> JavaSourceModules) {
    var byFolder = new HashMap<Path, JavaSourceModule>();
    for (var s : JavaSourceModules) {
      byFolder.put(s.dir(), s);
    }

    for (var maybeNested : JavaSourceModules) {
      for (@Null Path parent = maybeNested.dir().getParent();
          parent != null;
          parent = parent.getParent()) {
        @Null var parentModule = byFolder.get(parent);
        if (parentModule != null) {
          nesting.computeIfAbsent(parentModule.name(), k -> new HashSet<>())
              .add(maybeNested);
        }
      }
    }
  }

  private void collectDependencies(
      JavaModule thisModule,
      boolean includeStatic,
      boolean forceStatic,
      Map<ModuleName, Dependency> dependenciesByModule) {

    if (thisModule instanceof JavaSourceModule m) {
      for (var require : m.info().requires()) {
        // we can avoid including static in runtime-only cases, for example
        if (require.isStatic() && !includeStatic) continue;

        if (reservedJdkModules.contains(require.name())) {
          System.err.printf("Reserved dep: %s <- %s%n", require.name(), m.name());
          continue;
        }

        @Null var otherModule = definitions.get(require.name());
        if (otherModule == null) {
          System.err.printf("Missing dep: %s <- %s%n", require.name(), m.name());
          continue;
        }
        // the dependency is considered static if it's either declared static
        // or is a transitive dependency of a static dependency
        boolean isStatic = forceStatic || require.isStatic();

        @Null var existing = dependenciesByModule.get(require.name());
        if (existing != null) {
          if (!(existing.isStatic() && !isStatic)) {
            // The case where we meet the same dependency,
            // but this time it's non-static,
            // so we will reprocess the same dependency again,
            // but replacing with runtime now
            continue;
          }
        }

        dependenciesByModule.put(require.name(),
            new Dependency(require.name(), Dependency.Kind.Direct));
        //*isStatic*/, /*otherModule*//*is transitive*/true, false));

        if (require.isTransitive()) {
          collectDependencies(otherModule, includeStatic, isStatic, dependenciesByModule);
        }
      }
    }
  }
}
