package io.immutables.lang.syntax;

import io.immutables.lang.node.Term;

public final class Tokenizer {
	public final Terms terms = new Terms();
	private final char[] in;

	public Tokenizer(char[] in) {
		this.in = in;
	}

	/** current tokenization position. */
	private int p = 0;

	public void tokenize() {
		tokenize(false);
	}

	private void tokenize(boolean expectClosingBrace) {
		int openBraces = 0;

		while (p < in.length) {
			char c = in[p];

			switch (c) {
			case ' ':
			case '\t':
			case '\f':
			case '\r':
				if (terms.extend(Term.Whitespace, ++p)) continue;
			case '\n':
				if (terms.put(Term.Newline, ++p)) continue;
			case '/':
				if (lineComment()) continue;
				if (sequence('/', '=', Term.DivideAssign)) continue;
				if (terms.put(Term.Divide, ++p)) continue;
			case '{': {
				terms.put(Term.BraceL, ++p);
				openBraces++;
				continue;
			}
			case '}': {
				terms.put(Term.BraceR, ++p);
				if (openBraces-- == 0 && expectClosingBrace) return;
				continue;
			}
			case '(': if (terms.put(Term.ParenL, ++p)) continue;
			case ')': if (terms.put(Term.ParenR, ++p)) continue;
			case '[': if (terms.put(Term.BrackL, ++p)) continue;
			case ']': if (terms.put(Term.BrackR, ++p)) continue;
			case '=':
				if (sequence('=', '=', Term.Equals)) continue;
				if (terms.put(Term.Assign, ++p)) continue;
			case ',': if (terms.put(Term.Comma, ++p)) continue;
			case '.':
				if (sequence('.', '.', '<', Term.RangeExclusiveEnd)) continue;
				if (sequence('.', '.', Term.RangeInclusive)) continue;
				if (terms.put(Term.Dot, ++p)) continue;
			case '~': if (terms.put(Term.Tilde, ++p)) continue;
/*
			case '@': break;
			case '_': break;
			case '\'': break;
*/
			case '"': if (interpolateString()) continue;
			case '*':
				if (sequence('*', '=', Term.MultiplyAssign)) continue;
				if (terms.put(Term.Multiply, ++p)) continue;
			case '%':
				if (sequence('%', '=', Term.ModuloAssign)) continue;
				if (terms.put(Term.Modulo, ++p)) continue;
			case '+':
				if (sequence('+', '+', Term.Increment)) continue;
				if (sequence('+', '=', Term.PlusAssign)) continue;
				if (terms.put(Term.Plus, ++p)) continue;
			case '-':
				if (sequence('-', '-', Term.Decrement)) continue;
				if (sequence('-', '>', Term.ArrowRight)) continue;
				if (sequence('-', '=', Term.MinusAssign)) continue;
				if (terms.put(Term.Minus, ++p)) continue;
			case '|':
				if (sequence('|', '|', Term.LogicOr)) continue;
				if (terms.put(Term.Pipe, ++p)) continue;
			case ':':
				if (sequence(':', '=', Term.ColonAssign)) continue;
				if (sequence(':', ':', Term.Quadot)) continue;
				if (terms.put(Term.Colon, ++p)) continue;
			case '?':
				if (sequence('?', '?', Term.OrElse)) continue;
				if (terms.put(Term.Question, ++p)) continue;
			case '!':
				if (sequence('!', '=', Term.NotEquals)) continue;
				if (terms.put(Term.Exclaim, ++p)) continue;
			case '&':
				if (sequence('&', '&', Term.LogicAnd)) continue;
				if (terms.put(Term.Ampersand, ++p)) continue;
			case '<':
				if (sequence('<', '=', Term.LowerEquals)) continue;
				if (sequence('<', '-', Term.ArrowLeft)) continue;
				if (terms.current() != Term.Typename && interpolateMarkup()) continue;
				if (terms.put(Term.Lower, ++p)) continue;
			case '>':
				if (sequence('>', '=', Term.GreaterEquals)) continue;
				if (sequence('>', '.', '.', Term.RangeExclusiveBegin)) continue;
				if (terms.put(Term.Greater, ++p)) continue;
			// keywords
			case 'b':
				if (keyword('b', 'o', 'o', 'l', Term.Bool)) continue;
				break;
			case 'c':
				if (keyword('c', 'a' ,'s', 'e', Term.Case)) continue;
				if (keyword('c', 'o', 'n', 'c', 'e', 'p', 't', Term.Concept)) continue;
				break;
			case 'e':
				if (keyword('e', 'l', 's', 'e', Term.Else)) continue;
				break;
			case 'f':
				if (keyword('f', 'o', 'r', Term.For)) continue;
				if (keyword('f', 'a', 'l', 's', 'e', Term.False)) continue;
				if (keyword('f', 'l', 'o', 'a', 't', Term.Float)) continue;
				if (keyword('f', '3', '2', Term.f32)) continue;
				if (keyword('f', '6', '4', Term.f64)) continue;
				break;
			case 'i':
				if (keyword('i', 'f', Term.If)) continue;
				if (keyword('i', 'm', 'p', 'l', Term.Impl)) continue;
				if (keyword('i', 'n', 't', Term.Int)) continue;
				if (keyword('i', '8', Term.i8)) continue;
				if (keyword('i', '1', '6', Term.i16)) continue;
				if (keyword('i', '3', '2', Term.i32)) continue;
				if (keyword('i', '6', '4', Term.i64)) continue;
				break;
			case 'r':
				if (keyword('r', 'e', 't', 'u', 'r', 'n', Term.Return)) continue;
				break;
			case 't':
				if (keyword('t', 'y', 'p', 'e', Term.Type)) continue;
				if (keyword('t', 'r', 'u', 'e', Term.True)) continue;
				if (keyword('t', 'a', 'g', 'x', Term.Tagx)) continue;
				break;
			case 'u':
				if (keyword('u', '8', Term.u8)) continue;
				if (keyword('u', '1', '6', Term.u16)) continue;
				if (keyword('u', '3', '2', Term.u32)) continue;
				if (keyword('u', '6', '4', Term.u64)) continue;
				break;
			}
			switch (classes[c]) {
			case DIGIT -> {
				if (number()) continue;
			}
			case UPPER_LETTER -> {
				if (name(Term.Typename)) continue;
			}
			case LOWER_LETTER -> {
				if (name(Term.Name)) continue;
			}
			}
			terms.extend(Term.Unrecognized, ++p);
		}
	}

