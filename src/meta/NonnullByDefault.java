package io.immutables.meta;

import java.lang.annotation.ElementType;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.When;

@Nonnull(when = When.ALWAYS)
@TypeQualifierDefault({
	ElementType.PARAMETER,
	ElementType.FIELD,
	ElementType.METHOD,
	ElementType.RECORD_COMPONENT,
	ElementType.LOCAL_VARIABLE,
	ElementType.TYPE_PARAMETER,
})
public @interface NonnullByDefault {}
