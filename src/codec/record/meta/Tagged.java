package io.immutables.codec.record.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used on sealed interfaces and case records to provide
 * tagging during serialization/deserialization. Polymorphic mapping can work
 * without tagging, just on the basis of the difference of field set
 * (and for some codecs - in expected value kind). Tagging, is basically,
 * defining addition discriminator field to be written and read along each case.
 * Thus, tagging can be used to either resolve ambiguity or make difference more expressive.
 * any tagging if the differences between cases (fields and types) are expressive enough.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Tagged {
	String field() default AUTO_FIELD;
	String value() default AUTO;

	String AUTO = "\0*\0";
	String AUTO_FIELD = "@case";
}
