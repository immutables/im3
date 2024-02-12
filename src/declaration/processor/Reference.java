package dev.declaration.processor;

/**
 * Symbolic reference to a Declaration, which needs to be resolved/de-referenced
 * to a real declaration. We use it to marshall and pass declarations around by-reference.
 */
// TODO Move to top level
public record Reference(
	String module,
	String name) {}
