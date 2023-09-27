package io.immutables.build;

import io.immutables.build.build.Dependencies;
import io.immutables.build.build.SourceModule;
import io.immutables.stencil.Current;
import io.immutables.stencil.Directory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import static io.immutables.build.build.Sources.scanSources;
import static io.immutables.build.build.Vendored.module;

// Modules and versions are a copy-paste from build/Build
// just don't know any better now
interface Ver {
	String Immutables = "2.9.2";
	String Jsr305 = "3.0.1";
	String Jmh = "1.36";
	String Jackson = "2.14.2";
	String Guava = "31.1-jre";
	String Spring = "6.0.9";
	String Postgres = "42.6.0";
}

public class Release {
	public static void main(String[] args) throws IOException {
		module("org.immutables.value", a -> a
			.classes("org.immutables:value", Ver.Immutables)
			.noSources()
		);
		module("org.immutables.value.annotations", a -> a
			.classes("org.immutables:value-annotations", Ver.Immutables)
		);
		module("javax.annotation.jsr305", a -> a
			.classes("com.google.code.findbugs:jsr305", Ver.Jsr305)
			.sources("com.google.code.findbugs:jsr305", Ver.Jsr305)
			.repackage()
		);
		module("org.junit.junit4", a -> a
			.classes("junit:junit", "4.12")
			.classes("org.hamcrest:hamcrest-core", "1.3")
			.repackage()
		);
		module("com.fasterxml.jackson.core", a -> a
			.classes("com.fasterxml.jackson.core:jackson-core", Ver.Jackson)
		);
		module("com.fasterxml.jackson.databind", a -> a
			.classes("com.fasterxml.jackson.core:jackson-databind", Ver.Jackson)
		);
		module("com.fasterxml.jackson.annotation", a -> a
			.classes("com.fasterxml.jackson.core:jackson-annotations", Ver.Jackson)
		);
		module("com.google.common", a -> a
			.classes("com.google.guava:guava", Ver.Guava)
		);
		module("spring.web", a -> a
			.classes("org.springframework:spring-web", Ver.Spring)
		);
		module("org.postgresql.jdbc", a -> a
			.classes("org.postgresql:postgresql", Ver.Postgres)
		);

		scanSources("src");
		Dependencies.resolve();

		var published = List.of(
			"meta",
			"that",
			"common",
			"codec",
			"codec.jackson",
			"regres",
			"stencil",
			"stencil.template",
			"declaration",
			"declaration.processor");

		render(new File(".").getCanonicalFile().toPath(),
			published.stream()
				.map(p -> new GenModule(p,
					(SourceModule) Dependencies.get("io.immutables." + p)))
				.toList());
	}

	private static void render(Path root, List<GenModule> modules) throws IOException {
		var g = Current.use(new Directory(root), Gradles_generator::new);
		g.generate(modules);
	}
}
