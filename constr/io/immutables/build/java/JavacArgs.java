package io.immutables.build.java;

import io.immutables.meta.Null;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import static java.util.Objects.requireNonNull;

//-s
//--processor-module-path

//--module-version
//--release
//--add-modules
public final class JavacArgs {
  // Having no source path (set as empty) source path is a modern norm,
  // to not let in any "accidental" sources into compilation,
  // but sometimes it can be used, for example, to make java sources discoverable
  // by annotation processors
  private @Null String sourcePath;
  private String release = "";
  private String destination = ".";
  private boolean parameters;
  private Charset encoding = StandardCharsets.UTF_8;
  private boolean enablePreview;

  private final List<String> annotationOptions = new ArrayList<>();
  private final StringJoiner classPath = new StringJoiner(File.pathSeparator);
  private final StringJoiner modulePath = new StringJoiner(File.pathSeparator);
  private final StringJoiner processorModulePath = new StringJoiner(File.pathSeparator);
  private final List<String> sources = new ArrayList<>();
  private final List<String> moduleSourcePaths = new ArrayList<>();

  public JavacArgs withParameters() {
    parameters = true;
    return this;
  }

  public JavacArgs enablePreview(boolean enablePreview) {
    this.enablePreview = enablePreview;
    return this;
  }

  public JavacArgs encoding(Charset encoding) {
    this.encoding = requireNonNull(encoding, "encoding");
    return this;
  }

  public JavacArgs addAnnotationOption(String key, String value) {
    annotationOptions.add("-A" + key + "=" + value);
    return this;
  }

  public JavacArgs addAnnotationOption(String key) {
    annotationOptions.add("-A" + key);
    return this;
  }

  public JavacArgs destination(Path path) {
    destination = path.toString();
    return this;
  }

  public JavacArgs addClassPath(String path) {
    classPath.add(path);
    return this;
  }

  public JavacArgs addClassPath(Path path) {
    return addClassPath(path.toString());
  }

  public JavacArgs addModulePath(String path) {
    modulePath.add(path);
    return this;
  }

  public JavacArgs addModulePath(Path path) {
    return addModulePath(path.toString());
  }

  public JavacArgs addProcessorModulePath(String path) {
    processorModulePath.add(path);
    return this;
  }

  public JavacArgs addProcessorModulePath(Path path) {
    return addProcessorModulePath(path.toString());
  }

  public JavacArgs addSource(String source) {
    sources.add(source);
    return this;
  }

  public JavacArgs addSource(Path source) {
    return addSource(source.toString());
  }

  public JavacArgs release(String release) {
    this.release = requireNonNull(release, "release");
    return this;
  }

  public JavacArgs addModuleSourcePath(String moduleSourcePath) {
    this.moduleSourcePaths.add(requireNonNull(moduleSourcePath, "moduleSourcePath"));
    return this;
  }

  public JavacArgs sourcepPath(String sourcePath) {
    this.sourcePath = requireNonNull(sourcePath, "sourcePath");
    return this;
  }

  public String[] toArray() {
    var args = new ArrayList<String>();

    if (!release.isEmpty()) {
      args.add("--release");
      args.add(release);
    }

    if (sourcePath != null) {
      args.add("--source-path");
      args.add(sourcePath);
    }

    if (!moduleSourcePaths.isEmpty()) {
      for (var p : moduleSourcePaths) {
        args.add("--module-source-path");
        args.add(p);
      }
    }

    var cp = classPath.toString();
    if (!cp.isEmpty()) {
      args.add("--class-path");
      args.add(cp);
    }

    var mp = modulePath.toString();
    if (!mp.isEmpty()) {
      args.add("--module-path");
      args.add(mp);
    }

    var pp = processorModulePath.toString();
    if (!pp.isEmpty()) {
      args.add("--processor-module-path");
      args.add(pp);
    }

    args.addAll(annotationOptions);

    if (enablePreview) {
      args.add("--enable-preview");
    }

    args.add("-d");
    args.add(destination);

    if (parameters) {
      args.add("-parameters");
    }

    args.add("-encoding");
    args.add(encoding.toString());

    args.addAll(sources);

    return args.toArray(String[]::new);
  }
}
