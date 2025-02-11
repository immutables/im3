package io.immutables.build.java;

import io.immutables.build.FatalException;
import io.immutables.build.Workspace;
import java.util.spi.ToolProvider;

public interface JavaTools {
  ToolProvider javac();

  static void provide(Workspace workspace) {
    var javac = ToolProvider.findFirst("javac")
        .orElseThrow(() -> new FatalException("No `javac` tool found"));

    workspace.addTool(JavaTools.class, new JavaTools() {
      @Override public ToolProvider javac() {
        return javac;
      }
    });
  }
}
