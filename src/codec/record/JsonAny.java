package io.immutables.codec.record;

import io.immutables.codec.record.meta.Inline;
import io.immutables.meta.Null;
import java.util.List;
import java.util.Map;

public sealed interface JsonAny {
	@Inline
	record Array(List<JsonAny> elements) implements JsonAny {}

	@Inline
	record Struct(Map<String, JsonAny> fields) implements JsonAny {}

	@Inline
	record JsonString(String value) implements JsonAny {}

	@Inline
	record JsonBoolean(boolean value) implements JsonAny {}

	@Inline
	record JsonNumber(double number) implements JsonAny {}

	@Inline
	record JsonNull(@Null Void n) implements JsonAny {}
}
