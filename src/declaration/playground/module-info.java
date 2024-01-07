
// @processor dev.declaration.processor
// @option dev.declaration.processor.output=[project.dir]/.build/declaration/[module.name]
// @option dev.declaration.servers=dev>https://dev.usw2.svc.mastercontrol.engineering/pcs/publishing/v1
module dev.declaration.playground {
	requires static javax.annotation.jsr305;
	requires io.immutables.meta;
	requires dev.declaration;
	// requires io.immutables.codec;
	// requires io.immutables.codec.jackson;
	requires spring.web;
}
