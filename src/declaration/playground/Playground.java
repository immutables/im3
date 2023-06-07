package io.immutables.declaration.playground;

import io.immutables.declaration.http.ClientException.ImATeaPot;
import io.immutables.declaration.http.ClientException.NotFound;
import io.immutables.declaration.http.GET;
import io.immutables.declaration.http.POST;
import io.immutables.declaration.http.Path;

@Path("/x")
public interface Playground {
	/**
	 * Documentation on insert
	 * is quite good one
	 * per se
	 * @param value on param value
	 */
	@POST
	void insert(String value);

	/**
	 * Another example
	 * @param x on param x
	 */
	@GET("?abc={x}&y")
	Lass get(String x, String y) throws NotFound, ImATeaPot;
}
