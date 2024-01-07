package dev.declaration.processor;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Holds result of documentation comment extraction
 * @param lines element's own comment lines
 * @param returns method's return type comment lines
 * @param parameters method's parameter comment lines by name
 * @param thrown method's throws by type
 */
record DocComment(
	List<String> lines,
	List<String> returns,
	Map<String, List<String>> parameters,
	Map<String, List<String>> thrown
) {

	static DocComment extract(String content) {
		if (content.isBlank()) return Empty;

		var lines = new ArrayList<String>();
		var returns = new ArrayList<String>();
		// let's preserve order here
		var parameters = new LinkedHashMap<String, List<String>>();
		var thrown = new LinkedHashMap<String, List<String>>();

		var current = lines;
		boolean wasBlankLine = false;

		for (var line : content.stripTrailing().stripIndent().lines().toList()) {

			if (line.isBlank()) {
				current = lines;// always reset current for taglets

				if (!wasBlankLine) {
					lines.add(""); // normalize to an empty line
					wasBlankLine = true;
				}
				// skipping repeated blank lines
				// going to next line
				continue;
			}

			var m = TAGLET_RETURN.matcher(line);
			if (m.matches()) {
				current = returns;
				current.add(m.group(1));
				continue;
			}

			m = TAGLET_PARAM.matcher(line);
			if (m.matches()) {
				current = new ArrayList<>();
				parameters.put(m.group(1), current);
				current.add(m.group(2));
				continue;
			}

			m = TAGLET_THROWS.matcher(line);
			if (m.matches()) {
				current = new ArrayList<>();
				thrown.put(m.group(1), current);
				current.add(m.group(2));
				continue;
			}

			// because we first checked for blank (and empty line, we're sure
			// that line is not empty
			assert !line.isEmpty();

			if (current != lines && Character.isWhitespace(line.codePointAt(0))) {
				// Here we continue to add indented lines
				// to a preceding @param / @return / @throws
				current.add(line.strip());
			} else {
				// resetting and adding regular comment lines, not related to
				current = lines;
				current.add(line);
			}
		}

		return new DocComment(
			List.copyOf(lines),
			List.copyOf(returns),
			sealEntries(parameters),
			sealEntries(thrown));
	}

	private static Map<String, List<String>> sealEntries(
		LinkedHashMap<String, List<String>> map) {
		var copy = new LinkedHashMap<String, List<String>>(map.size());
		map.forEach((key, value) -> copy.put(key, List.copyOf(value)));
		return Collections.unmodifiableMap(copy);
	}

	static List<String> concat(List<String> a, List<String> b) {
		var appendTo = new ArrayList<>(a);
		appendTo.addAll(b);
		return List.copyOf(appendTo);
	}

	static final DocComment Empty = new DocComment(List.of(), List.of(), Map.of(), Map.of());

	private static final Pattern TAGLET_PARAM = Pattern.compile(
		"^\\s*@param\\s+([a-zA-Z0-9._]+)\\s+(.*)"); //<-- not exactly identifier, but it's ok

	private static final Pattern TAGLET_RETURN = Pattern.compile(
		"^\\s*@return\\s+(.*)");

	private static final Pattern TAGLET_THROWS = Pattern.compile(
		"^\\s*@throws\\s+([a-zA-Z0-9._]+)\\s+(.*)");
}
