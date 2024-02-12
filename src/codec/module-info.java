import io.immutables.codec.record.DefaultMetadataProvider;
import io.immutables.codec.record.MetadataProvider;

module io.immutables.codec {
	requires static io.immutables.meta;
	requires static javax.annotation.jsr305;

	exports io.immutables.codec;
	exports io.immutables.codec.record;
	exports io.immutables.codec.record.meta;

	uses MetadataProvider;

	provides MetadataProvider with DefaultMetadataProvider;
}
