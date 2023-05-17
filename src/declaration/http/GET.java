package io.immutables.declaration.http;

import java.lang.annotation.*;

@Documented
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GET {
	/** Extension path, can contain path & query parameters. */
	String value() default "";
}
