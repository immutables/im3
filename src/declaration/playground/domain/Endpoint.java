package dev.declaration.playground.domain;

import dev.declaration.http.GET;
import dev.declaration.http.POST;
import dev.declaration.http.Path;

@Path("/end")
public interface Endpoint {
	@GET void get();

	@GET("/rec") Rec rec();

	@POST void post(String content);
}
