package io.immutables.codec;

import java.lang.reflect.GenericDeclaration;
import java.util.List;

public sealed interface TypeUse {

	TypeUse substitute(Variable variable, TypeUse type);

	final class Variable implements TypeUse {
		public final String name;

		public Variable(String name) {
			this.name = name;
		}

		public TypeUse substitute(Variable variable, TypeUse type) {
			return variable == this ? type : this;
		}
	}

	record Terminal(Class<?> type) implements TypeUse {
		public Terminal {
			requireParametersLength(type, 0);
		}

		public TypeUse substitute(Variable variable, TypeUse type) {
			return this;
		}
	}

	record Applied(Class<?> type, List<TypeUse> arguments) implements TypeUse {
		public Applied {
			arguments = List.copyOf(arguments);
			requireParametersLength(type, arguments.size());
		}

		public TypeUse substitute(Variable variable, TypeUse replacement) {
			for (int i = 0; i < arguments.size(); i++) {
				if (arguments.get(i) == variable) {
					TypeUse[] copy = arguments.toArray(new TypeUse[0]);
					copy[i++] = replacement;
					for (; i < copy.length; i++) {
						if (copy[i] == variable) copy[i] = replacement;
					}
					return new Applied(type, List.of(copy));
				}
			}
			return this;
		}
	}

	static Applied apply(Class<?> type, TypeUse a0) {
		return new Applied(type, List.of(a0));
	}

	static Applied apply(Class<?> type, TypeUse a0, TypeUse a1) {
		return new Applied(type, List.of(a0, a1));
	}

	private static void requireParametersLength(GenericDeclaration declaration, int length) {
		if (declaration.getTypeParameters().length != length) {
			throw new IllegalArgumentException(
				"%s expected to have %d type parameters".formatted(declaration, length));
		}
	}
}
