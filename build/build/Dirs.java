package build;

import java.nio.file.Path;

interface Dirs {
	Path project = Path.of(".");

	Path lib = Path.of("lib");
	Path build = Path.of(".build");
	Path artifacts = build.resolve("artifacts");
	Path modules = lib.resolve("modules");

	Path idea = Path.of(".idea");
	Path ideaLibraries = idea.resolve("libraries");
	Path ideaModules = idea.resolve("modules");
	Path generatedContent = build.resolve("generated");
	Path generatedTestContent = build.resolve("generated");

	static Path downloaded(Jar jar) {
		return artifacts.resolve(jar.gav().group()).resolve(jar.toFilename());
	}

	static Path vendored(String module) {
		return modules.resolve(module + Mvn.ext_jar);
	}
}
