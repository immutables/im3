package io.immutables.declaration.http;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PUT {
	/** Extension path, can contain path & query parameters. */
	String value() default "";
}
