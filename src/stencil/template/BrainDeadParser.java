package io.immutables.stencil.template;

import io.immutables.common.Source;
import io.immutables.meta.Null;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import static java.util.Objects.requireNonNull;

/**
 * This is sketchy parsing, leaving many errors up to generated template
 * code to witness (Java syntax, type checking).
 * <ul>
 *   <li>Don't care about performance
 * 	 <li>Don't want to overuse use regexes (well, just a little bit)
 *   <li>Don't want to bring other (complex) tools
 *   <li>Want to write it in a day or so
 *   <li>(actually took 2 days and oh boi it's ugly, but what I want!)
 * </ul>
 */
class BrainDeadParser {
	private final char[] input;
	private int p = -1;

	List<Templating.Element> elements = new ArrayList<>();
	List<Source.Problem> problems = new ArrayList<>();
	List<String> literalsPool = new ArrayList<>();

	BrainDeadParser(char[] input) {
		this.input = input;
	}

	void tryParse(String filename) {
		try {
			parse();
		} catch (WrongSyntax e) {
			// only a single problem in a list for now
			problems.add(asProblem(filename, e));
		}
	}

	Source.Problem asProblem(String filename, WrongSyntax e) {
		var source = Source.wrap(input);
		var lines = Source.Lines.from(source);

		var begin = lines.get(e.begin);
		var end = e.end == 0 ? begin : lines.get(e.end);
		return new Source.Problem(filename, source, lines,
			Source.Range.of(begin, end), e.getMessage(), "");
	}

	void parse() {
		nonTemplate();
		char c;
		while ((c = next()) != END) {
			switch (c) {
				case '[' -> {
					flush();
					scanDeclaration();
					nonTemplate();
				}
				case '\\' -> escaping('[', ']');
				case ']' -> error("Unexpected closing bracket `]`, escape it with `\\]`");
				default -> accept(c);
			}
		}
		flush();
	}

	private void scanDeclaration() {
		char c = next();
		switch (c) {
			case END -> error("Premature end of file, construct not closed `]`");
			case ']' -> error("Empty construct not allowed, escape with `\\[\\]` if just text");
			default -> {
				if (prefix("--")) {
					int from = p;
					advanceUntilMatching(']');
					addComment(from, p);
				} else if (keyword("import") && whitespace()) {
					int from = p;
					advanceUntilMatching(']');
					addImport(from, p);
				} else if (definition()) {

				} else {
					error("Unrecognized declaration, template signature expected");
				}
			}
		}
	}

	private void addComment(int from, int to) {
		elements.add(new Templating.Comment(from, to, content(from, to)));
	}

	private boolean prefix(String prefix) {
		return p + prefix.length() + 1 < input.length
			&& String.valueOf(input, p, prefix.length()).equals(prefix);
	}

	private boolean definition() {
		int from = p;
		advanceUntilMatching(']');
		int savep = p;
		var signature = content(from, p);
		String sig = signature.toString();
		boolean endsWithParameters = sig.trim().endsWith(")");
		int opensParameters = sig.indexOf('(');
		if (endsWithParameters && opensParameters > 0) {
			var identifier = sig.substring(0, opensParameters).trim();
			if (!isIdentifier(identifier)) {
				int begin = from + signature.indexOf(identifier);
				int end = begin + identifier.length();
				error("Malformed template signature", begin, end);
			}
			p = opensParameters + from;
			scanner = new DefinitionScanner(from, p, identifier).signature();
			p = savep;
			scan();
			return true;
		}
		//p = from;
		return false;
	}

	private boolean isIdentifier(String identifier) {
		if (identifier.isEmpty()) return false;
		for (int i = 0; i < identifier.length(); i++) {
			char c = identifier.charAt(i);
			boolean ok = i == 0
				? Character.isJavaIdentifierStart(c)
				: Character.isJavaIdentifierPart(c);
			if (!ok) return false;
		}
		return true;
	}

	class DefinitionScanner extends TemplateScanner {
		private final int from;
		private final int to;
		private final String identifier;
		private final List<String> parameters = new ArrayList<>();
		private @Null StringBuilder signature;

		DefinitionScanner(int from, int to, String identifier) {
			this.from = from;
			this.to = to;
			this.identifier = identifier;
		}

