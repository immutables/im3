package dev.declaration.processor;

import java.util.List;

public record Module(
	String name,
	List<Declaration> declarations,
	List<String> comment
) implements Documented {}
