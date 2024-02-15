package io.immutables.regres;

import io.immutables.common.Source;
import io.immutables.common.Vect;
import io.immutables.meta.Null;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

final class Exceptions {
	private Exceptions() {}

	static String methodLine(MethodProfile profile, ParameterProfile highlightedParameter) {
		@Null var method = profile.method();
		if (method == null) return "";

		var b = new StringBuilder("\n> ");
		//int prefixLength = b.length();
		b.append(method.getDeclaringClass().getSimpleName());
		b.append('.').append(method.getName()).append('(');
		int highlightParameterPosition = 0;// even first parameter position will always be > 0
		int highlightParameterLength = 0;
		int parameterIndex = 0;
		for (var p : profile.parameters()) {
			if (parameterIndex++ > 0) b.append(", ");

			var parameterString = p.spread()
				.map(prefix -> prefix + "*")
				.orElse(p.name());

			if (p == highlightedParameter) {
				// next param stating position will be at current length
				// -1 - not counting starting newline
				highlightParameterPosition = b.length() - 1;
				highlightParameterLength = parameterString.length();
			}

			b.append(parameterString);
		}
		b.append(')');

		if (highlightParameterPosition > 0) {
			b.append('\n');
			for (int i = 0; i < highlightParameterPosition; i++) b.append(' ');
			for (int i = 0; i < highlightParameterLength; i++) b.append('^');
		}
		return b.toString();
	}

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
	static Exception refineException(
		@Null SqlSource source,
		@Null Method method,
		MethodSnippet definition,
		Exception originalException) {

		if (source == null
			|| !postgresDriverPresent
			|| !(originalException instanceof PSQLException)) {

			if (method != null && declaredInThrows(method, originalException.getClass())) {
				return originalException;
			}

			var refined = originalException instanceof RuntimeException
				? originalException
				: new SqlException(originalException.getMessage(), originalException);

			if (method != null) {
				refined.setStackTrace(trimStackTrace(refined.getStackTrace(), method));
			}
			return refined;
		}

		try {
			var postgresException = ((PSQLException) originalException);
			@Null var detailedServerError = postgresException.getServerErrorMessage();
			if (detailedServerError == null) return postgresException;
			// correction for 1-based position, which can be 0 if unknown
			int position = Math.max(0, detailedServerError.getPosition() - 1);

			// need to adjust to position of the beginning of the snippet
			// in a larger source file which might have many snippets per method
			var mainMessage = Objects.toString(detailedServerError.getMessage(), "");

			var range = bestGuessedRange(definition, source, mainMessage, position);

			var state = Objects.toString(detailedServerError.getSQLState(), "");
			var stateName = postgresStatesCodeToName.getOrDefault(state, "");
			var additionalHint = "%s %s %s".formatted(detailedServerError.getSeverity(), state,
				stateName);

			// append problem listing to the original exception message
			var newMessage = ("\n" + source.problemAt(range, mainMessage, additionalHint))
				.stripTrailing();// remove trailing newline, it will be unnecessary (extra)

			if (method != null && declaredInThrows(method, SQLException.class)) {
				// if declared to throw SQL exception we're reconstructing
				// refined exception from the refined parts of the original
				var refined = new SQLException(
					newMessage, state, postgresException.getErrorCode());
				// retain any next refined whatever it might be
				@Null SQLException nextException = postgresException.getNextException();
				if (nextException != null) {
					refined.setNextException(nextException);
				}
				refined.setStackTrace(trimStackTrace(refined.getStackTrace(), method));
				return refined;
			} else {
				var refined = new SqlException(newMessage, postgresException);
				if (method != null) {
					refined.setStackTrace(trimStackTrace(refined.getStackTrace(), method));
				}
				return refined;
			}
		} catch (RuntimeException unexpectedExceptionDuringExceptionRefining) {
			// because of heuristics here, we might miss some aspects of
			// postgres exception and might receive some exception if our
			// assumptions fail, so we don't want to lose actual exception
			// and will add this secondary exception as suppressed,
			// so we see it and can fix
			originalException.addSuppressed(unexpectedExceptionDuringExceptionRefining);
			return originalException;
		}
	}

	private static boolean declaredInThrows(Method method, Class<?> exceptionType) {
		return Arrays.stream(method.getExceptionTypes())
			.anyMatch(thrown -> thrown.isAssignableFrom(exceptionType));
	}

	private static Source.Range bestGuessedRange(
		MethodSnippet definition,
		SqlSource source,
		String mainMessage,
		int originalPosition) {

		int excerptLength = 1;
		int statementOffset = definition.statementsRange().begin.position;
		int statementEnd = definition.statementsRange().end.position;
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

			// Check if we have something quoted and out position matches it
			if (matchesQuotedAt(content, statementOffset + originalPosition, quotedExcerpt)) {
				matched = true;
				position = statementOffset + originalPosition;
			} else if (originalPosition == 0) {
				// if we haven't matched at position which is 0
				// we assume that position is missing, and just try to find first
				// occurrence of our quoted excerpt containing offending characters
				// this is not exact match by any means and can give wrong highlight
				var wholeStatementsFragment = definition.statementsRange().get(content).toString();
				var at = wholeStatementsFragment.indexOf(quotedExcerpt);
				if (at >= 0) {
					position = statementOffset + at;
					matched = true;
				}
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

	private static boolean matchesQuotedAt(
		CharSequence content, int position, String quotedExcerpt) {
		if (position >= content.length()) return false;
		int end = Math.min(content.length(), position + quotedExcerpt.length());
		var chunk = content.subSequence(position, end).toString();
		return chunk.equals(quotedExcerpt);
	}

	static StackTraceElement[] trimStackTrace(StackTraceElement[] originalStack, Method method) {
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
