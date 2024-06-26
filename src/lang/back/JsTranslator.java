package io.immutables.lang.back;

import io.immutables.lang.Unreachable;
import io.immutables.lang.node.Identifier;
import io.immutables.lang.node.Node;
import io.immutables.lang.node.Operators;
import io.immutables.lang.node.SourceSpan;
import io.immutables.lang.syntax.Unit;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

// TODO Get old version or manually revert to pattern matching
public class JsTranslator {
	private final String packageName;
	private final Output out;

	public JsTranslator(String packageName, Output output) {
		this.packageName = packageName;
		this.out = output;
	}

	void unit(Unit<Node.Unit> unit) {
		out.screenBackendKeyword = true;
		out.put("// ", unit.path()).ln();

		for (var e : unit.node().elements) {
      if (e instanceof Node.ValueBinding b) {
        valueBinding(b);
      } else if (e instanceof Node.TypeDeclaration t) {
        typeDeclaration(t);
      } else if (e instanceof Node.FeatureDeclaration f) {
        featureDeclaration("export function ", f);
      } else if (e instanceof Node.TagxDeclaration t) {
        tagxDeclaration(t);
      } else if (e instanceof Node.Comments comments) {
        comments(comments.comment);
      } else if (e instanceof Node.Blanks) {
        // skip blanks
      } else {
        out.put("/*? ", e, "*/");
      }
		}
	}

	private void prelude() {
		out.put("'use strict';");
		out.put("// package ", packageName).ln();

		out.put("import {h as $h, Fragment as $hf, render} from '/-/preact.js';").ln();
		out.put("import {useState, useMemo} from '/-/preact/hooks.js';").ln();
		out.put("import * as $s from '/js/support.rx.js';").ln();
	}

