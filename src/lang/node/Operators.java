package io.immutables.lang.node;

import io.immutables.lang.Unreachable;
import io.immutables.meta.Null;

public final class Operators {
	private Operators() {}

	// these initial sized should be enough for the full set of operators
	// and their lookup table in this language
	private static final char[] CHARS = new char[256];
	// this will be sparse, only created operators will be occupied
	private static final Identifier[] LOOKUP_TABLE = new Identifier[256];
	private static int charLimit = 0;

	private static Identifier create(short term) {
		var about = Term.Info.about(term);

		assert term > 0 && term < LOOKUP_TABLE.length;
		assert about.kind() == Term.Kind.Operator || about.kind() == Term.Kind.DelimiterOrOperator;
		assert LOOKUP_TABLE[term] == null : "already created";

		int length = about.symbol().length();
		assert charLimit + length < CHARS.length : "no much room in chars pool";

		about.symbol().getChars(0, length, CHARS, charLimit);
		var identifier = Identifier.create(CHARS, charLimit, length);
		charLimit += length;

		return LOOKUP_TABLE[term] = identifier;
	}

	public static Identifier byTerm(short term) {
		@Null var identifier = LOOKUP_TABLE[term];
		assert identifier != null : "Not a registered operator " + term;
		return identifier;
	}

	public static final Identifier Plus = create(Term.Plus);
	public static final Identifier Minus = create(Term.Minus);
	public static final Identifier Divide = create(Term.Divide);
	public static final Identifier Multiply = create(Term.Multiply);
	public static final Identifier Modulo = create(Term.Modulo);
	public static final Identifier PlusAssign = create(Term.PlusAssign);
	public static final Identifier MinusAssign = create(Term.MinusAssign);
	public static final Identifier DivideAssign = create(Term.DivideAssign);
	public static final Identifier MultiplyAssign = create(Term.MultiplyAssign);
	public static final Identifier ModuloAssign = create(Term.ModuloAssign);
	public static final Identifier ColonAssign = create(Term.ColonAssign);
	public static final Identifier Greater = create(Term.Greater);
	public static final Identifier GreaterEquals = create(Term.GreaterEquals);
	public static final Identifier Lower = create(Term.Lower);
	public static final Identifier LowerEquals = create(Term.LowerEquals);
	public static final Identifier Exclaim = create(Term.Exclaim);
	public static final Identifier Question = create(Term.Question);
	public static final Identifier Ampersand = create(Term.Ampersand);
	public static final Identifier Tilde = create(Term.Tilde);
	public static final Identifier Colon = create(Term.Colon);
	public static final Identifier Assign = create(Term.Assign);
	public static final Identifier ArrowLeft = create(Term.ArrowLeft);
	public static final Identifier ArrowRight = create(Term.ArrowRight);
	public static final Identifier Equals = create(Term.Equals);
	public static final Identifier NotEquals = create(Term.NotEquals);
	public static final Identifier Increment = create(Term.Increment);
	public static final Identifier Decrement = create(Term.Decrement);
	public static final Identifier LogicOr = create(Term.LogicOr);
	public static final Identifier LogicAnd = create(Term.LogicAnd);
	public static final Identifier OrElse = create(Term.OrElse);
	public static final Identifier RangeInclusive = create(Term.RangeInclusive);
	public static final Identifier RangeExclusiveBegin = create(Term.RangeExclusiveBegin);
	public static final Identifier RangeExclusiveEnd = create(Term.RangeExclusiveEnd);
//	public static final Identifier Dot = create(Term.Dot);
//	public static final Identifier Subscript = create(Term.BrackL);

	public interface Precedence {
		byte Access = 2;
		byte Unary = 3;
		byte Ranging = 4;
		byte Multiplicative = 5;
		byte Additive = 6;
		byte Relational = 7;
		byte Logical = 8;
		byte Assigning = 9;

		byte HIGHEST = 1;
		byte LOWEST = 10;

		static byte get(short term) {
			switch (term) {
			case Term.BrackL:// TODO Not sure about indexing
			case Term.Dot:
			case Term.Question:
				return Access;
			case Term.RangeInclusive:
			case Term.RangeExclusiveBegin:
			case Term.RangeExclusiveEnd:
				return Ranging;
			case Term.Multiply:
			case Term.Divide:
			case Term.Modulo:
				return Multiplicative;
			case Term.Plus:
			case Term.Minus:
				return Additive;
			case Term.Greater:
			case Term.Lower:
			case Term.GreaterEquals:
			case Term.LowerEquals:
			case Term.Equals:
			case Term.NotEquals:
				return Relational;
			case Term.LogicAnd:
			case Term.LogicOr:
			case Term.OrElse:
				return Logical;
			case Term.Assign:
			case Term.ColonAssign:
			case Term.PlusAssign:
			case Term.MinusAssign:
			case Term.MultiplyAssign:
			case Term.DivideAssign:
			case Term.ModuloAssign:
			case Term.ArrowLeft:
				return Assigning;
			default: throw Unreachable.contractual();
			}
		}

		static long mask(byte precedence) {
			assert precedence > 0 && precedence <= LOWEST;
			return 1L << precedence;
		}
	}
}
