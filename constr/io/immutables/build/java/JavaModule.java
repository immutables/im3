package io.immutables.build.java;

import io.immutables.build.ModuleName;

public sealed interface JavaModule permits JavaSourceModule, JavaVendorModule {

  ModuleName name();
}
