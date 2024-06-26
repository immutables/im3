package io.immutables.lang.typeold2;

import static io.immutables.lang.typeold2.Type.*;
import static io.immutables.lang.typeold2.Types.occurs;

class Unify {
	private Unify() {}

	interface Solution {
		void trivial();
		void recursive(Variable v, Type in);
		void alias(Variable v, Variable from);
		void substitute(Variable v, Type with);
		void bottom(Type to, Type from);
	}

	static void unify(Solution solution, Type to, Type from) {
		if (to.equals(from)) solution.trivial();
		else if (to instanceof Variable v) unifyVariable(solution, v, from);
		else if (from instanceof Variable v) unifyVariable(solution, v, to);
		else if (to instanceof Terminal t) unifyToTerminal(solution, t, from);
		else if (to instanceof Applied a) unifyToApplied(solution, a, from);
		else if (to instanceof Product p) unifyToProduct(solution, p, from);
	}

	static void unifyVariable(Solution solution, Variable v, Type against) {
		// v == against will not be ever true if expected.equals(actual) check would occur in `unify`
		// but this makes this function `unifyVariable` self-sufficient
		// FIXME here we might need to check if we already substituted/aliased this variable (v)
		if (v == against) solution.trivial();
		else if (against instanceof Variable a) solution.alias(v, a);
		else if (occurs(v, against)) solution.recursive(v, against);
		else solution.substitute(v, against);
	}

	// this gets a chance to run type coercions, otherwise expected.equals(actual) would
	// make it will never run or always mismatch
	private static void unifyToTerminal(Solution solution, Type.Terminal t, Type from) {
		assert !t.equals(from);
		solution.bottom(t, from); // add conformance coercions before else
	}

	private static void unifyToApplied(Solution solution, Type.Applied to, Type from) {
		if (from instanceof Applied f && to.constructor() == f.constructor()) {
			var ta = to.arguments;
			var fa = f.arguments;
			if (ta.size() == fa.size()) {
				for (int i = 0; i < ta.size(); i++) {
					unify(solution, ta.get(i), fa.get(i));
				}
			} else solution.bottom(to, from);
		} else solution.bottom(to, from); // add conformance coercions before else
	}

	// this gets a chance to run type coercions, otherwise expected.equals(actual) would
	// make it will never run or always mismatch
	private static void unifyToProduct(Solution solution, Type.Product to, Type from) {
		if (from instanceof Product f) {
			var tc = to.components();
			var fc = f.components();
			if (tc.size() == fc.size()) {
				for (int i = 0; i < tc.size(); i++) {
					unify(solution, tc.get(i), fc.get(i));
				}
			} else solution.bottom(to, from);
		} else solution.bottom(to, from); // add conformance coercions before else
	}
}