	private boolean interpolateMarkup() {
		int openedTags = 0;
		assert in[p] == '<';
		int i = p + 1;
		if (i < in.length) {
			char c = in[i];
			if (c == '>') {
				terms.put(Term.TagListOpen, p = ++i);
				openedTags++;
				// goes down to a loop to parse tag content
			} else if (classes[c] >= UPPER_LETTER) {
				p = i;
				if (openingTag()) {
					if (terms.current() == Term.TagOpenEmptyEnd) {
						// Single already closed (empty) tag
						// as there is no content we just return it
						return true;
					}
					assert terms.current() == Term.TagOpenEnd;
					openedTags++;
					// goes down to a loop to parse tag content
				} else return false;
			} else return false;
		}
		// within tag
		while (p < in.length) {
			char c = in[p];
			switch (c) {
			case '<':
				if (sequence('<', '>', Term.TagListOpen)) {
					openedTags++;
					continue;
				}
				if (sequence('<', '/', '>', Term.TagListClose)
					|| closingTag()) {

					if (--openedTags == 0) return true;
					continue;
				}
				if (p + 1 < in.length && classes[in[p + 1]] >= UPPER_LETTER) {
					p++;
					if (openingTag()) {
						if (terms.current() == Term.TagOpenEnd) {
							openedTags++;
						} else {
							assert terms.current() == Term.TagOpenEmptyEnd;
						}
						continue;
					} else {
						p--;// revert assumption
					}
				}
				terms.extend(Term.Text, ++p);
				continue;
			case '/':
				// is it a good idea to have c-style line comments inside markup?
				if (lineComment()) continue;
				terms.extend(Term.Text, ++p);
				continue;
			case '{': {
				terms.put(Term.BraceL, ++p);
				tokenize(true);
				assert terms.current() == Term.BraceR;
				continue;
			}
			default:
				terms.extend(Term.Text, ++p);
			}
		}
		return true;
	}

