package io.immutables.regres;

import io.immutables.common.Source;
import io.immutables.common.Vect;
import io.immutables.meta.Null;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

final class Errors {
	private Errors() {}

	// During static initialization we determine if postgres driver
	// is present on the classpath. After that we only use postgres-specific
	// types only inside methods (never in signatures, or our whole class will
	// fail to load rendering our check useless) and only if this present flag is true
	private static final boolean postgresDriverPresent;
	private static final Map<String, String> postgresStatesCodeToName;

	static {
		boolean isPresent;
		Map<String, String> codeToName;
		try { // trigger classloading
			@SuppressWarnings("all")
			var n = PSQLException.class.getCanonicalName();
			isPresent = true;
			// when present, we also populate state map, doing this inline
			// to avoid introducing method with signature containing optionally loaded types
			var states = new HashMap<String, String>();
			for (var state : PSQLState.values()) {
				var code = state.getState();
				var name = state.name().replace("_", " ").toLowerCase();
				states.put(code, name);
			}
			codeToName = Map.copyOf(states);
		} catch (NoClassDefFoundError notPresentAtRuntime) {
			isPresent = false;
			codeToName = Map.of();
		}

		postgresStatesCodeToName = codeToName;
		postgresDriverPresent = isPresent;
	}

	static SQLException refineException(
			@Null Regresql.SqlSource source,
			Method method,
			Regresql.MethodSnippet definition,
			SQLException originalException) {

		if (source == null
				|| !postgresDriverPresent
				|| !(originalException instanceof PSQLException)) {
			return originalException;
		}

		var postgresException = ((PSQLException) originalException);
		@Null var detailedServerError = postgresException.getServerErrorMessage();
		if (detailedServerError == null) return originalException;
		// correction for 1-based position, which can be 0 if unknown
		int position = Math.max(0, detailedServerError.getPosition() - 1);

		// need to adjust to position of the beginning of the snippet
		// in a larger source file which might have many snippets per method
		var mainMessage = Objects.toString(detailedServerError.getMessage(), "");

		var range = bestGuessedRange(definition, source, mainMessage, position);

		var state = Objects.toString(detailedServerError.getSQLState(), "");
		var stateName = postgresStatesCodeToName.getOrDefault(state, "");
		var additionalHint = "%s %s %s".formatted(detailedServerError.getSeverity(), state, stateName);
		var problem = source.problemAt(range, mainMessage, additionalHint);
		// append problem listing to the original exception message
		var newMessage = originalException.getMessage() + "\n" + problem;
		var refinedException = new SQLException(
				newMessage, state, originalException.getErrorCode());

		// retain any next refinedException whatever it might be
		@Null SQLException nextException = originalException.getNextException();
		if (nextException != null) {
			refinedException.setNextException(nextException);
		}

		refinedException.setStackTrace(trimStackTrace(refinedException.getStackTrace(), method));
		return refinedException;
	}

	private static Source.Range bestGuessedRange(
			Regresql.MethodSnippet definition,
			Regresql.SqlSource source, String mainMessage, int originalPosition) {
		int excerptLength = 1;
		int statementOffset = definition.statementsRange().begin.position;

		int position = statementOffset + originalPosition;
		// Improving range for known issue with multiple statements (where the position
		// is only given from the offset of the last statement
		var quotedMatcher = QUOTED.matcher(mainMessage);
		// if we matched the quouted region so pretty sure about the range
		boolean matched = false;
		if (quotedMatcher.find()) {
			String quotedExcerpt = quotedMatcher.group(1);
			excerptLength = quotedExcerpt.length();
			var content = source.content();
			int length = content.length();
			// Check if we have something quoted and out position matches it
			if (matchesQuotedAt(content, position, length, quotedExcerpt)) {
				matched = true;
			} else {
				// we're not matching our fragment, try to walk potential statement terminators
				// from which we try match our statement relative position coming from server
				// and here we try to match after semicolon+newline
				var methodContent = definition.statementsRange().get(content);
				var statementTerminatorMatcher = MAYBE_STATEMENT_TERMINATOR.matcher(methodContent);
				while (statementTerminatorMatcher.find()) {
					// this -1 empirically works, not sure why exactly
					int nextStatementOffset = statementTerminatorMatcher.end() - 1;
					if (matchesQuotedAt(methodContent,
							nextStatementOffset + originalPosition,
							methodContent.length(),
							quotedExcerpt)) {
						// Consider we've found our match
						position = statementOffset + nextStatementOffset + originalPosition;
						matched = true;
						break;
					}
				}
			}
		}

		return matched && excerptLength > 1
				? Source.Range.of(source.get(position), source.get(position + excerptLength))
				: Source.Range.of(source.get(position));
	}

	private static boolean matchesQuotedAt(CharSequence content, int position, int length, String quotedExcerpt) {
		return content.subSequence(position, Math.min(length, position + quotedExcerpt.length()))
				.toString().equals(quotedExcerpt);
	}

	private static StackTraceElement[] trimStackTrace(StackTraceElement[] originalStack, Method method) {
		return Vect.of(originalStack)
				.dropWhile(s -> !s.getClassName().contains(".$Proxy"))
				.rangeFrom(1)
				.prepend(new StackTraceElement(
						method.getDeclaringClass().getName(),
						method.getName(),
						"Dynamic Proxy", -1))
				.toArray(StackTraceElement[]::new);
	}

	private static final Pattern QUOTED = Pattern.compile("\"([^\"]+)\"");
	// we're not sure if this can be part of some crazy multiline string or something
	private static final Pattern MAYBE_STATEMENT_TERMINATOR = Pattern.compile(";\\n");
}
