package io.immutables.declaration.playground;

import io.immutables.declaration.http.GET;
import io.immutables.declaration.http.ClientException.NotFound;
import io.immutables.declaration.http.POST;
import io.immutables.declaration.http.Path;

@Path("X")
public interface Playground {
	@POST
	void insert(String value);

	@GET
	Lass get() throws NotFound;
}
