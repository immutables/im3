package io.immutables.stencil.template;

import io.immutables.stencil.Stencil;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static io.immutables.stencil.Literals.string;
import static io.immutables.stencil.template.Templating.*;

class Emitter extends Stencil.Raw {
	private final ProcessingStencil processing = new ProcessingStencil();
	private final AtomicInteger idCounter = new AtomicInteger();

	void generateStub(String packageName, String generatorName, List<?> problems) {
		processing.inPackage(packageName).name(generatorName, "_generator")
			.java((p, classname) -> {
				put("package ", packageName, ";").ln().ln();

				put("public final class ", classname, " extends ", generatorName, " {").ln();
				put("/*");
				for (var problem : problems) {
					put(dasheshAllTheWay).ln();
					put(p);
				}
				put("*/");
				put("}").ln();
			});
	}

	void generate(
		String packageName,
		String generatorName,
		LocalScope generatorScope,
		List<Element> elements) {

		processing.inPackage(packageName).name(generatorName, "_generator")
			.java((p, classname) -> {
				put("package ", packageName, ";").ln().ln();

				for (var e : elements) {
					if (e instanceof Import im) {
						for (var importLine : im.content().toString().split("\\n")) {
							String trimmedLine = importLine.trim();
							if (!trimmedLine.isEmpty()) {
								put("import ", trimmedLine, ";").ln();
							}
						}
					}
				}
				put("import static io.immutables.stencil.TemplateSupport.*;").ln();
				ln();
				put("public final class ", classname, " extends ", generatorName, " {").ln();
				put("protected final io.immutables.stencil.TemplateSupport __=",
					"new io.immutables.stencil.TemplateSupport();").ln();

				for (var e : elements) {
					if (e instanceof NonTemplate nt) {
						//java.comments(nt.content().toString());
					}

					if (e instanceof Definition df) {
						definition(generatorScope, df);
					}
				}

				put("}").ln();
			});
	}

	private void definition(LocalScope generatorScope, Definition df) {
		var scope = generatorScope.extend(df.identifier());

		for (var local : df.parameterNames()) {
			scope.declare(local);
		}
		put("//", dasheshAllTheWay).ln();
		put("public void ", df.signature(), " {__.dl();").ln();
		render(scope, df.content());
		put("__.dl();}//", df.identifier()).ln();
	}

	private void render(LocalScope scope, List<Content> contents) {
		out().indents++;
		for (var content : contents) {
			if (content instanceof Newline) {
				put("__.ln();").ln();
			} else if (content instanceof Fragment f) {
				put("__.$(", string(f.content()), ");");
			} else if (content instanceof TrimWhitespace w) {
				switch (w.direction()) {
					case After -> put("__.ta();");
					case Before -> put("__.tb();");
				}
			} else if (content instanceof Expression e) {
				put("__.$(()->", e.expression().expand(scope), ");");
			} else if (content instanceof BlockExpression b) {
				renderBlock(scope, b);
			} else if (content instanceof If i) {
				renderIf(scope, i);
			} else if (content instanceof CompactIf c) {
				renderCompactIf(scope, c);
			} else if (content instanceof Let l) {
				renderLet(scope, l);
			} else if (content instanceof For f) {
				renderFor(scope, f);
			} else if (content instanceof Spread s) {
				renderSpread(scope, s);
			} else if (content instanceof Case c) {
				renderCase(scope, c);
			}
		}
		out().indents--;
	}

	private void renderCase(LocalScope scope, Case c) {
		ifln();
		put("switch(", c.expression().expand(scope), "){").ln();
		for (var either : c.either()) {
			// we're getting ready for non-preview pattern matching in 'switch case',
			// so we're allocating new scope which can contribute variables
			// this would not make it automatically support variables,
			// expression will have to parse those out,
			// but we now have a fresh scope for it.
			var caseScope = scope.extend(":case");
			put("case ", either.expression().expand(caseScope), "->{__.dl();").ln();
			render(caseScope, either.content());
			put("__.dl();}").ln();
		}
		c.otherwise().ifPresent(otherwise -> {
			put("default -> {__.dl();").ln();
			// no special scope, default always use outer scope
			// (cannot introduce variables: no guarding expression, only content
			render(scope, otherwise.content());
			put("__.dl();}").ln();
		});
		put("}//case").ln();
	}

