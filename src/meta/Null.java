package io.immutables.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

/**
 * Nullable annotation with the semantics of {@code @Nonnull(when = When.MAYBE)} using,
 * now discontinued, JSR305 definition.
 */
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
@Retention(RetentionPolicy.RUNTIME)
public @interface Null {}