	private boolean closingTag() {
		assert in[p] == '<';
		// 4 would include whole </*>, where star is at least one letter
		if (p + 4 < in.length
			&& in[p + 1] == '/'
			&& classes[in[p + 2]] >= UPPER_LETTER) {
			int i = p + 3;
			for (; i < in.length; i++) {
				if (classes[in[i]] < DIGIT) break;
			}
			if (in[i] == '>') {
				terms.put(Term.TagClose, p = ++i);
				return true;
			}
		}
		return false;
	}

	private boolean lineComment() {
		assert in[p] == '/';
		if (p + 1 < in.length && in[p + 1] == '/') {
			p += 2; // past "//"
			while (p < in.length) {
				if (in[p++] == '\n') break;
			}
			return terms.put(Term.LineComment, p);
		}
		return false;
	}

	private boolean openingTag() {
		assert classes[in[p]] >= UPPER_LETTER;
		name(Term.TagOpen);
		while (p < in.length) {
			char c = in[p];
			switch (c) {
			case ' ':
			case '\t':
			case '\f':
			case '\r':
			case '\n':
				if (terms.extend(Term.Whitespace, ++p)) continue;
				// we don't process newlines and line comments here as we do in normal code
			case '>': {
				terms.put(Term.TagOpenEnd, ++p);
				return true;
			}
			case '/': {
				if (sequence('/', '>', Term.TagOpenEmptyEnd)) return true;
				if (terms.extend(Term.Unrecognized, ++p)) continue;
			}
			case '{': {
				terms.put(Term.BraceL, ++p);
				tokenize(true);
				if (terms.current() != Term.BraceR) return false;
				continue;
			}
			case '=': if (terms.put(Term.Assign, ++p)) continue;
			case '"': if (interpolateString()) continue;
			default:
				if (classes[c] >= UPPER_LETTER) {
					name(Term.TagAttribute);
					continue;
				}
				terms.extend(Term.Unrecognized, ++p);
			}
		}
		return true;
	}

	private boolean interpolateString() {
		assert in[p] == '"';
		terms.put(Term.DoubleQuote, ++p);

		while (p < in.length) {
			switch (in[p]) {
			case '\"':
				terms.put(Term.DoubleQuote, ++p);
				return true;
			case '{':
				terms.put(Term.BraceL, ++p);
				tokenize(true);
				if (terms.current() != Term.BraceR) return false;
				continue;
			default:
				terms.extend(Term.Text, ++p);
			}
		}

		return true;
	}

	private boolean number() {
		int i = p;
		i++;
		char c = in[p];
		short term = Term.IntNumber;
		if (c == '0' && i + 1 < in.length) {
			// can be binary or hexadecimal prefix
			c = in[i];
			if (c == 'b') {
				term = Term.BinNumber;
				int v = ++i;
				for (;i < in.length; i++) {
					c = in[i];
					if (c == '_'
						| c == '0' | c == '1');
					else break;
				}
				while (in[i - 1] == '_') i--; // we backtrack of trailing _
				return terms.put(term, p = i);
			} else if (c == 'x') {
				term = Term.HexNumber;
				int v = ++i;
				for (; i < in.length; i++) {
					c = in[i];
					if (c == '_'
						| (c >= '0' & c <= '9')
						| (c >= 'a' & c <= 'f')
						| (c >= 'A' & c <= 'F'));
					else break;
				}
				while (in[i - 1] == '_') i--; // backtrack trailing underscores/ can make it an error
				return terms.put(term, p = i);
			} else {
				// wrong prefix, just take previus number
				return terms.put(term, p = i);
			}
		}
		boolean exp = false;
		boolean dec = false;
		int v = i;
		for (; i < in.length; i++) {
			c = in[i];
			if (c == '.') {
				// no more room for digit or next is not a digit
				char t;
				if (i + 1 == in.length || (t = in[i + 1]) < '0' || t > '9') {
					return terms.put(term, p = i);
				}
				// cannot be more than one decimal dot or dot in exponent,
				// but we don't fail, just refuse next dot to another term
				if (dec | exp) return terms.put(term, p = i);
				dec = true;
				term = Term.DecNumber;
			} else if (c == 'e') {
				// cannot be more than one exponent
				if (exp) return false;
				// not enough letters for exponent
				if (i + 1 >= in.length) return terms.put(term, p = i);

				c = in[i + 1];
				if (c == '+' | c == '-') {
					i++;
					if (i + 1 >= in.length || (c = in[i + 1]) < '0' || c > '9') {
						return terms.put(term, p = --i);
					}
				} else if (c < '0' || c > '9') {
					return terms.put(term, p = i);
				}
				exp = true;
				term = Term.ExpNumber;
			} else if (c == '_' | (c >= '0') & (c <= '9')) {
				// ok just collect digits or separator
			} else break;
		}
		while (in[i - 1] == '_') i--; // backtrack trailing _
		return terms.put(term, p = i);
	}

