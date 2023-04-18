import io.immutables.codec.record.DefaultMetadataProvider;
import io.immutables.codec.record.MetadataProvider;

module io.immutables.codec {
	requires static io.immutables.meta;
	requires io.immutables.common;

	exports io.immutables.codec;
	exports io.immutables.codec.record;

	uses MetadataProvider;
	provides MetadataProvider with DefaultMetadataProvider;
}
