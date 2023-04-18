module io.immutables.codec.jackson {
	requires static io.immutables.meta;
	requires io.immutables.codec;

	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.annotation;
	exports io.immutables.codec.jackson;
}
