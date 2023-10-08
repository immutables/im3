
package io.immutables.declaration.constrain;

import java.util.List;

public class ConstraintsViolated extends RuntimeException {
	private final Object partial;
	private final List<Violation> violations;

	ConstraintsViolated(Object partial, List<Violation> violations) {
		this.partial = partial;
		this.violations = List.copyOf(violations);
	}

	public Object getPartial() {
		return partial;
	}

	public List<Violation> getViolations() {
		return violations;
	}
}
