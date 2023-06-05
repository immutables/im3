package io.immutables.declaration.playground;

import io.immutables.meta.Null;
import java.util.List;
import java.util.OptionalInt;

public record Lass(boolean jk, String asc,
	OptionalInt about,
	List<String> string,
	@Null Integer integer) {
}