		TemplateScanner signature() {
			do {
				advanceUntilMatching(')', ',');
				int i = reverseBeginOfIdentifierOrLeftmostWhitespace(p);
				char c = input[i];
				if (c != '(' && c != ',' && c != ')') {
					// this means we've found actual identifier,
					// it should be a valid Java syntax
					// c should be either the beginning of the identifier
					// or the leftmost whitespace between identifier and parameter type
					parameters.add(content(i, p).toString().trim());
				}
			} while (input[p] == ',');

			signature = content(from, p + 1);
			return this;
		}

		@Override void end(int from, int to, String tag) {
			if (tag.isEmpty() || tag.equals(identifier)) {
				elements.add(
					new Templating.Definition(this.from, to, identifier,
						requireNonNull(signature),
						List.copyOf(parameters), List.copyOf(content)));
				scanner = null;
			} else super.end(from, to, tag);
		}
	}

	private int endOfIdentifierOrRightmostWhitespace(int start, int end) {
		int i = start;
		for (; i < end; i++) {
			if (!Character.isWhitespace(input[i])) break;
		}
		if (i == end || !Character.isJavaIdentifierStart(input[i])) return -1;
		i++;
		for (; i < end; i++) {
			if (!Character.isJavaIdentifierPart(input[i])) break;
		}
		for (; i < end; i++) {
			if (!Character.isWhitespace(input[i])) break;
		}
		return i - 1;
	}

	private int reverseBeginOfIdentifierOrLeftmostWhitespace(int end) {
		int i = end - 1;
		for (; i >= 0; i--) {
			if (!Character.isWhitespace(input[i])) break;
		}
		boolean wasIdentifierChars = false;
		for (; i >= 0; i--) {
			if (!Character.isJavaIdentifierPart(input[i])) break;
			wasIdentifierChars = true;
		}
		if (wasIdentifierChars && Character.isJavaIdentifierStart(input[i + 1])) {
			for (; i >= 0; i--) {
				if (!Character.isWhitespace(input[i])) break;
			}
			return i + 1;
		}
		return end; // default which is mismatch / not found, staying on where we start
	}

	private @Null TemplateScanner scanner;

	void scan() {
		char c;
		assert scanner != null;
		scanner.fragmentBegin = p + 1;
		while ((c = next()) != END) {
			switch (c) {
				case '[' -> {
					assert scanner != null;
					scanner.flush();
					int from = p;
					advanceUntilMatching(']');
					int savep = p;
					assert scanner != null;
					scanner.tag(from + 1, p, content(from + 1, p));
					p = savep;
					if (scanner == null) return;
					scanner.fragmentBegin = p + 1;
				}
				case '\\' -> scanner.escaping('[', ']');
				case ']' -> error("Unexpected closing bracket `]`, escape it with `\\]`");
				default -> scanner.accept(c);
			}
		}
	}

	abstract class TemplateScanner {
		StringBuilder fragment = new StringBuilder();
		final List<Templating.Content> content = new ArrayList<>();
		private int fragmentBegin;

		public void acceptReturn(Templating.Content child) {
			content.add(child);
			scanner = this;
		}

		public void acceptReturn(Collection<? extends Templating.Content> children) {
			content.addAll(children);
			scanner = this;
		}

		void proceed(int from, int to, String tag) {
			error("Wrong nesting of tag `" + tag + "`", from, to);
		}

		void end(int from, int to, String tag) {
			error("Mismatched closing tag `" + tag + "`", from, to);
		}

