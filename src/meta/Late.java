package io.immutables.meta;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use {@code Late} annotation on fields which are considered non-null,
 * but are initialized relatively late (with some init or setter method),
 * but must not be used before it is initialized. This is not checked at compile/lint time,
 * so it just avoids warnings for such field when {@link NonnullByDefault} is used.
 * An alternative would be marking these fields as @{@link Null} and check it
 * (or {@code assert field != null}) to silence the warning.
 */
@Nonnull(when = When.UNKNOWN)
@TypeQualifierNickname
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Late {}
