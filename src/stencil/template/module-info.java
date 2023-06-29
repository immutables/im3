module io.immutables.stencil.template {
	requires static java.compiler;

	// These 2 are for annotations, but cannot be static as we need those
	// classes at annotation processor runtime
	requires io.immutables.meta;
	requires io.immutables.stencil;
	//	requires io.immutables.codec;
	//	requires io.immutables.codec.jackson;

	exports io.immutables.stencil.template;

	provides javax.annotation.processing.Processor with io.immutables.stencil.template.Processor;
}
