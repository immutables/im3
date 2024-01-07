package io.immutables.build.build;

import static io.immutables.build.build.Vendored.module;

public interface Import {
	String Immutables = "2.10.0";
	String Jsr305 = "3.0.1";
	String Jackson = "2.15.3";
	String Guava = "32.1.3-jre";
	String Postgres = "42.6.0";
	String Spring = "6.0.9";

	static void modules() {
		//'org.openjdk.jmh:jmh-core:1.37' - Supports Java 21?
		module("org.immutables.value", a -> a
			.classes("org.immutables:value", Immutables)
			.noSources()
		);
		module("org.immutables.value.annotations", a -> a
			.classes("org.immutables:value-annotations", Immutables)
		);
		module("javax.annotation.jsr305", a -> a
			.classes("com.google.code.findbugs:jsr305", Jsr305)
			.sources("com.google.code.findbugs:jsr305", Jsr305)
			.repackage()
		);
		module("org.junit.junit4", a -> a
			.classes("junit:junit", "4.12")
			.classes("org.hamcrest:hamcrest-core", "1.3")
			.repackage()
		);
		module("com.fasterxml.jackson.core", a -> a
			.classes("com.fasterxml.jackson.core:jackson-core", Jackson)
		);
		module("com.fasterxml.jackson.databind", a -> a
			.classes("com.fasterxml.jackson.core:jackson-databind", Jackson)
		);
		module("com.fasterxml.jackson.annotation", a -> a
			.classes("com.fasterxml.jackson.core:jackson-annotations", Jackson)
		);
		module("com.google.common", a -> a
			.classes("com.google.guava:guava", Guava)
		);
		module("org.postgresql.jdbc", a -> a
			.classes("org.postgresql:postgresql", Postgres)
		);
		module("spring.web", a -> a
			.classes("org.springframework:spring-web", Spring)
		);
	}
}
