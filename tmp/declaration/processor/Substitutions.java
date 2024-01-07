package io.immutables.declaration.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class Substitutions {
	private Substitutions() {}

	public static Type substitute(Type original, Function<Type, Type> fn) {
		var substitute = fn.apply(original);
		if (substitute != original) return substitute;
		if (original instanceof Type.Container c) {
			return substituteElement(c, fn);
		}
		if (original instanceof Type.Applied a) {
			return substituteArguments(a, fn);
		}
		return original;
	}

	private static Type.Container substituteElement(Type.Container original, Function<Type, Type> fn) {
		var substitutedElement = fn.apply(original.element());
		if (substitutedElement != original.element()) {
			return new Type.Container(original.container(), substitutedElement);
		}
		// preserve original if nothing was changed
		return original;
	}

	private static Type.Applied substituteArguments(Type.Applied original, Function<Type, Type> fn) {
		var replacementArguments = new ArrayList<Type>(original.arguments().size());
		boolean wasAnySubstituted = false;
		for (var originalArgument : original.arguments()) {
			var substitutedArgument = fn.apply(originalArgument);
			replacementArguments.add(substitutedArgument);
			if (substitutedArgument != originalArgument) {
				wasAnySubstituted = true;
			}
		}
		if (wasAnySubstituted) {
			return new Type.Applied(original.applies(), replacementArguments);
		}
		// preserve original if nothing was changed
		return original;
	}

	private static Declaration.Component substituteIn(
			Declaration.Component c, Function<Type, Type> fn) {
		return new Declaration.Component(
				c.name(),
				substitute(c.type(), fn),
				c.javaType(),
				c.comment());
	}

	interface SyntheticReferenceGenerator {
		Declaration.Reference forApplied(Declaration.Reference reference, List<Type> arguments);
	}

	public static Function<Type, Type> specialization(
			SyntheticReferenceGenerator referenceGenerator,
			Function<Declaration.Reference, Declaration.Datatype> resolver,
			Map<Declaration.Reference, Declaration.Datatype> syntheticDeclarations) {

		// Here we pack routines and enjoy access to the method parameters as a capture
		// to make co-recursive calls easier
		class Specializer {
			Declaration.Datatype specialize(
					Declaration.Reference reference,
					Declaration.Datatype genericDeclaration,
					List<Type> arguments) {
				assert genericDeclaration instanceof Declaration.Parameterizable;
				var parameters = ((Declaration.Parameterizable) genericDeclaration).parameters();
				var substitutions = appliesArguments(parameters, arguments);

				// cases for inline, record, and sealed (of records)
				// No case for Enum as case is not parameterizable
				if (genericDeclaration instanceof Declaration.Inline inline) {
					return new Declaration.Inline(
							Declaration.Inline.Tag.Is, reference, List.of(),
							substituteIn(inline.component(), substitutions),
							inline.comment());
				}
				if (genericDeclaration instanceof Declaration.Record record) {
					return new Declaration.Record(
							Declaration.Record.Tag.Is, reference, List.of(),
							record.components().stream()
									.map(c -> substituteIn(c, substitutions))
									.toList(),
							record.comment());
				}
				if (genericDeclaration instanceof Declaration.Sealed sealed) {
					/*return new Declaration.Sealed(
							Declaration.Sealed.Tag.Is, reference, List.of(),
							sealed.cases().stream().map(caseRecord -> {
								//specialize().reference();
							}).toList(),
							sealed.comment());*/
				}
				throw new AssertionError("Unsupported declaration %s of type %s"
						.formatted(genericDeclaration.reference(), genericDeclaration.getClass()));
			}
		}

		var specializer = new Specializer();

		return type -> {
			if (type instanceof Type.Applied a) {
				var reference = referenceGenerator.forApplied(a.applies(), a.arguments());
				var genericDeclaration = resolver.apply(a.applies());
				// Compilers proves proper declaration and construction of original
				// declaration must follow, i.e. if we have Type.Applied, we must
				// have parameterized datatype, actually having type parameters
				assert genericDeclaration instanceof Declaration.Parameterizable p
						&& !p.parameters().isEmpty();

				var declaration = syntheticDeclarations.computeIfAbsent(
						reference,
						t -> specializer.specialize(reference, genericDeclaration, a.arguments()));

				return new Type.Terminal(reference);
			}
			return type;
		};
	}

	static Function<Type, Type> appliesArguments(
			List<Type.Variable> parameters, List<Type> arguments) {
		assert parameters.size() == arguments.size();
		var substitutions = new HashMap<Type.Variable, Type>();
		for (int i = 0; i < parameters.size(); i++) {
			substitutions.put(parameters.get(i), arguments.get(i));
		}
		return t -> t instanceof Type.Variable v
				? substitutions.getOrDefault(v, v) : t;
	}
}