	private boolean name(short term) {
		int i = p + 1;
		for (; i < in.length; i++) {
			char c = in[i];
			// c >= classes.length is not checked in other places
			// how to fix it with minimum ripples?
			if (c >= classes.length || classes[c] < DIGIT) break;
		}
		return terms.put(term, p = i);
	}

	private boolean sequence(char k0, char k1, short term) {
		assert in[p] == k0;
		return in.length > p + 1
//			&& in[p] == k0
			&& in[p + 1] == k1
			&& terms.put(term, p += 2);
	}

	private boolean sequence(char k0, char k1, char k2, short term) {
		assert in[p] == k0;
		return in.length > p + 2
//			&& in[p] == k0
			&& in[p + 1] == k1
			&& in[p + 2] == k2
			&& terms.put(term, p += 3);
	}

	private boolean keyword(char k0, char k1, short term) {
		assert in[p] == k0;
		return in.length > p + 2
//			&& in[p] == k0
			&& in[p + 1] == k1
			&& classes[in[p + 2]] < DIGIT
			&& terms.put(term, p += 2);
	}

	private boolean keyword(char k0, char k1, char k2, short term) {
		assert in[p] == k0;
		return in.length > p + 3
//			&& in[p] == k0
			&& in[p + 1] == k1
			&& in[p + 2] == k2
			&& classes[in[p + 3]] < DIGIT
			&& terms.put(term, p += 3);
	}

	private boolean keyword(char k0, char k1, char k2, char k3, short term) {
		assert in[p] == k0;
		return in.length > p + 4
//			&& in[p] == k0
			&& in[p + 1] == k1
			&& in[p + 2] == k2
			&& in[p + 3] == k3
			&& classes[in[p + 4]] < DIGIT
			&& terms.put(term, p += 4);
	}

	private boolean keyword(char k0, char k1, char k2, char k3, char k4, short term) {
		assert in[p] == k0;
		return in.length > p + 5
//			&& in[p] == k0
			&& in[p + 1] == k1
			&& in[p + 2] == k2
			&& in[p + 3] == k3
			&& in[p + 4] == k4
			&& classes[in[p + 5]] < DIGIT
			&& terms.put(term, p += 5);
	}

	private boolean keyword(char k0, char k1, char k2, char k3, char k4, char k5, short term) {
		assert in[p] == k0;
		return in.length > p + 6
//			&& in[p] == k0
			&& in[p + 1] == k1
			&& in[p + 2] == k2
			&& in[p + 3] == k3
			&& in[p + 4] == k4
			&& in[p + 5] == k5
			&& classes[in[p + 6]] < DIGIT
			&& terms.put(term, p += 6);
	}

	private boolean keyword(char k0, char k1, char k2, char k3, char k4, char k5, char k6, short term) {
		assert in[p] == k0;
		return in.length > p + 7
//			&& in[p] == k0
			&& in[p + 1] == k1
			&& in[p + 2] == k2
			&& in[p + 3] == k3
			&& in[p + 4] == k4
			&& in[p + 5] == k5
			&& in[p + 6] == k6
			&& classes[in[p + 7]] < DIGIT
			&& terms.put(term, p += 7);
	}

	private static final byte SYMBOLS = 1;
	private static final byte DIGIT = 2;
	private static final byte UPPER_LETTER = 3;
	private static final byte LOWER_LETTER = 4;

	private static final byte[] classes = new byte[127];
	{
		for (char i = '0'; i <= '9'; i++) classes[i] = DIGIT;
		for (char i = 'A'; i <= 'Z'; i++) classes[i] = UPPER_LETTER;
		for (char i = 'a'; i <= 'z'; i++) classes[i] = LOWER_LETTER;
	}
}
