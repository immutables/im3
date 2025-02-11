package io.immutables.build.java;

import java.util.regex.Pattern;

public interface JavaPatterns {
  String MODULE_INFO_JAVA = "module-info.java";
  // These patterns require reasonable formatting, not a precise parsing
  Pattern LineComment = Pattern.compile("^\\s*//.*$");

  Pattern SimpleName = Pattern.compile("""
      [a-zA-Z][a-zA-Z0-9_]*""");

  Pattern QualifiedName = Pattern.compile("""
      [a-zA-Z][a-zA-Z0-9_]*(\\s*\\.\\s*[a-zA-Z][a-zA-Z0-9_]*)*""");

  Pattern ModuleDeclaration = Pattern.compile("""
      ^\\s*\
      (?<open>open\\s+)?module\\s+\
      (?<module>[a-zA-Z0-9_.]+)\\s*(\\{\\s*)?$""");

  Pattern RequiresStatement = Pattern.compile("""
      ^\\s*requires\\s+\
      (?<static>static\\s+)?\
      (?<transitive>transitive\\s+)?\
      (?<module>[a-zA-Z0-9._]+)\\s*;\\s*$""");

  Pattern ProcessorAnnotation = Pattern.compile("""
      ^\\s*\
      (?://\\s*)?\
      (?:[*]\\s*)?\
      @processor\\s+\
      (?<module>[a-zA-Z0-9._]+)\\s*""");

  Pattern ProcessorOptionAnnotation = Pattern.compile("""
      ^\\s*\
      (?://\\s*)?\
      (?:[*]\\s*)?\
      @option\\s+\
      (?<option>\\S+)\\s*""");
}