		private void tag(int from, int to, StringBuilder content) {
			var tag = content.toString().trim();
			if (content.length() >= 2
				&& content.charAt(0) == '-'
				&& content.charAt(1) == '-') {
				acceptReturn(new Templating.Comment(from, to, content));
			} else if (tag.equals(">-")) {
				acceptReturn(
					new Templating.TrimWhitespace(from, to,
						Templating.TrimWhitespace.Direction.After));
			} else if (tag.equals("-<")) {
				acceptReturn(
					new Templating.TrimWhitespace(from, to,
						Templating.TrimWhitespace.Direction.Before));
			} else if (startsWithKeyword(tag, "if")) {
				int atIf = content.indexOf("if");
				assert atIf >= 0;
				p = atIf + "if" .length() + from;
				assert scanner != null;
				scanner = parseIf(scanner, from, to);
				p = to;
			} else if (startsWithKeyword(tag, "for")) {
				int atFor = content.indexOf("for");
				assert atFor >= 0;
				p = atFor + "for" .length() + from;
				assert scanner != null;
				scanner = parseFor(scanner, from, to);
				p = to;
			} else if (startsWithKeyword(tag, "let")) {
				int atLet = content.indexOf("let");
				assert atLet >= 0;
				p = atLet + "let" .length() + from;
				assert scanner != null;
				scanner = parseLet(scanner, from, to);
				p = to;
			} else if (startsWithKeyword(tag, "case")) {
				int atCase = content.indexOf("case");
				assert atCase >= 0;
				p = atCase + "case" .length() + from;
				assert scanner != null;
				scanner = parseCase(scanner, from, to, content(p, to));
				p = to;
			} else if (tag.equals("else") || startsWithKeyword(tag, "else")) {
				if (startsWithKeyword(tag.substring("else" .length()).trim(), "if")) {
					int atIf = content.indexOf("if");
					assert atIf > 0; // and even move because of the `else`
					p = atIf + "if" .length() + from;
					proceed(from, to, "else if");
					p = to;
				} else {
					int atElse = content.indexOf("else");
					assert atElse >= 0;
					p = atElse + "else" .length() + from;
					proceed(from, to, "else");
					p = to;
				}
			} else if (tag.equals("void")) {
				scanner = parseVoid(scanner, from, to);
			} else if (tag.equals(":")) {
				proceed(from, to, ":");
			} else if (tag.startsWith("/")) {
				String remaining = tag.substring(1);
				if (remaining.startsWith("/")) {
					for (char r : remaining.toCharArray()) {
						if (r != '/') error(
							"In shorthand closing tag syntax, there must be only slashes, not `"
								+ tag + "`", from, to);
					}
					assert this == scanner;
					for (int i = 0; i < tag.length(); i++) {
						scanner.end(from, to, "");
					}
				} else {
					// includes empty single empty shorthand too
					end(from, to, remaining);
				}
			} else if (tag.endsWith("->")) {
				proceed(from, to, tag);
			} else if (tag.endsWith("...")) {
				int lastDots = content.lastIndexOf("...") + from;
				if (lastDots <= from) error("Not well formed spread ... operation", from, to);
				assert scanner != null;
				scanner = parseSpread(scanner, from, to, content(from, lastDots));
			} else if (tag.endsWith("?")) {
				int lastQuestion = content.lastIndexOf("?") + from;
				if (lastQuestion <= from) error("Not well formed compact if", from, to);
				assert scanner != null;
				scanner = parseCompactIf(scanner, from, to, content(from, lastQuestion));
			} else {
				boolean parsedWithBlock = false;
				if (tag.endsWith(")")) {
					int lastClosingParen = content.lastIndexOf(")") + from;
					int r = reverseBeginOfIdentifierOrLeftmostWhitespace(lastClosingParen);
					if (r != lastClosingParen) {
						// underscore supported only as a last parameter
						if ("_" .equals(content(r, lastClosingParen).toString().trim())) {
							int atUnderscore = content.lastIndexOf("_");
							assert atUnderscore >= 0;
							content.replace(atUnderscore, atUnderscore + 1, "__block__");
							assert scanner != null;
							scanner = parseBlock(scanner, from, to, content);
							parsedWithBlock = true;
						}
					}
				} else if (tag.endsWith("_")) {
					int atUnderscore = content.lastIndexOf("_");
					assert atUnderscore >= 0;
					content.replace(atUnderscore, content.length(), "(__block__)");
					assert scanner != null;
					scanner = parseBlock(scanner, from, to, content);
					parsedWithBlock = true;
				}
				if (!parsedWithBlock) {
					acceptReturn(new Templating.Expression(from, to, expressionOf(content)));
				}
			}
		}

		void addVerbatim(Templating.Verbatim verbatim) {
			content.add(verbatim);
		}

		void accept(char c) {
			if (c == '\n') {
				flush();
				addVerbatim(new Templating.Newline(p, p + 1));
			} else {
				fragment.append(c);
			}
		}

		void flush() {
			if (!fragment.isEmpty()) {
				assert fragmentBegin > 0;
				addVerbatim(new Templating.Fragment(fragmentBegin, p, fragment));
				fragment = new StringBuilder();
			}
			fragmentBegin = p;
		}

