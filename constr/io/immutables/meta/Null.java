package io.immutables.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Nullable annotation with the semantics of {@code @Nonnull(when = When.MAYBE)} using,
 * now discontinued, JSR305 definition. {@code Null} is deliberate shortening of "Nullable", which is arguably too long for regular use in code.
 *
 * <pre><code>
 * {@literal @}Null Object ...
 * // should be read as `null or an Object`
 * </code></pre>
 *
 * This annotation is runtime retained, can be used with reflection.
 * annotation when {@code TYPE_USE} is needed
 * (Need to revisit in general if we can use unified annotation).
 */
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
