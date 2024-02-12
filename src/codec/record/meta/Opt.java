package io.immutables.codec.record.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

/**
 * {@code Opt} annotation marks optional fields which have {@code null} as the
 * default (absent) value. Equivalent to most "Nullable" annotations (in particular with
 * semantic of {@code @Nonnull(when = When.MAYBE)} using, now discontinued, JSR305 definition.)
 */
@Retention(RetentionPolicy.RUNTIME)
@Nonnull(when = When.MAYBE)
@TypeQualifierNickname
@Target({
	ElementType.FIELD,
	ElementType.PARAMETER,
	ElementType.LOCAL_VARIABLE,
	ElementType.TYPE_PARAMETER,
	ElementType.METHOD,
	ElementType.RECORD_COMPONENT,
})
public @interface Opt {}
