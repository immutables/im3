package io.immutables.lang.test.type;

import io.immutables.lang.Unreachable;
import java.util.ArrayList;

/*
@interface Tidx {}
@interface Nidx {}
@interface Tkind {}
*/
// Layout
// char t[0] u16 len (this index + len == index of next sibling
// char t[1] u16 kind
// char t[2] u16 declaration / reference
// char t[3] u16 arguments count (if applies)
// char t[4+] u16 arguments (usually pointers)
interface Types {
	char TERMINAL = '.';
	char APPLIED = '*';
	char PARAMETER = 'p';
	// this is when variable is used, VARDECLARE is declaration and usage tracking
	char VARIABLE = '_';
	// there exist such unknown type, which may have some constraints attached
	char EXISTENTIAL = '∃';
	char PRODUCT = '(';
	char RECORD = '{';
	char OPTIONAL = '?';
	char VARIANT = '|';

	// these are so-called propositions
	// some of which are feature or type constraints
	// some are code node contexts such as function invocations
	// or variable assignments
	char BIND = ':';
	char INVOKE = '>';
	char WATCH = '%';

	char EQUALITY = '=';
	char CONCEPT = 'c';

	char VARDECLARE = '\'';
	char RELOCATED = '~';
	char ELIMINATED = '∎';
	//char SUBSTITUTED = '✓';
	char ALIASED = '>';

	static boolean isNominal(char t) {
		return switch (t) {
			case TERMINAL, APPLIED, PARAMETER -> true;
			default -> false;
		};
	}

	static boolean isStructural(char t) {
		return switch (t) {
			case PRODUCT, RECORD, OPTIONAL, VARIANT -> true;
			default -> false;
		};
	}

	static boolean hasArguments(char t) {
		return switch (t) {
			case APPLIED, PRODUCT, RECORD, OPTIONAL, VARIANT -> true;
			default -> false;
		};
	}
}

final class TypeBuffer {
	private @interface Unify {}

	/**
	 * this offset can be omitted (+0) but is used for clarity in a number of places.
	 * First u16 (char) contains length in number of entries (2 bytes for each u16 entry).
	 * Basically this index + len under this index = index of a next sibling
	 */
	private static final byte _LEN = 0;
	/**
	 * Kind of the entry, includes kinds of types, propositions
	 */
	private static final byte _KIND = 1;
	private static final byte _DECL = 2;
	// Logical size in number of components/arguments
	private static final byte _SIZE = 3;
	private static final byte _ARGS = 4;

	// using char as u16
	char[] c = new char[1024]; // would be growable
	int clim;

	// using char as u16
	char[] v = new char[512]; // would be growable
	int vlim;

	private int unifyFlags = 0;

	int Terminal(char declaration) {
		int i = clim;
		c[i + _KIND] = Types.TERMINAL;
		c[i + _DECL] = declaration;
		clim = (c[i + _LEN] = _DECL + 1) + i;
		return i;
	}

	int Variable(int reference) {
		int i = clim;
		c[i + _KIND] = Types.VARIABLE;
		c[i + _DECL] = (char) recordVariableUse(reference, i);
		clim = (c[i + _LEN] = _DECL + 1) + i;
		return i;
	}

	int Applied(char declaration, int size) {
		assert size > 0;
		int i = clim;
		c[i + _KIND] = Types.APPLIED;
		c[i + _DECL] = declaration;
		c[i + _SIZE] = (char) size;
		clim = (c[i + _LEN] = _SIZE + 1) + i;
		//should be ended with #end method
		return i;
	}

	void end(int i) {
		c[i + _LEN] = (char) (clim - i);
	}

	int Bind(char declaration) {
		int i = clim;
		c[i + _KIND] = Types.BIND;
		c[i + _DECL] = declaration;
		c[i + _SIZE] = 2;
		clim = (c[i + _LEN] = _SIZE + 1) + i;
		// should be ended with #end method
		return i;
	}

	int Equality(char declaration) {
		int i = clim;
		c[i + _KIND] = Types.EQUALITY;
		c[i + _DECL] = declaration;
		c[i + _SIZE] = 2;
		clim = (c[i + _LEN] = _SIZE + 1) + i;
		// should be ended with #end method
		return i;
	}

