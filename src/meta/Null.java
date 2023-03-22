package io.immutables.meta;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

/**
 * Nullable
 */
@Nonnull(when = When.MAYBE)
@TypeQualifierNickname
public @interface Null {}