	private void comments(List<SourceSpan> comments) {
		for (var c : comments) {
			if (c.length > 2 && c.buffer[c.offset] == ':') {
				try {
					out.raw(c.buffer, c.offset + 1, c.length - 1);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				out.put(" //:").ln();
			} else {
				out.put("//", c).ln();
			}
		}
	}

	private void valueBinding(Node.ValueBinding b) {
		out.ln();
		comments(b.comment);
		out.put("const ", b.name, " = ");
		expression(F_VALUE, b.value);
		out.ln();
	}

	private void typeDeclaration(Node.TypeDeclaration t) {
		out.ln();
		out.put("function ", t.name, "() {").ln();
		out.indents++;
		// TODO revise that
		out.put("if(!this)return new ", t.name, "(...arguments);").ln();
		if (t.shape.isRecord()) {
			out.put("if(arguments.length!==1||arguments[0].constructor!==Object)throw new TypeError" +
				"('!record arguments');").ln();
		}
		out.put(t.name, ".$cons(this,...arguments)").ln();
		out.indents--;
		out.put("}").ln();

		out.put(t.name, ".$cast=function(a) {").ln();
		out.indents++;
		out.put("if(a.constructor!==", t.name, ")throw new TypeError('constructor!=", t.name, "');").ln();
		out.put("return a;").ln();
		out.indents--;
		out.put("}").ln();

		out.put(t.name, ".$cons=function($t");
		constructorParameters((Node.Parameters) t.shape);
		out.put(") {").ln();
		out.indents++;
		constructorBindings((Node.Parameters) t.shape);
		out.indents--;
		out.put("}").ln();

		if (t.impl != null) implBlock(t, t.impl);
	}

	private void implBlock(Node.NamedDeclaration decl, Node.ImplFeatures impl) {
		out.ln();
		out.put(decl.name, ".prototype = {").ln();
		out.indents++;
		for (var e : impl.elements) {
      if (e instanceof Node.FeatureDeclaration f) {
        comments(f.comment);
        if (f.input.isImplicitlyEmpty()) out.put("get ");
        out.put(f.name);
        featureDeclarationParametersAndBody(f);
        out.put(',').ln();
      } else if (e instanceof Node.Comments comments) {
        comments(comments.comment);
      } else if (e instanceof Node.Blanks) {
      } else {
        out.put("/*? ", e, "*/");
      }
		}
		out.indents--;
		out.put("};").ln();
	}

	private void constructorBindings(Node.Parameters parameters) {
		if (parameters instanceof Node.ParameterRecord r) {
			for (var g : r.fields) constructoBindNamedGroup(g);
		} else if (parameters instanceof Node.ParameterProduct p) {
			for (var c : p.components) {
				if (c instanceof Node.ParameterNamedGroup g)
					constructoBindNamedGroup(g);
			}
		}
	}

	private void constructoBindNamedGroup(Node.ParameterNamedGroup group) {
		for (var n : group.names) {
			out.put("$t.", n, "=", n).ln();
		}
	}

	private void constructorParameters(Node.Parameters parameters) {
		// this already within parameter list parens ( )
		if (parameters instanceof Node.ParameterRecord r) {
			out.put(",{");
			parameterGroups(r.fields);
			out.put("}");
		} else if (parameters instanceof Node.ParameterProduct p) {
			if (!p.components.isEmpty()) {
				out.put(",");
				parameterGroups(p.components);
			}
		}
	}

	private void featureParameters(Node.Parameters parameters) {
    if (parameters instanceof Node.ParameterRecord r) {
      out.put("({");
      parameterGroups(r.fields);
      out.put("})");
    } else if (parameters instanceof Node.ParameterProduct p) {
      out.put('(');
      parameterGroups(p.components);
      out.put(')');
    } else if (parameters instanceof Node.ParameterEmpty) {
      out.put("()");
    } else {
      out.put("(/*unsupported*/)");
    }
	}

	private void parameterGroups(List<? extends Node.ParameterProductComponent> components) {
		int i = 0;
		for (var c : components) {
      if (c instanceof Node.ParameterUnnamed) {
        out.putIf(i++ > 0, ",").put("_", i);
      } else if (c instanceof Node.ParameterNamedGroup g) {
        for (var n : g.names) {
          out.putIf(i++ > 0, ", ").put(n);
          if (g.defaultValue != null) {
            out.put("=");
            expression(F_VALUE, g.defaultValue);
          }
        }
      }
		}
	}

	private void featureDeclaration(String prefix, Node.FeatureDeclaration f) {
		out.ln();
		out.put(prefix, f.name);
		featureDeclarationParametersAndBody(f);
	}

	private void featureDeclarationParametersAndBody(Node.FeatureDeclaration f) {
		featureParameters(f.input);
		out.put(" {").ln();
		out.indents++;
		int F = F_ALLOW_RETURN | F_ALLOW_STATEMENTS | F_RETURN_LAST;
		if (f.statements != null) {
			for (var s : f.statements.statements) {
				statementsElement(F, s);
			}
		} else {
			out.put("throw new Error('abstract')");
		}
		out.indents--;
		out.put("}").ln();
	}

	private void statementsElement(int F, Node.StatementsElement statement) {
    if (statement instanceof Node.LocalBinding l) {
      out.put(l.kind == Node.LocalBinding.Kind.Slot ? "let " : "const ", l.name, " = ");
      expression(F_VALUE, l.value);
      out.put(";").ln();
    } else if (statement instanceof Node.LocalMultiBinding l) {
      localMultibinding(F, l);
    } else if (statement instanceof Node.ReturnStatement r) {
      returnStatement(F, r);
    } else if (statement instanceof Node.StandaloneExpression e) {
      if (e.isLast && is(F, F_RETURN_LAST)) {
        out.put("return ");
        expression(F_VALUE, e.value);
        out.put(";").ln();
      } else {
        expression(F, e.value);
        out.put(";").ln();
      }
    } else {
      out.put("/*unknown ", statement, "*/");
    }
	}

	private void returnStatement(int F, Node.ReturnStatement r) {
		if (is(F, F_ALLOW_RETURN)) {
			if (r.value != Node.ImplicitEmptyExpression) {
				out.put("return ");
				expression(F_VALUE, r.value);
				out.put(";");
			} else {
				out.put("return;");
			}
		} else {
			if (r.value != Node.ImplicitEmptyExpression) {
				out.put("throw ");
				expression(F_VALUE, r.value);
				out.put(";");
			} else {
				out.put("throw new Error('no return');");
			}
		}
	}

	private static boolean is(int flags, int mask) {
		return (flags & mask) != 0;
	}

	private void localMultibinding(int F, Node.LocalMultiBinding l) {
		if (l.values.size() == l.names.size()) {
			for (var i = 0; i < l.names.size(); i++) {
				var n = l.names.get(i);
				var v = l.values.get(i);
				out.put(i == 0 ? "const " : ", ");
				out.put(n).put(" = ");
				expression(F_VALUE, v);
			}
			out.put(";").ln();
		} else {
			out.put("const [");
			names(l.names);
			out.put("] = ");
			if (l.values.size() == 1) {
				expression(F_VALUE, l.values.get(0));
			} else {
				out.put('[');
				components(l.values);
				out.put(']');
			}
			out.put(";").ln();
		}
	}

	private void names(List<Identifier> names) {
		boolean notFirst = false;
		for (var n : names) {
			if (notFirst) out.put(", ");
			else notFirst = true;
			out.put(n);
		}
	}

	private void statementsOrExpression(int F, Node.ExpressionOrStatements body) {
		if (body instanceof Node.Statements statements) {
			if (statements.statements.size() == 1
				&& statements.statements.get(0) instanceof Node.StandaloneExpression expression) {

				if (is(F, F_ALLOW_STATEMENTS) && !is(F, F_RETURN_LAST)) {
					out.put(" {").ln();
					out.indents++;
					// if (is(F, F_RETURN_LAST)) {}
					expression(F_VALUE
						| (F & F_ALLOW_STATEMENTS)
						| (F & F_ALLOW_RETURN), expression.value);

					out.indents--;
					out.ln().put('}');
				} else {
					expression(F_VALUE
						| (F & F_ALLOW_STATEMENTS)
						| (F & F_ALLOW_RETURN), expression.value);
				}
			} else if (is(F, F_ALLOW_STATEMENTS)) {
				out.put(" {").ln();
				out.indents++;
				for (var s : statements.statements) {
					statementsElement(F_ALLOW_STATEMENTS | (F & F_ALLOW_RETURN), s);
				}
				out.indents--;
				out.put("}").ln();
			} else {
				out.put("(() => {").ln();
				out.indents++;
				for (var s : statements.statements) {
					statementsElement(F_ALLOW_STATEMENTS | F_RETURN_LAST, s);
				}
				out.indents--;
				out.put("})()");
			}
		} else if (body instanceof Node.Expression expression) {
/*			out.ln();
			out.indents++;*/
			expression(F_VALUE
				| (F & F_ALLOW_STATEMENTS)
				| (F & F_ALLOW_RETURN), expression); // return and expression?
/*			out.indents--;
			out.ln();*/
		} else if (body instanceof Node.ReturnStatement r) {
			returnStatement(F_VALUE | (F & F_ALLOW_RETURN), r);
		} else Unreachable.exhaustive(body);
	}

	private void expression(int F, Node.Expression e) {
		if (e.inParens) out.put('(');
    if (e instanceof Node.ForStatement fors) {
      forStatement(F, fors);
    } else if (e instanceof Node.IfStatement ifs) {
      ifStatement(F, ifs);
    } else if (e instanceof Node.LiteralMarkup markup) {
      markup(markup);
    } else if (e instanceof Node.LiteralEmpty) {
      out.put("null");
    } else if (e instanceof Node.LiteralProduct product) {
      out.put('[');
      components(product.components);
      out.put(']');
    } else if (e instanceof Node.LiteralArray array) {
      array(F, array);
    } else if (e instanceof Node.LiteralRecord record) {
      record(F, record);
    } else if (e instanceof Node.BinaryOperator o) {
      binaryOperator(o);
    } else if (e instanceof Node.UnaryOperator o) {
      unaryOperator(o);
    } else if (e instanceof Node.LiteralNumber number) {
      out.put(number.literal);
    } else if (e instanceof Node.LiteralString string) {
      string(string);
    } else if (e instanceof Node.LambdaExpression l) {
      if (l.input.isEmpty()) {
        out.put("() => ");
      } else if (l.input instanceof Node.ParameterProduct p
          && p.components.size() == 1
          && p.components.get(0) instanceof Node.ParameterNamedGroup group
          && group.names.size() == 1) {
        var name = group.names.get(0);
        out.put(name, " => ");
      } else {
        featureParameters(l.input);
      }
      statementsOrExpression(F_VALUE, l.body);
    } else if (e instanceof Node.LambdaOperatorExpression l) {
      out.put("(");
      expression(F_VALUE, l.operator.left);
      out.put(",");
      expression(F_VALUE, l.operator.right);
      out.put(") => ");
      expression(F_VALUE, l.operator);
    } else if (e instanceof Node.FeatureApply f) {
      expression(F_VALUE, f.base);
      if (f.select != null) out.put(".", f.select);
      applyArguments(f);
    } else if (e instanceof Node.ConstructorApply c) {
      out.put(c.type.name);
      if (c.alternative != null) {
        out.put('.').put(c.alternative);
      }
      applyArguments(c);
    } else if (e instanceof Node.Reference r) {
      out.put(r.name);
    } else if (e instanceof Node.LiteralBoolean bool) {
      out.put(String.valueOf(bool.value));
    } else {
      out.put("/*").put(e).put("*/");
    }
		if (e.inParens) out.put(')');
	}

	private void array(int F, Node.LiteralArray array) {
		out.put("$s.arr(");
		components(array.components);
		out.put(')');
	}

	private void record(int F, Node.LiteralRecord record) {
		out.put("({").ln();
		out.indents++;
		int last = record.names.size() - 1;
		for (int i = 0; i <= last; i++) {
			out.put(record.names.get(i)).put(": ");
			expression(F, record.components.get(i));
			out.putIf(i != last, ",").ln();
		}
		out.indents--;
		out.put("})");
	}

	private void ifStatement(int F, Node.IfStatement ifs) {
		if (is(F, F_ALLOW_STATEMENTS)) {
			out.put("if (");
			expression(F_VALUE, ifs.condition);
			out.put(") ");
			statementsOrExpression(F_ALLOW_STATEMENTS | (F & F_ALLOW_RETURN), ifs.then);
			if (ifs.otherwise != null) {
				out.put(" else ");
				statementsOrExpression(F_ALLOW_STATEMENTS | (F & F_ALLOW_RETURN), ifs.otherwise);
			}
		} else {
			out.put('(');
			expression(F_VALUE, ifs.condition);
			out.put(") ? (");
			statementsOrExpression(F_VALUE, ifs.then);
			out.put(") : ");
			if (ifs.otherwise != null) {
				out.put('(');
				statementsOrExpression(F_VALUE, ifs.otherwise);
				out.put(')');
			} else {
				out.put("null");
			}
		}
	}

	private int forNesting = 0;

	private void forStatement(int F, Node.ForStatement fors) {
		forNesting++;
		if (is(F, F_ALLOW_STATEMENTS)) {
			out.put("for (const ");
			if (fors.bind.isEmpty()) {
				out.put("[,]");
				//out.put("_$", forNesting);
			} else if (fors.bind.size() == 1) {
				out.put(fors.bind.get(0));
			} else {
				out.put('[');
				names(fors.bind);
				out.put(']');
			}
			out.put(" of ");
			expression(F, fors.iterable);
			out.put(") ");
			statementsOrExpression(F_ALLOW_STATEMENTS | (F & F_ALLOW_RETURN), fors.loop);
		} else {
			out.put("$s.fore(");
			expression(F, fors.iterable);
			out.put(", ");
			if (fors.bind.size() == 0) {
				out.put("()");
			} else if (fors.bind.size() == 1) {
				names(fors.bind);
			} else {
				out.put("([");
				names(fors.bind);
				out.put("])");
			}
			out.put(" => ");
			statementsOrExpression(F_RETURN_LAST, fors.loop);
			out.put(")");
		}
		forNesting--;
	}

	private void markup(Node.LiteralMarkup m) {
    if (m instanceof Node.LiteralTagList tl) {
      out.put("$h($hf, null");
      tagElements(tl.elements);
      out.put(')');
    } else if (m instanceof Node.LiteralTag t) {
      out.put("$h(");
      if (isComponentReference(t.name)) out.put(t.name);
      else {
        out.screenBackendKeyword = false;
        out.put('"').put(t.name).put('"');
        out.screenBackendKeyword = true;
      }
      out.put(", ");
      if (!t.attributeNames.isEmpty()) {
        out.put("{");
        for (int i = 0; i < t.attributeNames.size(); i++) {
          if (i != 0) out.put(", ");
          var name = t.attributeNames.get(i);
          if (name.backendKeyword) {
            out.screenBackendKeyword = false;
            out.put('"').put(name).put('"');
            out.screenBackendKeyword = true;
          } else {
            out.put(name);
          }
          out.put(": ");
          expression(F_VALUE, t.attributeValues.get(i));
        }
        out.put("}");
      } else {
        out.put("null");
      }
      tagElements(t.elements);
      out.put(')');
    } else {
      out.put("/*?", m, "*/");
    }
	}

	private boolean isComponentReference(Identifier name) {
		char firstLetter = name.buffer[name.offset];
		return firstLetter >= 'A' && firstLetter <= 'Z';
	}

	private void tagElements(List<Node.TextElement> elements) {
		for (var e : elements) {
			out.put(',').ln();
      if (e instanceof SourceSpan span) {
        spanText(span);
      } else if (e instanceof Node.Expression expression) {
        expression(F_VALUE, expression);
      } else if (e instanceof Node.Comments comments) {
        out.put("/* ");
        for (var c : comments.comment) {
          spanText(c);
          out.ln();
        }
        out.put(" */");
      } else {
        out.put("/*?", e, "*/");
      }
		}
	}

	private void unaryOperator(Node.UnaryOperator operator) {
		if (operator.identifier == Operators.Question) {
			// option operator '?' is Optional.some(), on backend it's just a value or null
			// '()?' is null
			if (operator.base instanceof Node.LiteralEmpty) {
				out.put("null");
			} else {
				expression(F_VALUE, operator.base);
			}
		} else if (operator.isPrefix) {
			out.put(operator.identifier);
			expression(F_VALUE, operator.base);
		} else {
			expression(F_VALUE, operator.base);
			out.put(operator.identifier);
		}
	}

	private void binaryOperator(Node.BinaryOperator operator) {
		if (operator.identifier == Operators.RangeInclusive) {
			out.put("$s.rgii(");
			expression(F_VALUE, operator.left);
			out.put(", ");
			expression(F_VALUE, operator.right);
			out.put(')');
		} else if (operator.identifier == Operators.RangeExclusiveBegin) {
			out.put("$s.rgei(");
			expression(F_VALUE, operator.left);
			out.put(", ");
			expression(F_VALUE, operator.right);
			out.put(')');
		} else if (operator.identifier == Operators.RangeExclusiveEnd) {
			out.put("$s.rgie(");
			expression(F_VALUE, operator.left);
			out.put(", ");
			expression(F_VALUE, operator.right);
			out.put(')');
		} else if (operator.identifier == Operators.ArrowLeft) {
			expression(F_VALUE, operator.left);
			out.put("_(");
			expression(F_VALUE, operator.right);
			out.put(')');
		} else {
			Identifier backendOperator;
			if (operator.identifier == Operators.ColonAssign) {
				backendOperator = Operators.Assign;
			} else if (operator.identifier == Operators.OrElse) {
				backendOperator = Operators.LogicOr;
			} else if (operator.identifier == Operators.Equals) {
				backendOperator = BackendStrictEq;
			} else if (operator.identifier == Operators.NotEquals) {
				backendOperator = BackendStrictNotEq;
			} else {
				backendOperator = operator.identifier;
			}
			expression(F_VALUE, operator.left);
			out.put(" ", backendOperator, " ");
			expression(F_VALUE, operator.right);
		}
	}

	private void string(Node.LiteralString string) {
		int count = 0;
		if (string.elements.isEmpty()) {
			out.put("\"\"");
		} else if (string.elements.size() == 1
			&& string.elements.get(0) instanceof Node.Expression expression) {
			out.put("String( ");
			expression(F_VALUE, expression);
			out.put(" )");
		} else {
			for (var el : string.elements) {
				boolean first = count++ == 0;
				if (!first) out.put('+');
        if (el instanceof SourceSpan span) {
          spanText(span);
        } else if (el instanceof Node.Expression expression) {
          out.put("( ");
          expression(F_VALUE, expression);
          out.put(" )");
        } else {
          out.put("/* !text! ", el, " */");
        }
			}
		}
	}

	private void spanText(SourceSpan span) {
		out.put('"');
		int exclude = span.offset + span.length;
		for (int p = span.offset; p < exclude; p++) {
			char c = span.buffer[p];
			switch (c) {
			case '\\' -> out.put("\\\\");
			case '\"' -> out.put("\\\"");
			case '\b' -> out.put("\\b");
			case '\f' -> out.put("\\f");
			case '\n' -> {
				/*if (p < exclude - 1) out.put("\\n\"").ln().put("+\"");
				else */
				out.put("\\n");
			}
			case '\r' -> out.put("\\r");
			case '\t' -> out.put("\\t");
			case '\0' -> out.put("\\0");
			default -> out.put(c);
			}
		}
		out.put('"');
	}

	private void applyArguments(Node.Apply f) {
		if (f.input != Node.ImplicitEmptyExpression) {
      if (f.input instanceof Node.LiteralEmpty) {
        out.put("()");
      } else if (f.input instanceof Node.LiteralRecord) {//out.put('(');
        expression(F_VALUE, f.input);
        //out.put(')');
      } else if (f.input instanceof Node.LiteralProduct p) {
        out.put('(');
        components(p.components);
        out.put(')');
      } else if (f.input instanceof Node.LiteralArray a) {
        if (a.components.size() == 1) {
          out.put('[');
          expression(F_VALUE, a.components.get(0));
          out.put(']');
        } else {
          out.put('(').put('[');
          components(a.components);
          out.put(']').put(')');
        }
      } else {
        if (!f.input.inParens) out.put('(');
        expression(F_VALUE, f.input);
        if (!f.input.inParens) out.put(')');
      }
		}
	}

	private void components(List<Node.Expression> components) {
		boolean notFirst = false;
		for (var c : components) {
			if (notFirst) out.put(", ");
			else notFirst = true;
			expression(F_VALUE, c);
		}
	}

	private void tagxDeclaration(Node.TagxDeclaration t) {
		out.ln();
		comments(t.comment);
		out.put("function ", t.name);
		if (t.parameters != null) {
			out.put("({");
			parameterGroups(t.parameters.fields);
			out.put("}) {").ln();
		} else {
			out.put("() {").ln();
		}
		out.indents++;
		if (t.expression != null) {
			out.put("return ");
			expression(F_VALUE, t.expression);
			out.put(';').ln();
		} else {
			for (var f : t.stateFields) {
				out.put("const [", f.name, ", ", f.name, "_] = useState(");
				expression(F_VALUE, f.initial);
				out.put(");").ln();
			}
			int F = F_ALLOW_RETURN | F_ALLOW_STATEMENTS | F_RETURN_LAST;
			for (var s : t.statements) {
				statementsElement(F, s);
			}
		}

		for (var f : t.features) featureDeclaration("function ", f);

		out.indents--;
		out.put("}").ln();
	}

	public void units(List<Unit<Node.Unit>> units) {
		prelude();
		for (var u : units) unit(u);
	}

	private static final Identifier BackendStrictEq = Identifier.StaticPool.id("===");
	private static final Identifier BackendStrictNotEq = Identifier.StaticPool.id("!==");

	private static final int F_VALUE = 0;
	private static final int F_ALLOW_RETURN = 1 << 1;
	private static final int F_ALLOW_STATEMENTS = 1 << 2;
	private static final int F_RETURN_LAST = 1 << 3;
}
