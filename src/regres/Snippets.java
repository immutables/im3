package io.immutables.regres;

import io.immutables.common.Source;
import io.immutables.meta.Null;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class Snippets {
	private Snippets() {}

	static String resourceFilenameFor(Class<?> accessorInterface) {
		var canonicalName = accessorInterface.getCanonicalName();
		assert canonicalName != null : "precondition checked before";
		// not sure if null package can be for unnamed package, handling just in case
		var packageObject = accessorInterface.getPackage();
		var packageName = packageObject != null ? packageObject.getName() : "";
		var packagePath = packageName.replace('.', '/');
		String resourceFilename;
		if (canonicalName.startsWith(packageName + ".")) {
			resourceFilename =
				packagePath + "/" + canonicalName.substring(packageName.length() + 1);
		} else { // may include case for unnamed packages etc
			resourceFilename = canonicalName;
		}
		return "/" + resourceFilename + ".sql";
	}

	static Map<String, List<MethodSnippet>> parse(CharSequence content, Source.Lines lines) {
		var allMethods = new HashMap<String, List<MethodSnippet>>();

		class Parser {
			@Null MethodSnippet.Builder openBuilder;
			@Null Source.Range openRange;

			void parse() {
				for (int i = 1; i <= lines.count(); i++) {
					var range = lines.getLineRange(i);
					var line = range.get(content);
					var name = methodName(line);

					if (!name.isEmpty()) {
						// method identifier line
						// flush any open method and start
						// new method builder
						flushMethod(content, range);
						openMethod(name, range);
					} else {
						// regular statement line
						if (openBuilder != null) {
							// begin or expand range for open method
							openRange = openRange == null ? range : openRange.span(range);
						} else {
							// can collect unnamed leading lines for error reporting
							openMethod("", range);
						}
					}
				}

				var initialEmptyRange = Source.Range.of(Source.Position.of(0, 1, 1));
				flushMethod(content, initialEmptyRange);
			}

			String methodName(CharSequence line) {
				if (line.length() > 3
					&& line.charAt(0) == '-'
					&& line.charAt(1) == '-'
					&& line.charAt(2) == '.') {
					// can return empty string which is no method declared on this line
					// threat it as just an SQL comment. Or can be illegal name
					// anyway we expect these to be matched by the data access interface
					// method names and any discrepancy returned as errors
					return line.subSequence(3, line.length()).toString().trim();
				}
				return ""; // none
			}

			void openMethod(String name, Source.Range range) {
				openRange = null;
				openBuilder = new MethodSnippet.Builder();
				openBuilder.identifierRange = range;
				openBuilder.name = name;
			}

			void flushMethod(CharSequence content, Source.Range currentRange) {
				if (openBuilder != null) {
					if (openRange != null) prepareRange(content);
					else prepareEmpty(currentRange);

					allMethods.computeIfAbsent(openBuilder.name, k -> new ArrayList<>())
						.add(openBuilder.build());
				}
			}

			/**
			 * incomplete / empty method, defer error to runtime (like empty SQL statement)
			 * otherwise it would too painful during development
			 */
			void prepareEmpty(Source.Range currentRange) {
				assert openBuilder != null;
				openBuilder.statementsRange = Source.Range.of(currentRange.begin);
				openBuilder.statements = "--";
			}

			/**
			 * Parses source to extract placeholder list and also
			 * builds prepared statement where placeholders are substituted
			 * with '?' character to match the JDBC prepared statement syntax.
			 */
			void prepareRange(CharSequence content) {
				assert openBuilder != null;
				assert openRange != null;
				openBuilder.statementsRange = openRange;

				var source = content.subSequence(
					openRange.begin.position,
					openRange.end.position);

				openBuilder.statements =
					extractStatements(source, openBuilder.placeholders::add);
			}
		}

		new Parser().parse();

		return Map.copyOf(allMethods);
	}

	static String extractStatements(
		CharSequence source,
		Consumer<String> placeholders) {

		var buffer = new StringBuilder();
		var matcher = IdentifierPatterns.PLACEHOLDER_OR_COERCION.matcher(source);
		while (matcher.find()) {
			if (source.charAt(matcher.start(0) + 1) == ':') {
				// first char is always ':' in this match, so
				// we look for the second char and see if we
				// ignore and append the same SQL verbatim, consider this
				// just type coercion expression starting with '::'.
				// Could be potentially "fixed" by just not matching
				// such sequences in the first place if we have better pattern...
				matcher.appendReplacement(buffer, "$0");
			} else {
				var placeholder = matcher.group(1);
				placeholders.accept(placeholder);
				matcher.appendReplacement(buffer, "?");
				// append number of spaces to match the length of original
				// placeholder so SQL syntax error reporting would operate
				// on the same source positions/offsets as template definitions.
				for (int i = 0; i < placeholder.length(); i++) {
					buffer.append(' ');
				}
			}
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}
}
