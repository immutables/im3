package io.immutables.build;

import io.immutables.build.java.JavaPlugins;
import io.immutables.build.java.JavaTools;
import io.immutables.meta.Null;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static java.lang.System.err;
import static java.lang.System.out;

public final class Entrypoint {
  private Entrypoint() {}

  public static Launcher java() {
    return new Launcher(
        List.of(JavaTools::provide, Http::provide),
        List.of(JavaPlugins::vendor, JavaPlugins::source)
    );
  }

  public static final class Launcher {
    private final List<BuildProvider> providers = new ArrayList<>();
    private final List<BuildProvider> before;
    private final List<BuildProvider> after;

    private Launcher(List<BuildProvider> before, List<BuildProvider> after) {
      this.before = before;
      this.after = after;
    }

    public Launcher use(BuildProvider provider) {
      providers.add(provider);
      return this;
    }

    private JustWorkspace initializeWorkspace() {
      var workspace = new JustWorkspace(Path.of("."));

      for (var p : before) p.apply(workspace);
      for (var p : providers) p.apply(workspace);
      for (var p : after) p.apply(workspace);

      return workspace;
    }

    public void run(String... args) {
      var originalArgs = Arrays.asList(args);
      var deque = new ArrayDeque<>(originalArgs);
      @Null String next = deque.pollFirst();
      switch (next) {
        case null -> usage(originalArgs, false);
        case "-h", "--help" -> usage(originalArgs, false);
        default -> {
          next = next.strip();
          String module = "";
          String action = next;
          if (next.startsWith(":")) {
            module = next.substring(1);
            next = deque.pollFirst();
            if (next == null) {
              usage(originalArgs, true);
              return;
            }
          }
          perform(module, action, List.copyOf(deque));
        }
      }
    }

    private void usage(List<String> args, boolean mistaken) {
      if (mistaken) {
        err.println("Wrong arguments: " + String.join(" ", args));
      }
      err.println("Usage:");
      err.println("> --help");
      err.println("  To show this usage");
      err.println("> <action> [...arguments]");
      err.println("  An action with optional arguments.");
      err.println("  Workspace or any module supporting it will perform it.");
      err.println("> :<module-name> <action> [...arguments]");
      err.println("  An action with optional arguments directed to specific module.");
    }

    private void perform(String module, String action, List<String> args) {
      var moduleNameOpt = Optional.<ModuleName>empty();
      if (!module.isEmpty()) {
        moduleNameOpt = ModuleName.tryParse(module);
        if (moduleNameOpt.isEmpty()) {
          err.printf("""
              Module name '%s' is in wrong format, should be java module name with dot separators.
              Aborting.%n""", module);
          return;
        }
      }

      var workspace = initializeWorkspace();

      if (moduleNameOpt.isPresent()) {
        var name = moduleNameOpt.get();
        var moduleUnit = workspace.moduleBy(name);
        if (moduleUnit.isEmpty()) {
          err.printf("""
              Module named '%s' not found in workspace.
              Aborting.%n""", name.name());
          return;
        }

        var actionOpt = moduleUnit.get().action(action);

        if (actionOpt.isEmpty()) {
          err.printf("""
              Module named '%s' cannot perform an unknown command <%s>.
              Aborting.%n""", name.name(), action);
          return;
        }

        actionOpt.get().perform(out, err, args);
      } else {
        var workspaceActionOpt = workspace.actionBy(action);

        if (workspaceActionOpt.isPresent()) {
          workspaceActionOpt.get().perform(out, err, args);
        } else {
          int count = 0;

          for (var m : workspace.modules()) {
            var actionOpt = m.action(action);
            if (actionOpt.isPresent()) {
              count++;
              if (!actionOpt.get().perform(out, err, args)) {
                err.println("Aborting due to errors.");
              }
            }
          }

          if (count == 0) {
            err.printf("No modules have command <%s> to perform. Aborting.%n", action);
          }
        }
      }
    }
  }
}