		void escaping(char... chars) {
			if (p + 1 < input.length) {
				char next = input[p + 1];
				for (char c : chars) {
					if (next == c) {
						accept(next);
						p++;
					}
				}
			}
		}
	}

	private TemplateScanner parseVoid(TemplateScanner parent, int from, int to) {
		return new VoidScanner(parent, from, to);
	}

	private TemplateScanner parseCase(TemplateScanner parent, int from, int to,
		StringBuilder expression) {
		return new CaseScanner(parent, from, to, expression);
	}

	private TemplateScanner parseBlock(TemplateScanner parent, int from, int to,
		StringBuilder expression) {
		return new BlockScanner(parent, from, to, expression);
	}

	private TemplateScanner parseSpread(TemplateScanner parent, int from, int to,
		StringBuilder expression) {
		return new SpreadScanner(parent, from, to, expression);
	}

	private TemplateScanner parseCompactIf(
		TemplateScanner parent, int from, int to, StringBuilder condition) {
		return new CompactIfScanner(parent, from, to, condition);
	}

	private TemplateScanner parseIf(TemplateScanner parent, int from, int to) {
		return new IfScanner(parent, from, to).condition();
	}

	private TemplateScanner parseLet(TemplateScanner parent, int from, int to) {
		return new LetScanner(parent, from, to).signature();
	}

	private TemplateScanner parseFor(TemplateScanner parent, int from, int to) {
		return new ForScanner(parent, from, to).clauses();
	}

	class ForScanner extends TemplateScanner {
		private final TemplateScanner parent;
		private final int from;
		private final int to;
		private final List<Templating.For.Clause> clauses = new ArrayList();

		public ForScanner(TemplateScanner parent, int from, int to) {
			this.parent = parent;
			this.from = from;
			this.to = to;
		}

		TemplateScanner clauses() {
			do {
				int f = p;
				advanceUntilMatching(']', ',');
				int t = p;

				if (input[f] == ',') f++;

				var clause = content(f, t);

				int atIdentifier = endOfIdentifierOrRightmostWhitespace(f, t);
				if (atIdentifier < 0) error("Not a well-formed for clause", f, t);

				String identifier = content(f, atIdentifier + 1).toString().trim();
				if (!isIdentifier(identifier)) error("Not an identifier: " + clause);

				char operator = input[atIdentifier + 1];
				if (identifier.equals("if")) {
					clauses.add(new Templating.For.Predicate(
						expressionRange(atIdentifier, t)));
				} else if (operator == ':') {
					clauses.add(new Templating.For.Each(identifier,
						expressionRange(atIdentifier + 2, t)));
				} else if (operator == '=') {
					clauses.add(new Templating.For.Assign(identifier,
						expressionRange(atIdentifier + 2, t)));
				} else {
					error("Expected ':' or '=' delimiter in for clause", atIdentifier + 1,
						atIdentifier + 2);
				}
			} while (input[p] == ',');
			return this;
		}

		@Override void end(int from, int to, String tag) {
			if (tag.isEmpty() || tag.equals("for")) {
				parent.acceptReturn(new Templating.For(
					this.from, to, List.copyOf(clauses), List.copyOf(content)));
			} else super.end(from, to, tag);
		}
	}

	class SpreadScanner extends TemplateScanner {
		private final TemplateScanner parent;
		private final int from;
		private final int to;
		private final StringBuilder expression;

		SpreadScanner(TemplateScanner parent, int from, int to, StringBuilder expression) {
			this.parent = parent;
			this.from = from;
			this.to = to;
			this.expression = expression;
		}

		@Override void end(int from, int to, String tag) {
			if (tag.isEmpty()) {
				parent.acceptReturn(new Templating.Spread(
					this.from, to, expressionOf(expression), List.copyOf(content)));
			} else super.end(from, to, tag);
		}
	}

	class BlockScanner extends TemplateScanner {
		private final TemplateScanner parent;
		private final int from;
		private final int to;
		private final StringBuilder expression;

		BlockScanner(TemplateScanner parent, int from, int to, StringBuilder expression) {
			this.parent = parent;
			this.from = from;
			this.to = to;
			this.expression = expression;
		}

