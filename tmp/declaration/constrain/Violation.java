package io.immutables.declaration.constrain;

public record Violation(
		String constraint,
		String expression
) {}
