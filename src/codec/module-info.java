module io.immutables.codec {
	requires static io.immutables.meta;
	requires com.fasterxml.jackson.core;
	requires com.google.common;
	requires io.immutables.common;
	exports io.immutables.codec;
	exports io.immutables.codec.record;
	exports io.immutables.codec.jackson;
}