		@Override void end(int from, int to, String tag) {
			if (tag.isEmpty() || expression.toString().trim().startsWith(tag + "(")) {
				parent.acceptReturn(new Templating.BlockExpression(
					this.from, to, expressionOf(expression), List.copyOf(content)));
			} else super.end(from, to, tag);
		}
	}

	class LetScanner extends TemplateScanner {
		private final TemplateScanner parent;
		private final int from;
		private final int to;
		private @Null String letName;

		public LetScanner(TemplateScanner parent, int from, int to) {
			this.parent = parent;
			this.from = from;
			this.to = to;
		}

		TemplateScanner signature() {
			letName = content(p, to).toString().trim();
			if (!isIdentifier(letName)) error("Not an identifier for `let`", p, to);
			return this;
		}

		@Override void end(int from, int to, String tag) {
			if (tag.isEmpty() || tag.equals("let")) {
				parent.acceptReturn(new Templating.Let(
					this.from, to, requireNonNull(letName), List.copyOf(content)));
			} else {
				super.end(from, to, tag);
			}
		}
	}

	class CompactIfScanner extends TemplateScanner {
		private final TemplateScanner parent;
		private final int from;
		private final int to;
		private @Null Templating.If.Then then;
		private final StringBuilder condition;
		boolean wasElse;

		public CompactIfScanner(TemplateScanner parent, int from, int to,
			StringBuilder condition) {
			this.parent = parent;
			this.from = from;
			this.to = to;
			this.condition = condition;
		}

		@Override void proceed(int from, int to, String tag) {
			if (tag.equals(":")) {
				if (wasElse) error("Compact if? doesn't allow multiple else `:`", from, to);
				wasElse = true;
				cutThen();
			} else proceed(from, to, tag);
		}

		@Override void end(int from, int to, String tag) {
			if (tag.isEmpty()) {
				Templating.If.Compact ifelse;
				if (wasElse) {
					ifelse = new Templating.If.Compact(this.from, to,
						requireNonNull(then), Optional.of(
						new Templating.If.Else(List.copyOf(content))));
				} else {
					cutThen();
					ifelse = new Templating.If.Compact(this.from, to,
						requireNonNull(then), Optional.empty());
				}
				parent.acceptReturn(ifelse);
			} else super.end(from, to, tag);
		}

		private void cutThen() {
			then = new Templating.If.Then(expressionOf(condition), List.copyOf(content));
		}
	}

	class IfScanner extends TemplateScanner {
		private final TemplateScanner parent;
		private final int from;
		private final int to;

		private final List<Templating.If.Then> then = new ArrayList<>();
		private boolean wasElse = false;
		private @Null StringBuilder condition;

		public IfScanner(TemplateScanner parent, int from, int to) {
			this.parent = parent;
			this.from = from;
			this.to = to;
		}

		TemplateScanner condition() {
			int begin = p;
			advanceUntilMatching(']');
			condition = content(begin, p);
			return this;
		}

		@Override void proceed(int from, int to, String tag) {
			if (tag.equals("else")) {
				if (wasElse) error("`else` is not allowed after another `else`", from, to);
				wasElse = true;
				cutCurrentThen();
			} else if (tag.equals("else if")) {
				if (wasElse) error("`else if` comes after `else`", from, to);
				cutCurrentThen();
				condition();
			} else {
				super.proceed(from, to, tag);
			}
		}

		@Override void end(int from, int to, String tag) {
			if (tag.isEmpty() || tag.equals("if")) {
				Templating.If ifelse;
				if (wasElse) {
					ifelse = new Templating.If(this.from, to, List.copyOf(then),
						Optional.of(new Templating.If.Else(List.copyOf(content))));
				} else {
					cutCurrentThen();
					ifelse = new Templating.If(this.from, to, List.copyOf(then), Optional.empty());
				}
				parent.acceptReturn(ifelse);
			} else {
				super.end(from, to, tag);
			}
		}

		private void cutCurrentThen() {
			then.add(new Templating.If.Then(
				expressionOf(requireNonNull(condition)), List.copyOf(content)));
			condition = null;
			content.clear();
		}
	}

	class VoidScanner extends TemplateScanner {
		private final TemplateScanner parent;
		private final int from;
		private final int to;

		public VoidScanner(TemplateScanner parent, int from, int to) {
			this.parent = parent;
			this.from = from;
			this.to = to;
		}

