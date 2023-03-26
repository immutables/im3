module io.immutables.declaration.processor {
	requires java.compiler;
	requires static io.immutables.meta;
	requires io.immutables.common;

	provides javax.annotation.processing.Processor with io.immutables.declaration.processor.Processor;
}
