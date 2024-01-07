package dev.declaration.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

class Imports {
	private Imports() {}

	static Set<String> importedModules(
		String fromModule,
		Collection<? extends Declaration> declarations) {

		var imported = new HashSet<String>();

		for (var d : declarations) {
			traverseTypes(d, type ->
				traverseReferences(type, r -> {
					var m = r.module();
					if (!fromModule.equals(m)) imported.add(m);
				}));
		}

		return Set.copyOf(imported);
	}

	static void traverseTypes(Declaration declaration, Consumer<Type> consumer) {
		// can switch to pattern matching in Java 21
		if (declaration instanceof Declaration.Contract c) {
			for (var o : c.operations().values()) {
				consumer.accept(o.returns().type());
				for (var p : o.parameters()) {
					consumer.accept(p.type());
				}
				for (var t : o.thrown()) {
					consumer.accept(t.type());
				}
			}
		} else if (declaration instanceof Declaration.Record r) {
			for (var c : r.components()) {
				consumer.accept(c.type());
			}
		} else if (declaration instanceof Declaration.Product p) {
			for (var c : p.components()) {
				consumer.accept(c.type());
			}
		} else if (declaration instanceof Declaration.Inline n) {
			consumer.accept(n.component().type());
		} else if (declaration instanceof Declaration.Sealed p) {
			// do not visit cases? expect them to occur in declarations
		} else if (declaration instanceof Declaration.Enum e) {
			// not applicable to enum
		}
	}

	static void traverseReferences(Type type, Consumer<Declaration.Reference> consumer) {
		if (type instanceof Type.Terminal t) {
			consumer.accept(t.terminal());
		} else if (type instanceof Type.Applied a) {
			consumer.accept(a.applies());
			for (var arg : a.arguments()) {
				traverseReferences(arg, consumer);
			}
		} else if (type instanceof Type.Container c) {
			traverseReferences(c.element(), consumer);
		}
	}
}