		@Override void addVerbatim(Templating.Verbatim verbatim) {
			// black-holes (voids) verbatim content
		}

		@Override void end(int from, int to, String tag) {
			if (tag.equals("void") || tag.isEmpty()) {
				// we pass our collected content (minus any verbatim around)
				// but we "void" ourselves, ironically
				parent.acceptReturn(content);
			} else super.end(from, to, tag);
		}
	}

	private static final int NOPE = -1;

	class CaseScanner extends TemplateScanner {
		private final TemplateScanner parent;
		private final int from;
		private final int to;
		private final StringBuilder expression;
		private @Null String letName;

		private List<Templating.Case.Either> either = new ArrayList<>();
		private @Null Templating.Case.Else otherwise;
		private @Null StringBuilder eitherExpression;

		int eitherFrom = NOPE;
		int elseFrom = NOPE;

		public CaseScanner(TemplateScanner parent, int from, int to,
			StringBuilder expression) {
			this.parent = parent;
			this.from = from;
			this.to = to;
			this.expression = expression;
		}

		@Override void accept(char c) {
			super.accept(c);
		}

		@Override void addVerbatim(Templating.Verbatim verbatim) {
			if (eitherFrom == NOPE && elseFrom == NOPE) {
				if (verbatim instanceof Templating.Fragment f
					&& !f.content().toString().isBlank()) {
					error("""
						No (non-blank) template content allowed directly inside case, use '->'
						or 'else' tag: %s""".formatted(f.content()),
						verbatim.from(), verbatim.to());
				}
				// just ignore verbatim content
			} else super.addVerbatim(verbatim);
		}

		@Override void proceed(int from, int to, String tag) {
			if (tag.equals("else") && elseFrom == NOPE) {
				if (eitherFrom > NOPE) {
					endEither(from); // ends where we start here
				}
				elseFrom = from;
			} else if (tag.endsWith("->")) {
				if (elseFrom > NOPE) {
					error("Case -> tag cannot be after 'else', only before", from, to);
				}
				if (eitherFrom > NOPE) {
					endEither(from);
				}
				eitherFrom = from;
				// indexOf cannot fail if string ends with arrow here, in this `if`
				eitherExpression = new StringBuilder(tag.substring(0, tag.lastIndexOf("->")));
			} else super.proceed(from, to, tag);
		}

		@Override void end(int from, int to, String tag) {
			if (tag.equals("case")) { // closing regardless
				if (elseFrom > NOPE) {
					endElse(to);
				} else if (eitherFrom > NOPE) {
					endEither(to);
				}
				endCase(to);
			} else if (tag.equals("else") && elseFrom > NOPE) {
				endElse(to);
			} else if (tag.isEmpty()) {
				// here it's more complicated, anonymous closing tag can close any
				// of the subtags and case itself
				if (elseFrom > NOPE) {
					endElse(to);
				} else if (eitherFrom > NOPE) {
					endEither(to);
				} else {
					endCase(to);
				}
			} else super.end(from, to, tag);
		}

		private void endCase(int to) {
			parent.acceptReturn(new Templating.Case(this.from, to,
				expressionOf(expression),
				List.copyOf(either), Optional.ofNullable(otherwise)));
		}

		private void endEither(int to) {
			either.add(new Templating.Case.Either(
				eitherFrom, to,
				expressionOf(requireNonNull(eitherExpression)),
				List.copyOf(content)));
			eitherFrom = NOPE;
			content.clear();
		}

		private void endElse(int to) {
			otherwise = new Templating.Case.Else(
				elseFrom, to, List.copyOf(content));
			elseFrom = NOPE;
			content.clear();
		}
	}

	private boolean startsWithKeyword(String tag, String keyword) {
		return (tag.length() > keyword.length() + 1)
			&& tag.startsWith(keyword)
			&& !Character.isJavaIdentifierPart(tag.charAt(keyword.length()));
	}

	private ExpressionContent expressionRange(int from, int to) {
		return expressionOf(new StringBuilder().append(input, from, to - from));
	}

	private ExpressionContent expressionOf(StringBuilder chars) {
		return new ExpressionContent(chars, literalsPool);
	}

	private StringBuilder content(int from, int to) {
		return new StringBuilder().append(input, from, to - from);
	}

