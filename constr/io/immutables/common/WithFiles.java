package io.immutables.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import static java.util.stream.Collectors.toCollection;

// TODO Incomplete
public class WithFiles {
  private final Path dir;

  private final List<Glob> includes = new ArrayList<>();
  private final List<Glob> excludes = new ArrayList<>();

  WithFiles(Path dir) {
    this.dir = dir;
  }

  public static WithFiles dir(Path path) {
    return new WithFiles(path);
  }

  public WithFiles with(Consumer<WithFiles> consumer) {
    consumer.accept(this);
    return this;
  }

  /// @deprecated use [#with(Consumer)]
  @Deprecated
  public <T> WithFiles forEach(
      Iterable<T> elements,
      BiConsumer<WithFiles, T> consumer) {

    for (var e : elements) {
      consumer.accept(this, e);
    }
    return this;
  }

  public WithFiles include(Object... include) {
    includes.add(Glob.of(concat(include)));
    return this;
  }

  public WithFiles exclude(Object... excludeParts) {
    excludes.add(Glob.of(concat(excludeParts)));
    return this;
  }

  public void copyTo(Object... targetParts) {
    copyTo(Path.of(concat(targetParts)));
  }

  public void copyTo(Path base, Object... targetParts) {
    copyTo(base.resolve(concat(targetParts)));
  }

  public void copyTo(Path targetDir) {
    if (!Files.exists(dir)) return;
    try {
      try (var stream = Files.walk(dir)) {
        var matching = stream.filter(path ->
            matches(dir.relativize(path))).toList();

        for (var source : matching) {
          var relative = dir.relativize(source);
          var targetFile = targetDir.resolve(relative);
          Files.createDirectories(targetFile.getParent());
          Files.copy(source, targetFile,
              StandardCopyOption.REPLACE_EXISTING);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private boolean matches(Path path) {
    var asString = path.toString();
    return (includes.isEmpty()
        || includes.stream().anyMatch(p -> p.matches(asString)))
        && excludes.stream().noneMatch(p -> p.matches(asString));
  }

  private static String concat(Object[] path) {
    var builder = new StringBuilder();
    for (var p : path) {
      builder.append(p);
    }
    return builder.toString();
  }

  public List<Path> toList() {
    var paths = new ArrayList<Path>();
    if (!Files.exists(dir)) return paths;
    try {
      try (var stream = Files.walk(dir)) {
        stream.filter(path -> matches(dir.relativize(path)))
            .collect(toCollection(() -> paths));
      }
      return paths;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
