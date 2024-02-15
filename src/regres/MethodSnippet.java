package io.immutables.regres;

import io.immutables.common.Source;
import io.immutables.meta.Null;
import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNull;

record MethodSnippet(
	String name,
	SqlSource source,
	List<Placeholder> placeholders,
	Source.Range identifierRange,
	Source.Range statementsRange,
	String statements
) {
	record Placeholder(String name, Source.Range range) {}

	static class Builder {
		@Null String name;
		@Null SqlSource source;
		List<Placeholder> placeholders = new ArrayList<>();
		@Null Source.Range identifierRange;
		@Null Source.Range statementsRange;
		@Null String statements;

		MethodSnippet build() {
			return new MethodSnippet(
				requireNonNull(name),
				requireNonNull(source),
				List.copyOf(placeholders),
				requireNonNull(identifierRange),
				requireNonNull(statementsRange),
				requireNonNull(statements)
			);
		}
	}
}
