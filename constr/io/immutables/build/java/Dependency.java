package io.immutables.build.java;

import io.immutables.build.ModuleName;

/// Dependency pairs module name with the kind
public record Dependency(ModuleName module, Kind kind) {

  enum Kind {
    /// Simple direct dependency. Compile and runtime, does not propagate.
    Direct,
    /// Static dependency, compile only. Static dependencies cannot be transitive
    /// and do not propagate.
    Static,
    /// Transitive dependency propagates to anyone dependent on this module
    Transitive,
    /// Indirect dependency propagated from dependencies' transitive dependencies.
    /// Doesn't propagate, but [#IndirectTransitive] does.
    Indirect,
    /// Indirect dependency which would also propagate to anyone dependent on this module,
    /// as these come via this module's transitive dependency
    IndirectTransitive,
    /// Indirect dependency, which comes as transitive of our static dependency. It will not
    /// propagate to anyone dependent on this module.
    IndirectStatic
  }

  public boolean isStatic() {
    return switch (kind) {
      case Static, IndirectStatic -> true;
      default -> false;
    };
  }

  public boolean isTransitive() {
    return switch (kind) {
      case Transitive, IndirectTransitive -> true;
      default -> false;
    };
  }

  public boolean isIndirect() {
    return switch (kind) {
      case Indirect, IndirectStatic, IndirectTransitive -> true;
      default -> false;
    };
  }

  @Override public String toString() {
    return "requires " + switch (kind) {
      case Static -> "static ";
      case Transitive -> "transitive ";
      case Indirect -> "[indirectly] ";
      case IndirectTransitive -> "[indirectly] transitive ";
      case IndirectStatic -> "[indirectly] static ";
      default -> "";
    } + module;
  }
}