	int WatchType(char declaration) {
		int i = clim;
		c[i + _KIND] = Types.WATCH;
		c[i + _DECL] = declaration;
		c[i + _SIZE] = 1;
		clim = (c[i + _LEN] = _SIZE + 1) + i;
		// requires end1
		return i;
	}

	int WatchVariable(int reference) {
		reference = resolveRelocatedVariable(reference);
		int i = clim;
		c[i + _KIND] = Types.WATCH;
		c[i + _DECL] = v[reference + _DECL];
		c[i + _SIZE] = 1;
		clim = _SIZE + 1 + i;

		// write variable inline
		Variable(reference);

		c[i + _LEN] = (char) (clim - i);
		return i;
	}

	private static final int VAR_USE_DEFAULT_CAPACITY = 8;

	int VariableDeclare(char declaration) {
		return VariableDeclare(declaration, VAR_USE_DEFAULT_CAPACITY);
	}

	int VariableDeclare(char declaration, int capacity) {
		int i = vlim;
		v[i + _KIND] = Types.VARDECLARE;
		v[i + _DECL] = declaration;
		v[i + _SIZE] = 0; // count how many usages there are
		vlim = (v[i + _LEN] = (char) (_SIZE + 1 + capacity)) + i;
		return i;
	}

	private int recordVariableUse(int reference, int from) {
		int i = resolveRelocatedVariable(reference);
		int capacity = v[i + _LEN] - _ARGS; //for t0(len), t1(head), and t2(count)
		int count = v[i + _SIZE];
		if (capacity - count < 1) {
			i = relocateVariableUse(i, VAR_USE_DEFAULT_CAPACITY);
		}
		v[i + _SIZE] = (char) (count + _KIND);
		v[i + _ARGS + count] = (char) from;
		return i;
	}

	private int resolveRelocatedVariable(int reference) {
		int i = reference;
		while (v[i + _KIND] == Types.RELOCATED) {
			i = v[i + _DECL];
		}
		assert v[i + _KIND] == Types.VARDECLARE;
		return i;
	}

	private int resolveRelocated(int i) {
		while (c[i + _KIND] == Types.RELOCATED) {
			i = c[i + _DECL];
		}
		return i;
	}

	private int relocateVariableUse(int vi, int additionalCapacity) {
		assert v[vi + _KIND] == Types.VARDECLARE;
		assert additionalCapacity > 1;

		int len = v[vi + _LEN];
		int dest = vlim;

		System.arraycopy(v, vi, v, dest, len);

		len += additionalCapacity;
		v[dest + _LEN] = (char) len;
		v[vi + _KIND] = Types.RELOCATED;
		v[vi + _DECL] = (char) dest;
		vlim = dest + len;

		// fix outdated references, we do this before adding a new one
		int count = v[vi + _SIZE];
		for (int j = 0; j < count; j++) {
			int backreference = v[vi + _ARGS + j];
			c[backreference + _DECL] = (char) dest;
		}
		return dest;
	}

	private void substituteVariable(int variableReference, int substitution) {
		int vi = resolveRelocatedVariable(variableReference);

		assert v[vi + _KIND] == Types.VARDECLARE;
		v[vi + _KIND] = Types.ELIMINATED;
		char declaration = v[vi + _DECL];
		int vsize = v[vi + _SIZE];

		char substitutionLen = c[substitution + _LEN];
		char substitutionKind = c[substitution + _KIND];

		switch (substitutionKind) {
		case Types.TERMINAL -> {
			// this branch is for any inline substitution - entry of the same size
			// in this case sizeof Terminal == sizeof Variable
			char substitutionDeclaration = c[substitution + _DECL];
			for (int j = vi + _ARGS, jlim = j + vsize; j < jlim; j++) {
				int ventry = v[j];

				assert c[ventry + _LEN] >= substitutionLen; // allow for smaller replacements
				assert c[ventry + _KIND] == Types.VARIABLE;

				c[ventry + _KIND] = Types.TERMINAL;
				c[ventry + _DECL] = substitutionDeclaration;
			}
		}
		case Types.VARIABLE -> {
			// This is simple variable substitution with a sample,
			// tracking new usages and eliminating substituted variable.
			// During unification, we use another routine for aliasing variables
			int substituteReference = c[substitution + _DECL];
			for (int j = vi + _ARGS, jlim = j + vsize; j < jlim; j++) {
				int ventry = v[j];
				assert c[ventry + _LEN] == substitutionLen;
				assert c[ventry + _KIND] == Types.VARIABLE;
				c[ventry + _DECL] = (char) recordVariableUse(substituteReference, ventry);
			}
		}
		case Types.PRODUCT, Types.APPLIED -> { // TODO and others
			// common substitution by relocation
			for (int j = vi + _ARGS, jlim = j + vsize; j < jlim; j++) {
				int ventry = v[j];

				assert c[ventry + _LEN] < substitutionLen;
				assert c[ventry + _KIND] == Types.VARIABLE;

				c[ventry + _KIND] = Types.RELOCATED;
				c[ventry + _DECL] = (char) substitution;
			}
		}
		default -> {
			assert false : "no substitution possible to kind of "
				+ substitutionKind + " for index " + substitution;
		}
		}
	}

