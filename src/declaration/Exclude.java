package io.immutables.declaration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * If type is excluded it will not be part of the inventory, or otherwise it will
 * not raise errors if it's unsupported kind of type or just unrelated class definition
 * (which shouldn't be there, but that is what exclude for — to make an exception)
 */
@Target(ElementType.TYPE)
public @interface Exclude {}
