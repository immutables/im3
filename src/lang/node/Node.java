package io.immutables.lang.node;

import io.immutables.lang.Unreachable;
import io.immutables.lang.typeold2.Type;
import io.immutables.meta.Null;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public abstract class Node {
	public int productionIndex = -1;

	public static class Unit extends Node {
		public final List<Element> elements = new ArrayList<>();
	}

	public static abstract class Element extends Node {}

	public static class Blanks extends Element {}

	public static final Blanks Blanks = new Blanks();

	public static class Comments extends CommentedOn {}

	public static abstract class CommentedOn extends Element {
		public final List<SourceSpan> comment = new ArrayList<>();
	}

	public static class ValueBinding extends NamedDeclaration {
		public Expression value;
	}

	public static abstract class Declaration extends CommentedOn {}

	public static abstract class NamedDeclaration extends Declaration {
		public Identifier name;
	}

	public static class TypeDeclaration extends NamedDeclaration {
		public ParametersOrConstructors shape = ImplicitParameterEmpty;
		public @Null ImplFeatures impl;
	}

	public static class ImplFeatures extends Node {
		public final List<Element> elements = new ArrayList<>();
	}

	public static class ConceptDeclaration extends Declaration {
		public Identifier name;
	}

	public static abstract class ParametersOrConstructors extends Node {
		public boolean isImplicitlyEmpty() {
			return this == ImplicitParameterEmpty;
		}

		public boolean isEmpty() {
			return this instanceof ParameterEmpty
				|| (this instanceof ParameterProduct p && p.components.isEmpty());
		}

		public boolean isRecord() {
			return this instanceof ParameterRecord;
		}
	}

	public static abstract class Parameters extends ParametersOrConstructors {}

	public static class ConstructorCases extends ParametersOrConstructors {}

	public static class ParameterEmpty extends Parameters {}

	public static class ParameterProduct extends Parameters {
		public final List<ParameterProductComponent> components = new ArrayList<>();
	}

	public static class ParameterRecord extends Parameters {
		public final List<ParameterNamedGroup> fields = new ArrayList<>();
	}

	public static abstract class ParameterProductComponent extends CommentedOn {
		public TypeUse type;
		public @Null Expression defaultValue;
	}

	public static class ParameterUnnamed extends ParameterProductComponent {}

	public static class ParameterNamedGroup extends ParameterProductComponent {
		public final List<Identifier> names = new ArrayList<>();
	}

	public static abstract class TypeUse extends Node {}

	// TODO
	public static class TypeWrapper extends TypeUse {
		private TypeUse current;

		public void optional() {
			var t = TypeContainer.kind(TypeContainer.Kind.Optional);
			t.content = current;
			current = t;
		}

		public void slot() {
			var t = TypeContainer.kind(TypeContainer.Kind.Slot);
			t.content = current;
			current = t;
		}

		public TypeUse unwrap() {
			return current;
		}
	}

	public static class TypeVariant extends TypeUse {}

	public static abstract class TypeInvariant extends TypeUse {}

	public static class TypeEmpty extends TypeInvariant {}

	// TODO
	public static abstract class TypeWithComponents extends TypeInvariant {
		public List<TypeUse> components = new ArrayList<>();
	}

	public static class TypeProduct extends TypeWithComponents {}

	public static class TypeRecord extends TypeWithComponents {}

	public static class TypeNominal extends TypeInvariant {
		public Identifier name;
		public @Null List<TypeUse> arguments;
	}

	public static class TypeContainer extends TypeInvariant {
		public Kind kind;
		public TypeUse content;
		//public @Null TypeUse argumentSecond;

		public enum Kind {
			Array,
			Optional,
			Slot
		}

		public static TypeContainer kind(Kind k) {
			var t = new TypeContainer();
			t.kind = k;
			return t;
		}
	}

	public static final ParameterEmpty ImplicitParameterEmpty = new ParameterEmpty();

	public static class FeatureDeclaration extends NamedDeclaration {
		public Parameters input = ImplicitParameterEmpty;
		public TypeUse output;
		public @Null Statements statements;
	}

	public interface ExpressionOrStatements {}

	public static class Statements extends Node implements ExpressionOrStatements {
		public List<StatementsElement> statements = new ArrayList<>(0);

		public Statements markLast() {
			var last = statements.get(statements.size() - 1);
			if (last instanceof StandaloneExpression e) e.isLast = true;
			return this;
		}
	}

	//	public static abstract class StatementsElement extends Node {}

	public interface StatementsElement {}

	public static abstract class Expression extends Node
		implements TextElement, ExpressionOrStatements {
		public boolean inParens;
	}

	public static abstract class Statement extends Node implements StatementsElement {}

	public static class LiteralEmpty extends Expression {}

	public static class ImplicitEmpty extends LiteralEmpty {}

	public static final LiteralEmpty ImplicitEmptyExpression = new ImplicitEmpty();

	public static class ReturnStatement extends Statement implements ExpressionOrStatements {
		public Expression value = ImplicitEmptyExpression;
	}

	public static class StandaloneExpression extends Statement {
		public Expression value;
		public boolean isLast;
	}

	public static class Reference extends Expression {
		public Identifier name;
	}

	public static class LocalMultiBinding extends Statement {
		public final List<Identifier> names = new ArrayList<>();
		public final List<Expression> values = new ArrayList<>();
	}

	public static class LocalBinding extends Statement {
		// no destructuring for now
		public Identifier name;
		public Expression value;
		// public final List<Identifier> names = new ArrayList<>();
		// public final List<Expression> values = new ArrayList<>();
		public Kind kind = Kind.Value;

		public enum Kind {
			Value,
			Slot
		}

		public LocalBinding slot() {
			kind = Kind.Slot;
			return this;
		}
	}

	public static abstract class Literal extends Expression {}

	public static abstract class LiteralWithText extends Expression {
		public final List<TextElement> elements = new ArrayList<>(0);
	}

	public static abstract class LiteralMarkup extends LiteralWithText {}

	public static class LiteralTagList extends LiteralMarkup {}

	public static class LiteralTag extends LiteralMarkup {
		public Identifier name;
		public boolean isEmpty;
		public final List<Identifier> attributeNames = new ArrayList<>(); // TODO free-form attributes/dict ?
		public final List<Expression> attributeValues = new ArrayList<>();
	}

	public interface TextElement {}

	public static class LiteralString extends LiteralWithText {}

	public static class LiteralBoolean extends Expression {
		public boolean value;

		public void fromTerm(short t) {
			value = switch (t) {
				case Term.True -> true;
				case Term.False -> false;
				default -> throw Unreachable.contractual();
			};
		}
	}

	public interface ExpressionElement {}

	public static class LiteralNumber extends Expression {
		public SourceSpan literal;
		public Kind kind = Kind.Int;

		public enum Kind {
			Int,
			Dec,
			Exp,
			Bin,
			Hex
		}

		public void fromTerm(short t) {
			kind = switch (t) {
				case Term.IntNumber -> Kind.Int;
				case Term.DecNumber -> Kind.Dec;
				case Term.ExpNumber -> Kind.Exp;
				case Term.BinNumber -> Kind.Bin;
				case Term.HexNumber -> Kind.Hex;
				default -> throw Unreachable.contractual();
			};
		}

		public static LiteralNumber number(SourceSpan span) {
			var e = new LiteralNumber();
			e.literal = span;
			return e;
		}
	}

	public static abstract class Apply extends Expression {
		public Expression input = ImplicitEmptyExpression;

		public boolean isDefaultEmpty() {
			return input == ImplicitEmptyExpression;
		}
	}

	public static class FeatureApply extends Apply {
		public Expression base;
		public @Null Identifier select;
	}

	public static class ConstructorApply extends Apply {
		public TypeNominal type;
		public @Null Identifier alternative; // For enum case

		public boolean isCaseConstructor() {
			return alternative != null;
		}
	}

	public static class BinaryOperator extends Expression {
		public final Identifier identifier;
		public final short term;
		public final byte precedence;

		public Expression left;
		public Expression right;

		public BinaryOperator(short term) {
			this.term = term;
			this.identifier = Operators.byTerm(term);
			this.precedence = Operators.Precedence.get(term);
		}
	}

	public static class UnaryOperator extends Expression implements StatementsElement {
		public final Identifier identifier;
		public final short term;
		public final boolean isPrefix;

		public Expression base;

		public UnaryOperator(short term, boolean isPrefix) {
			this.term = term;
			this.isPrefix = isPrefix;
			this.identifier = Operators.byTerm(term);
		}
	}

	public static class ApplyChain extends Expression {
		public Expression base;
		private final Deque<Object> arguments = new ArrayDeque<>();

		public void select(Identifier identifier) {
			arguments.add(identifier);
		}

		public void apply(Expression expression) {
			arguments.add(expression);
		}

		public void option(short term) {
			arguments.add(new UnaryOperator(term, false));
		}

		public Expression fold() {
			Expression base = this.base;

			@Null FeatureApply selected = null;
			@Null Object a;
			while ((a = arguments.poll()) != null) {
				if (a instanceof Identifier name) {
					selected = new FeatureApply();
					selected.base = base;
					selected.select = name;
					base = selected;
				} else if (a instanceof UnaryOperator operator) {
					selected = null;
					operator.base = base;
					base = operator;
				} else if (a instanceof Expression expression) {
					if (selected != null) {
						selected.input = expression;
						selected = null;
					} else {
						var f = new FeatureApply();
						f.base = base;
						f.input = expression;
						base = f;
					}
				}
			}
			return base;
		}
	}

	public static abstract class DelimitedExpression extends Expression {

	}

	public static abstract class DelimitedComponents extends DelimitedExpression {
		public final List<Expression> components = new ArrayList<>();
	}

	public static class LiteralProduct extends DelimitedComponents {
		public Expression fold() {
			// simplify common cases of product such as empty and
			// just one expression in parentheses
			if (components.isEmpty()) return new LiteralEmpty();
			if (components.size() == 1) {
				// Here a single components product just deteriorates into
				// just an expression in parentheses.
				var e = components.get(0);
				e.inParens = true;
				return e;
			}
			return this;
		}
	}

	public static class LiteralRecord extends DelimitedComponents {
		public final List<Identifier> names = new ArrayList<>();
	}

	public static class LiteralArray extends DelimitedComponents {}

	public static class OperatorSoup extends Expression {
		private final List<Expression> operands = new ArrayList<>();
		private final List<BinaryOperator> operators = new ArrayList<>(0);
		private @Null UnaryOperator unaryOperator;
		private long precedences;

		public Expression fold() {
			assert !operands.isEmpty();

			if (unaryOperator != null) {
				assert operands.size() == 1;
				unaryOperator.base = operands.get(0);
				return unaryOperator;
			}

			if (operands.size() > 1) {
				for (byte p = Operators.Precedence.HIGHEST; p <= Operators.Precedence.LOWEST; p++) {
					if ((precedences & Operators.Precedence.mask(p)) != 0) {
						reduceOperandsWith(p);
					}
				}
			}
			assert operands.size() == 1;
			return operands.get(0);
		}

		private void reduceOperandsWith(byte precedence) {
			assert operands.size() == operators.size() + 1;

			for (int i = 0; i < operators.size();) {
				BinaryOperator operator = operators.get(i);
				if (operator.precedence == precedence) {
					operator.left = operands.get(i);
					operator.right = operands.get(i + 1);
					// replace left operand with binary operator expression
					operands.set(i, operator);
					// remove current operator and right operand
					operators.remove(i);
					operands.remove(i + 1);
					// stay on the same i
				} else i++; // increment i
			}
		}

		public void binaryOperator(short term) {
			assert operands.size() == operators.size() + 1 : "out of sequence: operator(term)";
			// create and fill binary operator
			// but keep left and right uninitialized (null) until we build
			// up a tree of operators in process()
			var o = new BinaryOperator(term);
			precedences |= Operators.Precedence.mask(o.precedence);

			operators.add(o);
		}

		public void operand(Expression e) {
			assert operands.size() == operators.size() : "out of sequence: operand(expression)";
			operands.add(e);
		}

		public void assignOperator(short term) {
			// TODO is it really ok, the same? i.e. difference is only in how production is parsed
			// but not in how tree is built, given the assign operator precedence will be lowest
			binaryOperator(term);
			assert operators.size() == 1 : "assign must be leftmost operator, grammar enforced";
		}

		public void prefixOperator(short term) {
			unaryOperator(term, true);
		}

		public void suffixOperator(short term) {
			unaryOperator(term, false);
		}

		private void unaryOperator(short term, boolean prefix) {
			unaryOperator = new UnaryOperator(term, prefix);
		}
	}

	public static class IfStatement extends Expression {
		public Expression condition;
		public ExpressionOrStatements then;
		public @Null ExpressionOrStatements otherwise;
	}

	public static class ForStatement extends Expression {
		public final List<Identifier> bind = new ArrayList<>();
		public Expression iterable;
		public ExpressionOrStatements loop;
	}

	public static class CaseStatement extends Expression {
		public Expression matched;
		public final List<CasePattern> patterns = new ArrayList<>();
		public final List<ExpressionOrStatements> outcomes = new ArrayList<>();
		public @Null Expression otherwise;
	}

	public static class CasePattern extends Node {
		public @Null Type.Nominal nominal;
		public @Null Identifier bind;
		public final List<CasePattern> components = new ArrayList<>();
		public Structural kind;
		public int size;

		public enum Structural {
			Array,
			Record,
			Product,
			Value,
		}

		public static CasePattern of(Structural kind) {
			var p = new CasePattern();
			p.kind = kind;
			return p;
		}
	}

	public static class LambdaExpression extends Expression {
		public Parameters input = ImplicitParameterEmpty;
		public ExpressionOrStatements body;
	}

	public static class LambdaOperatorExpression extends Expression {
		public BinaryOperator operator;

		public void operator(short term) {
			this.operator = new BinaryOperator(term);

			var left = new Node.Reference();
			var right = new Node.Reference();
			left.name = leftName;
			right.name = rightName;

			this.operator.left = left;
			this.operator.right = right;
		}

		private static final Identifier leftName = Identifier.StaticPool.id("l");
		private static final Identifier rightName = Identifier.StaticPool.id("r");
	}

	public static class TagxDeclaration extends NamedDeclaration {
		public @Null ParameterRecord parameters;
		public @Null LiteralMarkup expression;
		public List<StatementsElement> statements = new ArrayList<>();
		public List<StateField> stateFields = new ArrayList<>();
		public List<FeatureDeclaration> features = new ArrayList<>();

		public TagxDeclaration markLast() {
			if (!statements.isEmpty()) {
				var last = statements.get(statements.size() - 1);
				if (last instanceof StandaloneExpression e) e.isLast = true;
			}
			return this;
		}
	}

	public static class StateField extends Node {
		public Identifier name;
		public Expression initial;
	}
}
