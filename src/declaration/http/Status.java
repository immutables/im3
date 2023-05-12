package io.immutables.declaration.http;

import java.lang.annotation.*;

/**
 * Annotate exceptions and custom return values with specific HTTP status codes, i.e.
 * all those 2xx, 3xx, 4xx, 5xx codes.
 * Provided values are used to create responses as well as bind exceptions and response
 * objects when reading. Can also be used to annotate
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Status {
	/** HTTP status code value. */
	int value();
	/**
	 * If set, suggests custom status text to be returned alongside response status code.
	 * Implementations are encouraged to follow this hint and return custom text if it's
	 * specified, but it's not strictly required and not enforced.
	 */
	String text() default "";
}
