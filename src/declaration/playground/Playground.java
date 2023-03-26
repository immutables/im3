package io.immutables.declaration.playground;

import io.immutables.declaration.http.GET;
import io.immutables.declaration.http.NotFound;
import io.immutables.declaration.http.POST;
import io.immutables.declaration.http.Path;

//@Path("X")
public interface Playground {
	@POST
	void insert(String value);

	@GET
	String get() throws NotFound;
}
