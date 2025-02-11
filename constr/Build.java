interface Build {
  interface Ver {
    String Immutables = "2.10.0";
    String Jsr305 = "3.0.1";
    String Jackson = "2.15.3";
    String Guava = "32.1.3-jre";
    String Postgres = "42.6.0";
    String Spring = "6.0.9";
  }

  static void main(String... args) {
 /*   var manager = ModuleManager.Instance;

    manager.library("org.immutables.value", a -> a
        .classes("org.immutables:value", Ver.Immutables)
        .noSources());

    manager.library("org.immutables.value.annotations", a -> a
        .classes("org.immutables:value-annotations", Ver.Immutables));

    manager.library("javax.annotation.jsr305", a -> a
        .classes("com.google.code.findbugs:jsr305", Ver.Jsr305)
        .sources("com.google.code.findbugs:jsr305", Ver.Jsr305)
        .repackage());

    manager.library("org.junit.junit4", a -> a
        .classes("junit:junit", "4.12")
        .classes("org.hamcrest:hamcrest-core", "1.3")
        .repackage());

    manager.library("com.fasterxml.jackson.core", a -> a
        .classes("com.fasterxml.jackson.core:jackson-core", Ver.Jackson));

    manager.library("com.fasterxml.jackson.databind", a -> a
        .classes("com.fasterxml.jackson.core:jackson-databind", Ver.Jackson));

    manager.library("com.fasterxml.jackson.annotation", a -> a
        .classes("com.fasterxml.jackson.core:jackson-annotations", Ver.Jackson));

    manager.library("com.google.common", a -> a
        .classes("com.google.guava:guava", Ver.Guava));

    manager.library("org.postgresql.jdbc", a -> a
        .classes("org.postgresql:postgresql", Ver.Postgres));

    manager.library("spring.web", a -> a
        .classes("org.springframework:spring-web", Ver.Spring));

    try {
      manager.scanSources("src");
      manager.prepareTargets();

      for (var m : manager.sourceModules) {
        //var sources = manager.get(m.name()).as(ModulePathFacet.class);

        //out.println(m.name() + "> " + sources.modulePath().get());

        // var compiledPath = manager.get(m.name()).as(ModulePathFacet.class).modulePath().get();
        //
        // System.out.println(compiledPath);
      }
      */
/*    for (var m : manager.vendorModules) {
        manager.get(m.name()).as(ModulePathFacet.class).modulePath().get();
      }
*/
   /*   Idea.libraries();
      Idea.modules();
      Idea.compiler();
    } catch (FatalException e) {
      err.println(e.getMessage());
    } catch (RuntimeException e) {
      if (e.getCause() instanceof FatalException f) {
        err.println(e.getMessage());
      } else {
        e.printStackTrace();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } */
  }
}