	private void renderSpread(LocalScope scope, Spread s) {
		var spreadElementId = newIdentifier("e");
		var iterationCounterId = newIdentifier("i");
		var iterationNumberFinal = newIdentifier("f");
		var spreadScope = scope.extend("...");
		spreadScope.substitutions.put("#", iterationNumberFinal);
		ifln();
		// ok not to use spreadScope in spread expression? probably this is correct
		// do not declare in scope use it directly, no expression will contain it
		put("{int ", iterationCounterId, "=0;for(final var ", spreadElementId, ":$for(",
			s.expression().expand(scope), ")){",
			"final int ", iterationNumberFinal, "=", iterationCounterId, "++;").ln();
		put("if(", iterationNumberFinal, ">0){__.dl();").ln();
		ifln();
		render(spreadScope, s.content());
		put("__.dl();}").ln();
		put("__.$(", spreadElementId, ");}}//...").ln();
	}

	private void renderFor(LocalScope scope, For f) {
		ifln();
		int bracesToClose = 0;

		var iterationCounterId = newIdentifier("i");
		var iterationNumberFinal = newIdentifier("fi");
		var forScope = scope.extend(":for");
		forScope.substitutions.put("#", iterationNumberFinal);
		put("{int ", iterationCounterId, "=0;");
		bracesToClose++;
		for (var clause : f.clauses()) {
			if (clause instanceof For.Each each) {
				put("for(final var ", each.identifier(), ":$for(",
					each.expression().expand(forScope), ")){");
				scope.declare(each.identifier());
				bracesToClose++;

			} else if (clause instanceof For.Predicate predicate) {
				put("if(");
				ifCondition(predicate.expression(), forScope);
				put("){");
				bracesToClose++;

			} else if (clause instanceof For.Assign assign) {
				put("final var ", assign.identifier(), "=",
					assign.expression().expand(forScope), ";");
				scope.declare(assign.identifier());
			}
		}
		put("__.dl();final int ", iterationNumberFinal, "=", iterationCounterId, "++;");
		ln();
		render(forScope, f.content());
		ifln();
		put("__.dl();", "}".repeat(bracesToClose), "//for").ln();
	}

	private void renderLet(LocalScope scope, Let l) {
		ifln();
		put("final var ", l.identifier(), "=__.let(()->{").ln();
		render(scope.extend(l.identifier()), l.content());
		ifln();
		put("});//let").ln();
		scope.declare(l.identifier());
	}

	private void renderCompactIf(LocalScope scope, CompactIf c) {
		ifln();
		put("if(");
		var ifScope = scope.extend(":?");
		ifCondition(c.then().condition(), ifScope);
		put("){__.dl();").ln();
		render(ifScope, c.then().content());
		ifln();
		put("__.dl();}");
		c.otherwise().ifPresent(e -> {
			put("else{__.dl();").ln();
			// regular scope in if
			render(scope, e.content());
			ifln();
			put("__.dl();}//if?").ln();
		});
	}

	private void renderIf(LocalScope scope, If i) {
		ifln();
		boolean first = true;
		for (var then : i.then()) {
			LocalScope ifScope;
			if (first) {
				first = false;
				put("if(");
				ifScope = scope.extend(":if");
			} else {
				ifln();
				put("}else if(");
				ifScope = scope.extend(":else:if");
			}
			ifCondition(then.condition(), ifScope);

			put("){__.dl();").ln();
			render(ifScope, then.content());
		}
		ifln();
		// actually scope, not ifScope, no new vars in else from if/ifelse
		i.otherwise().ifPresent(e -> render(scope, e.content()));
		ifln();
		put("__.dl();}//if").ln();
	}

	private void ifCondition(ExpressionContent condition, LocalScope scope) {
		int localsCount = scope.locals.size();
		var expression = condition.expand(scope);
		boolean useNakedIf = scope.locals.size() > localsCount;
		if (useNakedIf) put(expression);
		else put("$if(", expression, ")");
	}

	private void renderBlock(LocalScope scope, BlockExpression b) {
		ifln();
		put("{final var __block__=__.bl(()->{").ln();
		render(scope.extend(b.expression().toString()), b.content());
		ifln();
		LocalScope scope1 = scope.extendLocal("__block__");
		put("});__.$(()->", b.expression().expand(scope1), ");}").ln();
	}

	private String newIdentifier(String base) {
		return "__" + base + idCounter.getAndIncrement();
	}

	public static final String dasheshAllTheWay = "-" .repeat(80);
}
