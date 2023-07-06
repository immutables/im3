package io.immutables.declaration.constrain;

public @interface Size {
	int value() default Integer.MIN_VALUE;
	int min() default Integer.MIN_VALUE;
	int max() default Integer.MAX_VALUE;
}
