package io.immutables.stencil.template;

import io.immutables.meta.Null;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.SourceVersion;

class ExpressionContent {
	private final List<String> literals;
	private final StringBuilder expression;
	private final List<String> locals = new ArrayList<>();

	ExpressionContent(CharSequence expression, List<String> literals) {
		this.literals = literals;
		this.expression = extractLiterals(expression);
	}

	static ExpressionContent binding(CharSequence expression, List<String> literals) {
		var e = new ExpressionContent(expression, literals);
		var m = bindingVariable.matcher(expression);
		if (m.find()) {
			e.locals.add(m.group(1));
		}
		return e;
	}

	private String replaceLiteral(CharSequence literal) {
		int index = literals.size();
		literals.add(literal.toString());
		return "@@" + index + "@@"; // should match regex: literalReference
	}

	private StringBuilder extractLiterals(CharSequence expression) {
		var b = new StringBuilder(expression);
		for (int i = 0; i < b.length(); i++) {
			char c = b.charAt(i);
			if (c == '\'' || c == '"') {
				i = extractTillMatching(b, i, c);
			}
		}
		return b;
	}

	private int extractTillMatching(StringBuilder b, int begin, char quote) {
		int end = begin;
		for (int j = begin + 1; j < b.length(); j++) {
			char c = b.charAt(j);
			if (c == quote) {
				// obviously, exclusive end is 1 char past our closing quote char
				end = j + 1;
				break;
			}
			// simple escape handling, we don't care if it's just \' or a unicode escape,
			// incrementing by one (skipping next 1 char) just before iteration's own increment
			if (c == '\\') j++;
		}
		// nothing matched, broken literal, probably, just return where we started
		if (end == begin) return begin;

		// perform literal externalization to save it from our accessor regexes
		var replacement = replaceLiteral(b.subSequence(begin, end));
		b.replace(begin, end, replacement);
		return begin + replacement.length();
	}

	StringBuilder expand(LocalScope scope) {
		// order matters - so there is no bad mix-ups
		for (var v : locals) scope.declare(v);
		var expansion = expandCasts(scope);
		expansion = expandAccessors(expansion, scope::hasLocal);
		expansion = expandLiterals(expansion, literals);
		expansion = expandIterations(expansion, scope);
		return expansion;
	}

	private StringBuilder expandCasts(LocalScope scope) {
		var expanded = new StringBuilder();
		Matcher m = renamingCast.matcher(expression);
		while (m.find()) {
			var typeName = m.group(1);
			var javaReplacement = " instanceof " + typeName;
			@Null var variableName = m.group(3);
			if (variableName != null) {
				// we don't check here if local is a keyword, let java stumble on it
				var n = variableName.trim();
				javaReplacement += " " + n;
				// side effect to declare variable in a current scope
				// so those will be properly recognised in this and below scopes
				scope.declare(n);
			}
			m.appendReplacement(expanded, javaReplacement);
		}
		m.appendTail(expanded);
		return expanded;
	}

	private static StringBuilder expandLiterals(
			CharSequence expression, List<String> literals
	) {
		var expanded = new StringBuilder();
		Matcher m = literalReference.matcher(expression);
		while (m.find()) {
			int index = Integer.parseInt(m.group(1));
			m.appendReplacement(expanded, literals.get(index));
		}
		m.appendTail(expanded);
		return expanded;
	}

	private static StringBuilder expandIterations(
		CharSequence expression, LocalScope scope
	) {
		var expanded = new StringBuilder();
		Matcher m = iterationCounters.matcher(expression);
		while (m.find()) {
			var hashes = m.group(); // whole match, all hashes
			// if one # - nearest scope(for) containing # mapping,
			// if two ## - next scope (outer for) up the chain
			// ### and so on
			int skipLevels = hashes.length() - 1;
			@Null var variable = scope.substitutionSkipping("#", skipLevels);
			// if not found - just leave same hashes
			m.appendReplacement(expanded, variable != null ? variable : hashes);
		}
		m.appendTail(expanded);
		return expanded;
	}

	private static StringBuilder expandAccessors(
		CharSequence expression, Predicate<String> isLocal
	) {
		var expanded = new StringBuilder();
		Matcher m = accessorPattern.matcher(expression);
		while (m.find()) {
			@Null var dotSelect = m.group(1);
			var accessor = m.group(2);
			@Null var existingParameters = m.group(3);
			if (existingParameters != null) {
				// already has parameters, keep verbatim
				m.appendReplacement(expanded, m.group());
			} else if (dotSelect == null
				&& (isLocal.test(accessor) || isKeyword(accessor))) {
				// is local (top) identifier, no need parameters, keep verbatim
				m.appendReplacement(expanded, m.group());
			} else {
				// property-like accessor, adding parens
				m.appendReplacement(expanded, m.group() + "()");
			}
		}
		m.appendTail(expanded);
		return expanded;
	}

	private static boolean isKeyword(String accessor) {
		return SourceVersion.isKeyword(accessor);
	}

	private static final Pattern accessorPattern = Pattern.compile(
		"(\\.\\s*)?(\\b[_a-z][_a-zA-Z0-9]*)(\\s*\\()?", Pattern.MULTILINE);

	// matches
	// @@42@@
	private static final Pattern literalReference = Pattern.compile("@@([0-9]+)@@");

	// matches
	// .(TypeName)
	// .(A.Bb.Cc val)
	// this is a second iteration of the syntax, somewhat inspired by Go type assertions
	private static final Pattern renamingCast = Pattern.compile(
		"\\.\\s*\\(\\s*([A-Z][_a-zA-Z0-9]*(\\s*\\.\\s*[A-Z][_a-zA-Z0-9]*)*)(\\s+[_a-zA-Z][_a-zA-Z0-9]*)?" +
			"\\s*\\)", Pattern.MULTILINE);

	// consecutive hashes #
	private static final Pattern iterationCounters = Pattern.compile(
		"(#)+");

	private static final Pattern bindingVariable = Pattern.compile(
		"\\s+(\\b[_a-z][_a-zA-Z0-9]*)\\s*$");

	@Override public String toString() {
		return expression.toString();
	}

	public static void main(String[] args) throws IOException {
		System.out.println(ExpressionContent.expandAccessors("""
			ff; hh
			aa.bb .cc(). ddd .
			ooo() *""", "aa"::equals));

		var bc = new ExpressionContent("""
			aa.(TypeOf.Int az)
			""", new ArrayList<>());

		var localScope = new LocalScope(null, "");
		localScope.declare("aa");

		System.out.println(bc.expand(localScope));
		System.out.println(localScope.hasLocal("az"));
	}
}
