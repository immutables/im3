package io.immutables.build.idea;

import io.immutables.build.ModuleManager;
import io.immutables.build.java.Dependencies;
import io.immutables.build.java.Dependency;
import io.immutables.build.java.JavaSourceModule;
import io.immutables.build.java.JavaVendorModule;
import io.immutables.build.java.JavaModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.stream.Stream;
import static java.util.stream.Collectors.joining;

public interface Idea {

  /**
   * IntelliJ's dependency scopes
   */
  enum DependencyScope {
    /** test only compile and runtime */
    TEST,
    /** compile only */
    PROVIDED,
    /** compile and runtime */
    COMPILE,
    /** runtime only, no compile */
    RUNTIME,
  }

  private static Path libraryFile(JavaVendorModule m) {
    return Dirs.ideaLibraries.resolve(m.name() + ".xml");
  }

  private static Path moduleFile(JavaSourceModule m) {
    return Dirs.ideaModules.resolve(ijname(m) + ".iml");
  }

  private static Path allModulesFile() {
    return Dirs.idea.resolve("modules.xml");
  }

  private static Path compilerFile() {
    return Dirs.idea.resolve("compiler.xml");
  }

  private static String ijname(JavaSourceModule module) {
    return module.dir()/*FIXME This might be important .relative()*/.toString().replace('/', '.');
  }

  private static String libraryRoot(Path jar) {
    return """
        			<root url="jar://$PROJECT_DIR$/[jar]!/" />
        """.replace("[jar]", jar.toString());
  }

