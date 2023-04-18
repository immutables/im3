module io.immutables.regres {
	requires static io.immutables.meta;
	requires io.immutables.common;
	requires io.immutables.codec;
	requires io.immutables.codec.jackson;

	requires com.fasterxml.jackson.core;
	requires java.sql;

	exports io.immutables.regres;
}
