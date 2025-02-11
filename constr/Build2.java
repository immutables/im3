import static io.immutables.build.Entrypoint.java;
import static io.immutables.build.java.SourceModules.sourcesCompactLayout;
import static io.immutables.build.java.VendoredModules.vendor;

interface Ver {
  String Guava = "32.1.3-jre";
  String Postgres = "42.6.0";
  String Immutables = "2.10.0";
  String Jsr305 = "3.0.1";
  String Jackson = "2.15.3";
  String Spring = "6.0.9";
}

public static void main(String... args) {
  java()
      .use(vendor(lib -> {
        lib.module("com.google.common")
            .classes("com.google.guava:guava", Ver.Guava);

        lib.module("org.postgresql.jdbc")
            .classes("org.postgresql:postgresql", Ver.Postgres);

        lib.module("org.immutables.value")
            .classes("org.immutables:value", Ver.Immutables)
            .noSources();

        lib.module("org.immutables.value.annotations")
            .classes("org.immutables:value-annotations", Ver.Immutables);

        lib.module("javax.annotation.jsr305")
            .classes("com.google.code.findbugs:jsr305", Ver.Jsr305)
            .sources("com.google.code.findbugs:jsr305", Ver.Jsr305)
            .repackage();

        lib.module("org.junit.junit4")
            .classes("junit:junit", "4.12")
            .classes("org.hamcrest:hamcrest-core", "1.3")
            .repackage();

        lib.module("com.fasterxml.jackson.core")
            .classes("com.fasterxml.jackson.core:jackson-core", Ver.Jackson);

        lib.module("com.fasterxml.jackson.databind")
            .classes("com.fasterxml.jackson.core:jackson-databind", Ver.Jackson);

        lib.module("com.fasterxml.jackson.annotation")
            .classes("com.fasterxml.jackson.core:jackson-annotations", Ver.Jackson);

        lib.module("spring.web")
            .classes("org.springframework:spring-web", Ver.Spring);
      }))
      .use(sourcesCompactLayout("src"))
      .run(args);
}
