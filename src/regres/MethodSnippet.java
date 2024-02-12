package io.immutables.regres;

import io.immutables.common.Source;
import io.immutables.meta.Null;
import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNull;

record MethodSnippet(
	String name,
	List<String> placeholders,
	Source.Range identifierRange,
	Source.Range statementsRange,
	String statements
) {
	static class Builder {
		@Null String name;
		List<String> placeholders = new ArrayList<>();
		@Null Source.Range identifierRange;
		@Null Source.Range statementsRange;
		@Null String statements;

		MethodSnippet build() {
			return new MethodSnippet(
				requireNonNull(name),
				List.copyOf(placeholders),
				requireNonNull(identifierRange),
				requireNonNull(statementsRange),
				requireNonNull(statements)
			);
		}
	}
}
