// @processor io.immutables.stencil.template
// @option io.immutables.dir=[module.dir]
module io.immutables.declaration.processor {
	requires static java.compiler;

	// These 2 are for annotations, but cannot be static as we need those
	// classes at annotation processor runtime
	requires io.immutables.meta;
	requires io.immutables.declaration;

	requires io.immutables.stencil;
	requires io.immutables.stencil.template;
	//	requires io.immutables.codec;
	//	requires io.immutables.codec.jackson;
/*	provides javax.annotation.processing.Processor
		with io.immutables.declaration.processor.Processor;*/
}
