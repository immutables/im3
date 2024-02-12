package io.immutables.regres;

import java.util.regex.Pattern;

interface IdentifierPatterns {
	Pattern param = Pattern.compile("[a-zA-Z0-9_]+");
	String DESCRIBE_PARAM =
		"one or more uppercase or lowercase characters, digits, or underscores";

	Pattern plain = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
	String DESCRIBE_PLAIN =
		"one or more uppercase or lowercase characters, may have underscores and digits, "
			+ "but not first digit";

	Pattern quoted = Pattern.compile("[0-9\\w_ -/]+");
	String DESCRIBE_QUOTED =
		"one or more alphanumeric characters and can have space, underscore, "
			+ "dash or forward slash as separator";

	Pattern PLACEHOLDER_OR_COERCION = Pattern.compile(":{1,2}([a-zA-Z0-9_.]+)");
}
