package io.immutables.build;

import io.immutables.build.build.Dependencies;
import io.immutables.build.build.Import;
import io.immutables.build.build.SourceModule;
import io.immutables.build.build.Sources;
import io.immutables.stencil.Current;
import io.immutables.stencil.Directory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

// Modules and versions are a copy-paste from build/Build
// just don't know any better now

public class Release {
	public static void main(String[] args) throws IOException {
		Import.modules();
		Sources.scanSources("src");
		Dependencies.resolve();

		var published = List.of(
				"io/immutables/meta",
 			"that",
			"common",
			"codec",
			"codec.jackson",
			"regres",
			"stencil",
			"stencil.template");
		// "declaration",
		// "declaration.processor"

		render(new File(".").getCanonicalFile().toPath(),
			published.stream()
				.map(p -> new GenModule(p,
					(SourceModule) Dependencies.get("io.immutables." + p)))
				.toList());
	}

	private static void render(Path root, List<GenModule> modules) {
		var g = Current.use(new Directory(root), Gradles_generator::new);
		g.generate(modules);
	}
}
