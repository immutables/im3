import io.immutables.codec.record.DefaultMetadataProvider;
import io.immutables.codec.record.MetadataProvider;

module io.immutables.codec {
	requires static io.immutables.meta;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.annotation;
	requires com.google.common;
	requires io.immutables.common;
	exports io.immutables.codec;
	exports io.immutables.codec.record;
	exports io.immutables.codec.jackson;

	uses MetadataProvider;
	provides MetadataProvider with DefaultMetadataProvider;
}
