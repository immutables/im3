package io.immutables.lang.typeold2;

import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Constraint<C extends Constraint<C>> {
	public int originProductionIndex;
	public Where where;

	public enum Where { Output, Input, Constraint }

	public abstract void trySolve(Unify.Solution solution);

	public abstract void forEachType(Consumer<Type> type);

	public abstract C transformTypes(Function<? super Type, ? extends Type> substitution);

	public final static class Equivalence extends Constraint<Equivalence> {
		private final Type to;
		private final Type from;

		public Equivalence(Type to, Type from) {
			this.to = to;
			this.from = from;
		}

		@Override public void trySolve(Unify.Solution solution) {
			Unify.unify(solution, to, from);
		}

		@Override public void forEachType(Consumer<Type> consumer) {
			consumer.accept(to);
			consumer.accept(from);
		}

		@Override public Equivalence transformTypes(Function<? super Type, ? extends Type> substitution) {
			var eq = new Equivalence(to.transform(substitution), from.transform(substitution));
			eq.originProductionIndex = originProductionIndex;
			eq.where = where;
			return eq;
		}

		@Override public String toString() {
			return to + " == " + from;
		}
	}
}
