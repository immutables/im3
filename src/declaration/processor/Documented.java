package dev.declaration.processor;

import java.util.List;

/**
 * Element which can have documentation comment. Even if implemented,
 * the comment can be empty (either actually empty or not documented), for the model,
 * these too are equivalent.
 */
// cannot be nested into declaration
public interface Documented {
	List<String> comment();
}
