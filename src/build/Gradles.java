package io.immutables.build;

import io.immutables.build.build.*;
import io.immutables.meta.Null;
import io.immutables.stencil.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Generator
abstract class Gradles extends Template {
	final FilesStencil files = new FilesStencil();

	ProvidingModule dep(String name, ModuleInfo forModule) {
		return Dependencies.get(name, forModule);
	}

	public abstract void generate(List<GenModule> modules);

	void copyJava(GenModule module) {
		files.dir("src/", module.path())
				.include("**/*.java")
				.exclude("test/**")
				.forEach(module.nested(), (f, nested) ->
						f.exclude(nested.path(), "/**"))
				.exclude("**/module-info.java")
				.copyTo("rel/mod/", module.path(), "/src/main/java/io/immutables/",
						module.path());

		@Null var pathToGenerated = pathToGenerated(module);
		if (pathToGenerated == null) return;

		files.dir(pathToGenerated)
				.include("**/*.java")
				.copyTo("rel/mod/", module.path(), "/src/main/java");
	}

	private @Null Path pathToGenerated(GenModule module) {
		try {
			var path = files.root().resolve(
					".build/generated/" + module.shortName() + "/_annotations");
			if (!Files.exists(path)) return null;
			return path.toRealPath();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
