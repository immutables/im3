// @processor io.immutables.stencil.template
// @option io.immutables.dir=[module.dir]
module dev.declaration.processor {
	requires static java.compiler;
	//requires static javax.annotation.jsr305;

	requires dev.declaration;

	requires io.immutables.codec;
	requires io.immutables.meta;
	requires io.immutables.common;
	requires io.immutables.stencil;
	requires io.immutables.stencil.template;

	provides javax.annotation.processing.Processor
		with dev.declaration.processor.Processor;
}
