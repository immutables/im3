package io.immutables.stencil.template;

import io.immutables.meta.Null;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class LocalScope {
	final String name;

	private final @Null LocalScope parent;

	final Set<String> locals = new HashSet<>();
	final Map<String, String> substitutions = new HashMap<>();

	LocalScope(@Null LocalScope parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	@Null LocalScope parent() {
		return parent;
	}

	boolean declare(String local) {
		return locals.add(local);
	}

	@Null String substitutionSkipping(String key, int skipLevels) {
		for (@Null var scope = this; scope != null; scope = scope.parent) {
			@Null var substitution = scope.substitutions.get(key);
			if (substitution != null) {
				if (skipLevels-- == 0) {
					return substitution;
				}
			}
		}
		return null;
	}

	boolean hasLocal(String local) {
		for (@Null var g = this; g != null; g = g.parent) {
			if (g.locals.contains(local)) {
				return true;
			}
		}
		return false;
	}

	LocalScope extend(String name) {
		return new LocalScope(this, name);
	}

	LocalScope extendLocal(String name) {
		var d = extend(name);
		d.declare(name);
		return d;
	}
}
