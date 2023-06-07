package io.immutables.build;

import io.immutables.build.build.*;
import io.immutables.stencil.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Generator
public abstract class Gradles extends Template {
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
			.exclude("module-info.java")
			.copyTo("rel/mod/", module.path(), "/src/main/java/io/immutables/",
				module.path());

		Path pathToGenerated;
		try {
			pathToGenerated = files.root().resolve(
					".build/generated/" + module.shortName() + "/_annotations");
			if (!Files.exists(pathToGenerated)) return;

			pathToGenerated = pathToGenerated.toRealPath();

			//var p = Files.readSymbolicLink(pathToGenerated);
			//System.out.println(p);
			//pathToGenerated = p;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		files.dir(pathToGenerated)
			.include("**/*.java")
			.copyTo("rel/mod/", module.path(), "/src/main/java");
	}
}
