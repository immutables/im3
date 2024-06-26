module dev.declaration {
	requires static javax.annotation.jsr305;
	requires io.immutables.meta;
	// requires io.immutables.codec;
	//requires io.immutables.codec.jackson;

	//exports dev.declaration;
	exports dev.declaration.http;
	exports dev.declaration.module;
}