  static String libraryContent(JavaVendorModule module) {
    var classes = Dirs.vendored(module.name());
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <component name="libraryTable">
        	<library name="[name]">
        		<CLASSES>
        [classRoots]
        		</CLASSES>
        		<JAVADOC />
        		<SOURCES>
        [sourceRoots]
        		</SOURCES>
        	</library>
        </component>
        """
        .replace("[name]", module.name().name())
        .replace("[classRoots]", libraryRoot(classes))
        .replace("[sourceRoots]", module.sourceJars().stream()
            .map(sj -> libraryRoot(Dirs.downloaded(sj)))
            .collect(joining("\n")));
  }

  static void compiler() throws IOException {
    writeFile(compilerFile(), compilerContent());
  }

  static void libraries() throws IOException {
/*    for (var m : ModuleManager.InstanceJavaVendorModules) {//Vendored.modules
      writeFile(libraryFile(m), libraryContent(m));
    }*/
  }

  static void modules() throws IOException {
 /*   for (var m : ModuleManager.Instance.JavaSourceModules) {
      writeFile(moduleFile(m), moduleContent(m));
    }*/
    writeFile(allModulesFile(), allModulesContent());
  }

  static void writeFile(Path file, CharSequence content) throws IOException {
    Files.createDirectories(file.getParent());
    Files.writeString(file, content);
  }

  private static boolean hasGenerated(JavaSourceModule m) {
    return !m.info().processors().isEmpty();
  }

  static DependencyScope scope(Dependency dependency, boolean isTest) {
    if (isTest) return DependencyScope.TEST;
    if (dependency.isStatic()) return DependencyScope.PROVIDED;
    return DependencyScope.COMPILE;
  }

  static String moduleContent(JavaSourceModule m) {
    var info = m.info();
    var isTest = isTest(m);

    var dependencies = new StringBuilder();
    for (var d : dependencies().dependenciesOf(m)) {
      addDependency(dependencies, d, scope(d, isTest));
    }

    for (var p : m.info().processors()) {
      // We add this dependency just for compilation/build ordering.
      // we don't need it at compile, actually.
      // Absence of 'require' for processor module in module-info.java
      // will shield from processor classes being imported/auto-completed
    /*  addDependency(dependencies,
          new Dependency(p.name(), true, dependencies().get(p.name(), m.info())),
          DependencyScope.PROVIDED);*/
    }

    // This is super-duper ad-hoc for release
    // will see if there will be any need / chance to generalize it
    // probably never untill new build system will be available
    // if (info.name().name().equals("io.immutables.release")) {
    //   dependencies.append("""
    //       					<orderEntry type="module" module-name="im3" scope="COMPILE" />
    //       """);
    // }

    var generatedContent = new StringBuilder();
    if (hasGenerated(m)) {
      var suffix = isTest ? "_test_annotations" : "_annotations";
      var output = classesOutputOf(m);
      ensureGeneratedContentLinked(m, output, suffix);
      generatedContent.append("""
          		<content url="file://[project]/[generated]">
          			<sourceFolder url="file://[project]/[generated]/[annotations]"
          				generated="true" isTestSource="[isTest]" />
          		</content>
          """
          .replace("[generated]", Dirs.generatedContent.resolve(ijname(m)).toString())
          .replace("[output]", output.toString())
          .replace("[annotations]", suffix));
    }

    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <module type="JAVA_MODULE" version="4">
        	<component name="NewModuleRootManager" inherit-compiler-output="true">
        		<exclude-output />
        		<content url="file://[project]/[dir]">
        			<sourceFolder url="file://[project]/[dir]"
        				isTestSource="[isTest]" packagePrefix="[packagePrefix]" />
        			<!--<excludeFolder url="file://$MODULE_DIR$/../../${mod.path}/${dir}" />-->
        		</content>
        [generatedContent]
        		<orderEntry type="inheritedJdk" />
        		<orderEntry type="sourceFolder" forTests="false" />
        [dependencies]
        	</component>
        </module>
        """
        .replace("[dependencies]", dependencies)
        .replace("[generatedContent]", generatedContent)
        .replace("[project]", "$MODULE_DIR$/../..")
        .replace("[ijname]", ijname(m))
        .replace("[dir]", m.dir().toString())
        .replace("[packagePrefix]", info.name().name())
        .replace("[isTest]", String.valueOf(isTest));
  }
  private static Dependencies dependencies() {
    //return ModuleManager.Instance.dependencies;
    return null;
  }

  private static void ensureGeneratedContentLinked(JavaSourceModule m, Path output, String suffix) {
    Path generatedPath = Dirs.generatedContent.resolve(ijname(m));
    try {
      Files.createDirectories(generatedPath);
      var link = generatedPath.resolve(suffix);
      var target = Path.of("../../..").resolve(output).resolve(suffix);
      System.err.println(link + " --> " + target);
      if (Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
        Files.delete(link);
      }
      Files.createSymbolicLink(link, target);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static boolean isTest(JavaSourceModule m) {
    var n = m.info().name().name();
    return n.endsWith(".test")
        || n.endsWith("_test");
  }

  private static void addDependency(StringBuilder result, Dependency dependency,
      DependencyScope scope) {
   /* if (dependency.module() instanceof JavaSourceModule sm) {
      result.append("""
          			<orderEntry type="module" module-name="[ijname]" scope="[scope]" /><!-- exported="" -->
          """
          .replace("[ijname]", ijname(sm))
          .replace("[scope]", scope.name())
      );
    } else if (dependency.module() instanceof JavaVendorModule vm) {
      result.append("""
          			<orderEntry type="library" name="[library]" scope="[scope]" level="project"/><!-- exported="" -->
          """
          .replace("[library]", dependency.name().name())
          .replace("[scope]", scope.name())
      );
    }*/
  }

  static String allModulesContent() {
    var modulesEntries = new StringBuilder();

    // for (var m : ModuleManager.Instance.JavaSourceModules) {
    //   modulesEntries.append("""
    //       			<module
    //       				fileurl="file://$PROJECT_DIR$/[dir]/[ijname].iml"
    //       				filepath="$PROJECT_DIR$/[dir]/[ijname].iml" />
    //       """
    //       .replace("[dir]", Dirs.ideaModules.toString())
    //       .replace("[ijname]", ijname(m)));
    // }

    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <project version="4">
        	<component name="ProjectModuleManager">
        		<modules>
        			<!-- project-level hardcoded -->
        			<module
        				fileurl="file://$PROJECT_DIR$/.idea/modules/im3.iml"
        				filepath="$PROJECT_DIR$/.idea/modules/im3.iml" />
        [modules]
        		</modules>
        	</component>
        </project>
        """
        .replace("[modules]", modulesEntries);
  }

  static CharSequence compilerContent() {
    var s = """
        <?xml version="1.0" encoding="UTF-8"?>
        <project version="4">
        	<component name="CompilerConfiguration">
        		<option name="BUILD_PROCESS_HEAP_SIZE" value="1024" />
        		<addNotNullAssertions enabled="false" />
        		<wildcardResourcePatterns>
        			<!--<entry name="!?*.java" />-->
        			<entry name="!?*.class" />
        		</wildcardResourcePatterns>
        		<annotationProcessing>
        			<profile default="true" name="Default" enabled="false">
        			</profile>
        [annotationProfiles]
        		</annotationProcessing>
        		<bytecodeTargetLevel target="23" />
        	</component>
        	<!--
        	<component name="JavacSettings">
        		<option name="ADDITIONAL_OPTIONS_OVERRIDE">
        [javacOptions]
        		</option>
        	</component> -->
        </project>
        """;

    var moduleOptions = new StringBuilder();

  /*  for (var m : ModuleManager.Instance.JavaSourceModules) {
      moduleOptions.append("""
          			<module name="[module]" options="[options]" />
          """
          .replace("[module]", ijname(m))
          .replace("[options]", javacOptions(m)));
    }*/

    var annotationProfiles = new StringBuilder();

  //  for (var m : ModuleManager.Instance.JavaSourceModules) {
      // var processors = m.info().processors();
      // if (processors.isEmpty()) continue;

      annotationProfiles.append("""
          			<profile name="[ijname]" enabled="true">
          				<module name="[ijname]" />
          				[options]
          				<sourceOutputDir name="_annotations" />
          				<sourceTestOutputDir name="_test_annotations" />
          				<outputRelativeToContentRoot value="false" />
          				<processorPath useClasspath="false" useProcessorModulePath="true">
          					[processors]
          				</processorPath>
          			</profile>
          """
        //!!!  .replace("[ijname]", ijname(m))
        //!!!  .replace("[options]", options(m))
          );
/*          .replace("[processors]", processors.stream().flatMap(p -> {
                var b = Stream.<ProvidingModule>builder()
                    .add(dependencies().get(p.name(), m.info()));
                for (var d : dependencies().dependenciesOf(p.name())) {
                  if (!d.isStatic()) b.add(d.module());
                }
                return b.build();
              })
              .distinct()
              .map(p -> "<entry name=\"$PROJECT_DIR$/" + compiledModulePath(p) + "\"/>")
              .collect(joining("\n					"))))*/;
   // }

    return s
        .replace("[annotationProfiles]", annotationProfiles)
        .replace("[javacOptions]", ""/* moduleOptions */);
  }

  static CharSequence options(JavaSourceModule m) {
    var builder = new StringBuilder();
    for (var option : m.info().options()) {
      int atAssignedValue = option.indexOf('=');
      String key;
      String value;
      if (atAssignedValue >= 0) {
        key = option.substring(0, atAssignedValue);
        value = option.substring(atAssignedValue + 1);
      } else {
        key = option;
        value = "";
      }
      // no xml escape, hope for the better
      builder.append("<option name=\"")
          .append(key)
          .append("\" value =\"")
          .append(value).append("\" />\n					");
    }
    return builder;
  }

  private static CharSequence javacOptions(JavaSourceModule m) {
    var options = new StringBuilder()
        //	.append(" --release 18")
        .append(" --enable-preview")
        .append(" -parameters");
    //	.append(" -g");

    var ijname = ijname(m);

    var annotationsOutput = Dirs.generatedContent.resolve(ijname).resolve("annotations");

    //options.append(" -d ").append(classesOutputOf(m));
    options.append(" -s ").append(annotationsOutput);

    // Handled in XML <options>, right ?
//  for (var argument : m.info().options()) {
// 		System.out.println("AA!!!  " + argument);
// 		options.append(" -A").append(argument);
// 	}
    //options.append(" --module ").append(m.name());

    //options.append(" -sourcepath ").append(m.dir());

    if (hasGenerated(m)) {
      var path = m.info().processors().stream()
          .map(p -> compiledModulePath(dependencies().get(p.name(), m.info())).toString())
          .collect(joining(":", "'", "'"));

      options.append(" --processor-module-path ").append(path);
    }

	/*	if (!m.moduleInfo().requires().isEmpty()) {
			var path = Dependencies.dependenciesOf(m).stream()
				.map(r -> compiledModulePath(r.module()).toString())
				.collect(joining(":", "'", "'"));

			options.append(" --module-path ").append(path);
		}
*/
    // PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + m.dir() + "/**/*
    // .java");
    // try (var walk = Files.walk(m.dir())) {
    // 	walk.filter(Files::isRegularFile)
    //
    // 		.forEach(f -> options.append(" ").append(f));
    //
    // } catch (Exception e) {
    // 	e.printStackTrace();
    // }

    return options;
  }

  static Path compiledModulePath(JavaModule module) {
/* FIXME
		return switch (module) {
			case JavaSourceModule sm -> classesOutputOf(sm);
			caseJavaVendorModule vm -> Dirs.vendored(vm.name());
		};*/
    if (module instanceof JavaSourceModule sm) return classesOutputOf(sm);
    if (module instanceof JavaVendorModule vm) return Dirs.vendored(vm.name());
    throw new UnsupportedOperationException();
  }

  private static Path classesOutputOf(JavaSourceModule m) {
    return Dirs.idea.resolve(".out")
        .resolve(isTest(m) ? "test" : "production")
        .resolve(ijname(m));
  }
}
