package io.immutables.meta;

import java.lang.annotation.ElementType;
import javax.annotation.CheckReturnValue;
import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.When;

@CheckReturnValue(when = When.ALWAYS)
@TypeQualifierDefault(ElementType.METHOD)
public @interface CheckReturnByDefault {}
