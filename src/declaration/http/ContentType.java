package dev.declaration.http;

import java.lang.annotation.*;

/**
 * Meta annotation to create content type specific annotations. To avoid crazy mixtures,
 * this {@link ContentType} annotation is only allowed to be used as meta annotation,
 * and then, only use your own, or one of the set of pre-defined annotations to specify
 * or override types. See nested annotations for most common content types.
 * @see Json
 * @see Form
 * @see Binary
 * @see Text
 */
// Used by annotation processor and runtime reflective handlers
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Documented
public @interface ContentType {
	/** Mime content type */
	String value();

	@Retention(RetentionPolicy.RUNTIME)
	@Target({
		ElementType.PACKAGE,
		ElementType.PARAMETER,
		ElementType.TYPE,
		ElementType.METHOD
	})
	@Documented
	@ContentType("application/json")
	@interface Json {}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({
		ElementType.PACKAGE,
		ElementType.PARAMETER,
		ElementType.TYPE,
		ElementType.METHOD
	})
	@Documented
	@ContentType("application/x-www-form-urlencoded")
	@interface Form {}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({
		ElementType.PACKAGE,
		ElementType.PARAMETER,
		ElementType.TYPE,
		ElementType.METHOD
	})
	@Documented
	@ContentType("application/octet-stream")
	@interface Binary {}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({
		ElementType.PACKAGE,
		ElementType.PARAMETER,
		ElementType.TYPE,
		ElementType.METHOD
	})
	@Documented
	@ContentType("text/plain")
	@interface Text {}
}
