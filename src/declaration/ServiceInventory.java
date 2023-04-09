package io.immutables.declaration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marks package that contains API types and/or service interfaces.
 */
@Target(ElementType.PACKAGE)
public @interface ServiceInventory {}
