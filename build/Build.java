import static build.Sources.project;
import static build.Sources.scanSources;
import static build.Vendored.module;
import static build.Vendored.vendor;

interface Ver {
	String Immutables = "2.9.2";
	String Jsr305 = "3.0.1";
	String Jmh = "1.36";
	String Jackson = "2.14.2";
	String Guava = "31.1-jre";
}

interface Build {
	static void main(String... args) throws Exception {

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
		module("com.google.common", a -> a
			.classes("com.google.guava:guava", Ver.Guava)
		);

		vendor();
		scanSources("src");
		project();

		System.out.println("END! " + String.join(", ", args));
	}
}