	boolean unify(int proposition) {
		int p = proposition;
		char propositionKind = c[p + _KIND];
		unifyFlags = 0;
		return switch (propositionKind) {
			case Types.EQUALITY, Types.BIND -> {
				assert c[p + _SIZE] == 2; // "2 types: (to, from)"
				int to = p + _ARGS;
				int from = to + c[to];
				reportUnify(propositionKind, to, from);
				yield unify(to, from);
			}
			default -> {
				assert false : "unsupported yet proposition kind: " + propositionKind;
				yield false;
			}
		};
	}

	private @Unify boolean unify(int to, int from) {
		to = resolveRelocated(to);
		from = resolveRelocated(from);

		char kindTo = c[to + _KIND];
		char kindFrom = c[from + _KIND];

		// special case to prefer reverse matching for the whole variable
		if (kindTo != Types.VARIABLE && kindFrom == Types.VARIABLE)
			return unifyToVariable(from, to);

		return switch (kindTo) {
			case Types.VARIABLE -> unifyToVariable(to, from);
			case Types.TERMINAL -> unifyToTerminal(to, from);
			case Types.APPLIED -> unifyToApplied(to, from);
			case Types.PRODUCT -> unifyToProduct(to, from);
			default -> bottom(from, to);
		};
	}

	private @Unify boolean unifyToTerminal(int terminal, int from) {
		char fromKind = c[from + _KIND];
		return switch (fromKind) {
			case Types.TERMINAL -> {
				yield declarationOf(terminal) == declarationOf(from)
					? trivial(terminal, from)
					: bottom(terminal, from);
			}
			case Types.VARIABLE -> throw Unreachable.contractual();
			default -> bottom(terminal, from);
		};
	}

	private @Unify boolean unifyToApplied(int applied, int from) {
		char fromKind = c[from + _KIND];
		return switch (fromKind) {
			case Types.APPLIED -> {
				int size = sizeOf(applied);
				if (declarationOf(applied) == declarationOf(from)
					&& size == sizeOf(from)) {
					yield unifyProductArguments(applied, from, size);
				} else {
					yield bottom(applied, from);
				}
			}
			case Types.VARIABLE -> throw Unreachable.contractual();
			default -> bottom(applied, from);
		};
	}

	private @Unify boolean unifyProductArguments(int to, int from, int size) {
		int mt = to + _ARGS;
		int mf = from + _ARGS;
		while (size-- > 0) {
			if (!unify(mt, mf)) return false;
			mt += c[mt];
			mf += c[mf];
		}
		return true;
	}

	private @Unify boolean unifyToProduct(int product, int from) {
		char fromKind = c[from + _KIND];
		return switch (fromKind) {
			case Types.VARIABLE -> throw Unreachable.contractual();
			default -> bottom(product, from);
		};
	}

	private @Unify boolean unifyToVariable(int variable, int from) {
		if (c[from + _KIND] == Types.VARIABLE) {
			return referenceOf(variable) == referenceOf(from)
				? trivial(variable, from)
				: alias(variable, from);
		} else {
			return !occurs(variable, from)
				&& substitute(variable, from);
		}
	}

	private @Unify boolean substitute(int variable, int from) {
		reportSubstitute(variable, from);

		substituteVariable(c[variable + _DECL], from);
		return true;
	}

	private @Unify boolean occurs(int variable, int in) {
		return isOrOccursIn(c[variable + _DECL], in) && recursive(variable, in);
	}

