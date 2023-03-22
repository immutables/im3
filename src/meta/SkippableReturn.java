package io.immutables.meta;

import javax.annotation.CheckReturnValue;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

@CheckReturnValue(when = When.MAYBE)
@TypeQualifierNickname
public @interface SkippableReturn {}
