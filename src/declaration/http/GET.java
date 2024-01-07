package dev.declaration.http;

import java.lang.annotation.*;

/**
 * Annotates contract operation with HTTP GET method.
 */
@Documented
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GET {
	/** Extension path, can contain path and query parameters. */
	String value() default "";
}