	private @Unify boolean alias(int variable, int from) {
		reportAlias(variable, from);

		int a = referenceOf(variable);
		int b = referenceOf(from);

		int capacity = v[a + _SIZE] + v[b + _SIZE] + VAR_USE_DEFAULT_CAPACITY;
		char aliasDeclaration = v[b + _DECL]; // just use "from" decl?

		int alias = VariableDeclare(aliasDeclaration, capacity);
		substituteVariableWithAlias(a, alias);
		substituteVariableWithAlias(b, alias);
		return true;
	}

	private void substituteVariableWithAlias(int vi, int alias) {
		int vsize = v[vi + _SIZE];
		for (int j = vi + _ARGS, jlim = j + vsize; j < jlim; j++) {
			int ventry = v[j];
			assert c[ventry + _KIND] == Types.VARIABLE;
			c[ventry + _DECL] = (char) recordVariableUse(alias, ventry);
		}
		v[vi + _KIND] = Types.ALIASED;
		v[vi + _DECL] = (char) alias;
	}

	private boolean isOrOccursIn(int variableReference, int in) {
		in = resolveRelocated(in);
		char kind = c[in + _KIND];
		if (kind == Types.VARIABLE) {
			return c[in + _DECL] == variableReference;
		}
		if (Types.hasArguments(kind)) {
			int m = in + _ARGS;
			int end = in + c[in];
			while (m < end) {
				if (isOrOccursIn(variableReference, m)) return true;
				m += c[m];
			}
		}
		return false;
	}

	private @Unify char declarationOf(int i) {
		return c[i + _DECL];
	}

	private @Unify char sizeOf(int i) {
		return c[i + _SIZE];
	}

	private @Unify char referenceOf(int i) {
		return c[i + _DECL];
	}

/*	private boolean equal(int a, int b) { // FIXME not actually equality for complex types
		return c[a + _KIND] == c[b + _KIND] && c[a + _DECL] == c[b + _DECL];
	}*/

	private boolean trivial(int to, int from) {
		reportTrivial(to, from);
		return true;
	}

	private boolean bottom(int to, int from) {
		reportBottom(to, from);
		return false;
	}

	private boolean recursive(int variable, int in) {
		reportRecusive(variable, in);
		return false;
	}

	private void reportTrivial(int to, int from) {
		System.out.println("TRIVIAL " + showType(to) + " == " + showType(from));
	}

	private void reportBottom(int to, int from) {
		System.out.println("BOTTOM " + showType(to) + " ⊥ " + showType(from)); //  U+22A5
	}

	private void reportRecusive(int variable, int in) {
		System.out.println("OCCURS " + showType(variable) + " in " + showType(in));
	}

	private void reportAlias(int variable, int from) {
		System.out.println("ALIAS  "
			+ showType(variable) + " ↦ " + showType(from));
	}

	private void reportSubstitute(int variable, int from) {
		System.out.println("SUBSTITUTE "
			+ showType(variable) + " ⤇ " + showType(from));
	}

	private void reportUnify(char propositionKind, int to, int from) {
		System.out.printf("UNIFY  %s %s %s%n", showType(to), propositionKind, showType(from));
	}

	String showType(int i) {
		int len = c[i];
		char kind = c[i + _KIND];

		return switch (kind) {
			case Types.RELOCATED -> {
				int where = i;
				do where = c[where + _DECL];
				while (c[where + _KIND] == Types.RELOCATED);
				yield showType(where);
			}
			case Types.TERMINAL -> {
				char declaration = c[i + _DECL];
				yield String.valueOf(declaration);
			}
			case Types.APPLIED -> {
				char declaration = c[i + _DECL];
				int count = c[i + _SIZE];
				var arguments = new ArrayList<String>();
				int m = i + _ARGS;
				int end = i + len;
				while (m < end) {
					arguments.add(showType(m));
					m += c[m];
				}
				yield declaration + "<" + String.join(",", arguments) + ">";
			}
			case Types.VARIABLE -> {
				int declarationRef = c[i + _DECL];
				int reference = resolveRelocatedVariable(declarationRef);
				char declaration = v[reference + _DECL]; // `v` here, not `a`
				yield declaration + "'";
			}
			default -> "`" + kind + "`";
		};
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		for (int j = 0; j < clim; ) {
			assert c[j] != 0 :
				"unoccupied slot outside of top level element at " + j;
			j += show(b, c, 0, j);
		}
		b.append("VARS").append('\n');
		for (int j = 0; j < vlim; ) {
			assert v[j] != 0;
			j += show(b, v, 0, j);
		}
		return b.toString();
	}