	private void addImport(int from, int to) {
		elements.add(new Templating.Import(from, to, content(from, to)));
	}

	private void advanceUntilMatching(char end) {
		advanceUntilMatching(end, END);
	}

	private void advanceUntilMatching(char end, char orEnd) {
		do {
			char c = next(end);
			if (c == end) return;
			if (c == orEnd) return;
			switch (c) {
				case '[' -> advanceUntilMatching(']');
				case '{' -> advanceUntilMatching('}');
				case '(' -> advanceUntilMatching(')');
				case '\'' -> advancePastLiteral('\'');
				case '\"' -> advancePastLiteral('\"');
			}
		} while (true);
	}

	private void advancePastLiteral(char quote) {
		do {
			char c = next(quote);
			if (c == quote) return;
			if (c == '\\') next();
		} while (true);
	}

	private char next(char expectedEnd) {
		char c = next();
		if (c == END) error("Premature end of file, construct not closed `" + expectedEnd + "`");
		return c;
	}

	private void escaping(char... chars) {
		if (p + 1 < input.length) {
			char next = input[p + 1];
			for (char c : chars) {
				if (next == c) {
					accept(next);
					p++;
				}
			}
		}
	}

	private boolean keyword(String keyword) {
		// +1 for a character following keyword, cannot be end
		if (p + keyword.length() + 1 < input.length) {
			int i = 0;
			for (; i < keyword.length(); i++) {
				if (keyword.charAt(i) != input[p + i]) return false;
			}
			char after = input[p + i];
			// keyword must be followed with some delimiter or whitespace, not alphanumeric
			if (Character.isJavaIdentifierPart(after)) return false;
			// move pointer if matches to before following char,
			// so next() will then advance properly
			p = p + i - 1;
			return true;
		}
		return false;
	}

	private boolean whitespace() {
		int next = p + 1;
		if (next < input.length) {
			if (Character.isWhitespace(input[next])) {
				p = next;
				return true;
			}
		}
		return false;
	}

	private void error(String message) {
		throw new WrongSyntax(message, p);
	}

	private void error(String message, int begin, int end) {
		throw new WrongSyntax(message, begin, end);
	}

	static class WrongSyntax extends RuntimeException {
		final int begin;
		final int end;

		WrongSyntax(String message, int begin) {
			this(message, begin, 0);
		}

		WrongSyntax(String message, int begin, int end) {
			super(message);
			this.begin = begin;
			this.end = end;
		}

		@Override public String toString() {
			return super.toString() + "; at " + begin;
		}
	}

	private char next() {
		return p + 1 < input.length ? input[++p] : END;
	}

	public static final char END = '\0';

	private void accept(char c) {
		requireNonNull(consumer).accept(c);
	}

	private @Null Consumer consumer;

	private void nonTemplate() {
		var content = new StringBuilder();
		consumer = new Consumer() {
			final int begin = p + 1;

			@Override public void accept(char c) {
				content.append(c);
			}

			@Override public void flush() {
				int end = p + 1;
				if (end != begin) {
					elements.add(new Templating.NonTemplate(begin, end, content));
				}
			}
		};
	}

	interface Consumer {
		void accept(char c);
		void flush();
	}

	private void flush() {
		if (consumer != null) consumer.flush();
	}

	public static void main(String[] args) throws IOException {
		String content;
		String filename = "Gen.generator";
		try (var is = BrainDeadParser.class.getResourceAsStream(filename);
			var br = new BufferedReader(new InputStreamReader(requireNonNull(is)))) {
			content = br.lines().collect(Collectors.joining("\n"));
		}

		BrainDeadParser parser = new BrainDeadParser(content.toCharArray());
		parser.parse();

		Source.Lines lines = Source.Lines.from(content);

		for (var e : parser.elements) {
			System.out.println(e.getClass().getSimpleName() + ":" + Source.Range.of(lines.get(e.from()),
				lines.get(e.to())));
			if (e instanceof Templating.NonTemplate nt) {
				System.out.println(nt.content());
			}
			if (e instanceof Templating.Comment comment) {
				System.out.println(comment.content());
			}
			if (e instanceof Templating.Definition def) {
				System.out.println(def.signature());
				System.out.println(def.content());
			}
			if (e instanceof Templating.Import def) {
				System.out.println(def.content());
			}
		}
	}
}
