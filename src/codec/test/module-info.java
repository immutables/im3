open module io.immutables.codec.test {
	requires io.immutables.meta;
	requires io.immutables.common;
	requires io.immutables.codec;
	requires io.immutables.codec.jackson;

	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.annotation;

	requires io.immutables.that;
	requires org.junit.junit4;

	exports io.immutables.codec.test;
}
