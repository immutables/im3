
module dev.declaration.compiler {
  requires static javax.annotation.jsr305;
  requires io.immutables.meta;

  // in question???
  requires dev.declaration;

  // requires io.immutables.codec;
  // requires io.immutables.codec.jackson;
  // requires spring.web;
}
