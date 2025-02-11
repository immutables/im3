package io.immutables.build.java;

import io.immutables.build.FatalException;
import io.immutables.build.BuildProvider;
import io.immutables.build.Workspace;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

public final class SourceModules {
  private SourceModules() {}

  public static BuildProvider sourcesCompactLayout(String path) {
    return new JavaSourceModulesScanner(Path.of(path));
  }

  private record JavaSourceModulesScanner(Path searchPath) implements BuildProvider {
    @Override public void apply(Workspace workspace) {
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
              workspace.addModule(readModule(file));
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

    private JavaSourceModule readModule(Path file)
        throws IOException, ModuleInfo.MalformedModuleException {

      var dir = file.getParent();

      var projectDir = Path.of(".").toAbsolutePath().normalize();
      //FIXME will it work without absolute path
      // var moduleDir = dir.toAbsolutePath();
      var moduleInfo = ModuleInfo.parse(file, dir, projectDir);

      return new JavaSourceModule(moduleInfo, dir, file);//searchPath.relativize(dir)
    }
  }

  private static final int MAX_DEPTH = 30;
}