	private int show(StringBuilder b, char[] a, int nesting, int i) {
		int len = a[i];
		char kind = a[i + _KIND];

		b.append(String.format("%04d..+%04d| ", i, len));

		indent(b, nesting);

		switch (kind) {
		case Types.TERMINAL -> {
			char declaration = a[i + _DECL];
			b.append(kind).append(' ')
				.append(declaration)
				.append('\n');
		}
		case Types.APPLIED -> {
			char declaration = a[i + _DECL];
			int size = a[i + _SIZE];
			b.append(kind).append(' ')
				.append(declaration)
				.append('<').append(size).append('>')  //.append((char) (0x2080 + count))
				.append('\n');

			int m = i + _ARGS;
			int end = i + len;
			while (m < end) {
				m += show(b, a, nesting + _KIND, m);
			}
		}
		case Types.VARIABLE -> {
			int reference = resolveRelocatedVariable(a[i + _DECL]);
			char declaration = v[reference + _DECL]; // v here, not a
			b.append(kind).append(' ')
				.append(declaration)
				.append('\'').append(String.format("\t\t#%04d", reference))
				.append('\n');
		}
		case Types.BIND, Types.EQUALITY -> {
			char declaration = a[i + _DECL];
			b.append(kind).append(' ')
				.append(declaration)
				.append('\n');
			assert a[i + _SIZE] == 2;
			int m = i + _ARGS;
			int end = i + len;
			while (m < end) {
				m += show(b, a, nesting + _KIND, m);
			}
		}
		case Types.WATCH -> {
			char declaration = a[i + _DECL];
			b.append(kind).append(' ')
				.append(declaration)
				.append('\n');
			assert a[i + _SIZE] == 1;
			int m = i + _ARGS;
			show(b, a, nesting + _KIND, m);
		}
		case Types.VARDECLARE -> {
			char declaration = a[i + _DECL];
			int count = a[i + _SIZE];
			b.append(kind).append(' ')
				.append(declaration).append(' ');
			boolean notFirst = false;
			for (int j = 0; j < count; j++) {
				int ref = a[i + _ARGS + j];
				b.append(String.format("#%04d ", ref));
			}
			b.append('\n');
		}
		case Types.RELOCATED -> {
			int destination = a[i + _DECL];
			b.append(kind).append(' ')
				.append(String.format("#%04d", destination))
				.append('\n');
		}
		case Types.ELIMINATED -> {
			char declaration = a[i + _DECL];
			b.append(kind).append(' ')
				.append(declaration)
				.append('\n');
		}
		case Types.ALIASED -> {
			int destination = a[i + _DECL];
			b.append(kind).append(' ')
				.append(String.format("#%04d", destination))
				.append('\n');
		}
		default -> {
			b.append(kind).append(" (\\u").append((int) kind).append(")").append('\n');
		}
		}
		return len;
	}

	private static void indent(StringBuilder b, int nesting) {
		for (int j = 0; j < nesting * 2; j++) b.append(' ');
	}

	public static void main(String[] args) {
		TypeBuffer tb = new TypeBuffer();

		int X_ = tb.VariableDeclare('X');
		int Y_ = tb.VariableDeclare('Y');

		int b = tb.Equality('b');
		int A = tb.Applied('A', 2);
		int Y = tb.Variable(Y_);
		int C = tb.Terminal('C');
		tb.end(A);
		int A2 = tb.Applied('A', 2);
		int B = tb.Terminal('B');
		int X = tb.Variable(X_);
		tb.end(A2);
		tb.end(b);
		tb.WatchVariable(Y_);
		tb.WatchVariable(X_);

		System.out.println(tb);
		System.out.println("------------");

		tb.unify(b);

		System.out.println(tb);
		System.out.println("------------");
		System.out.println("C BYTES " + (tb.clim * 2));
		System.out.println("V BYTES " + (tb.vlim * 2));
	}
}
