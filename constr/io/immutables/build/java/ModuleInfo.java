package io.immutables.build.java;

import io.immutables.build.ModuleName;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import static io.immutables.build.java.JavaPatterns.ModuleDeclaration;
import static io.immutables.build.java.JavaPatterns.ProcessorOptionAnnotation;
import static io.immutables.build.java.JavaPatterns.ProcessorAnnotation;
import static io.immutables.build.java.JavaPatterns.RequiresStatement;

public record ModuleInfo(
    ModuleName name,
    boolean isOpen,
    List<Require> requires,
    List<Processor> processors,
    List<String> options) {

  public record Require(ModuleName name, boolean isStatic, boolean isTransitive) {
    public Require {
      assert !isStatic || !isTransitive;
    }
  }

  /**
   * {@link Processor} This is not a real part of standard module-info syntax, it's doclet-like
   * special comment directive
   */
  public record Processor(ModuleName name) {}

  public static class MalformedModuleException extends Exception {
    final String problem;
    final Path file;

    MalformedModuleException(Path file, String problem) {
      this.problem = problem;
      this.file = file;
    }

    @Override public String getMessage() {
      return file + ": " + problem;
    }
  }

  public static ModuleInfo parse(Path file, Path moduleDir, Path projectDir)
      throws IOException, MalformedModuleException {

    boolean isOpen = false;
    String module = "";

    var requires = new ArrayList<Require>();
    var processors = new ArrayList<Processor>();
    var options = new ArrayList<String>();

    for (var line : Files.readAllLines(file)) {
      Matcher m;

      m = ProcessorAnnotation.matcher(line);
      if (m.matches()) {
        var processorModule = m.group("module");

        var name = ModuleName.tryParse(processorModule)
            .orElseThrow(() -> new MalformedModuleException(
                file, "Processor declaration has wrong module-name syntax: " + processorModule));

        processors.add(new ModuleInfo.Processor(name));
        continue;
      }

      m = ModuleDeclaration.matcher(line);
      if (m.matches()) {
        isOpen = m.group("open") != null;
        module = m.group("module");
        continue;
      }

      m = RequiresStatement.matcher(line);
      if (m.matches()) {
        var requiredModule = m.group("module");
        var isStatic = m.group("static") != null;
        var isTransitive = m.group("transitive") != null;

        var name = ModuleName.tryParse(requiredModule)
            .orElseThrow(() -> new MalformedModuleException(
                file, "Required module has wrong module-name syntax: " + requiredModule));

        requires.add(new ModuleInfo.Require(name, isStatic, isTransitive));
        continue;
      }

      m = ProcessorOptionAnnotation.matcher(line);
      if (m.matches()) {
        options.add(m.group("option"));
        continue;
      }
      // no need for now
      // if (LineComment.matcher(line).matches()) continue;
    }

    if (module.isEmpty()) throw new MalformedModuleException(file, "No module moduleName");

    var finalName = module; // making it effectively final to use in lambda
    var moduleName = ModuleName.tryParse(finalName)
        .orElseThrow(() -> new MalformedModuleException(
            file, "Module declaration has wrong module name syntax: " + finalName));

    // process options, when module moduleName is available etc
    for (var it = options.listIterator(); it.hasNext(); ) {
      var option = it.next();
      option = option
          .replace("[project.dir]", projectDir.toString())
          .replace("[module.dir]", moduleDir.toString())
          .replace("[module.moduleName]", module)
          .replace("\\[", "[") // good manners to allow escapes for our special syntax
          .replace("\\]", "]");

      it.set(option);
    }

    return new ModuleInfo(
        moduleName, isOpen,
        List.copyOf(requires),
        List.copyOf(processors),
        List.copyOf(options));
  }
}
