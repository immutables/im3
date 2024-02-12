package dev.declaration.module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies external java types to be accepted as opaque scalar (inline) value
 * that can be used as is, as record components or elements of the collection. Usually
 * inline value types are being declarated as inline record having a single component, its value.
 * Vetted types are all other non-record types, which somehow expected to be propertly marshalled
 * having proper codec enabled. In the generated API spec, these types can only be properly
 * described if there's a mapping to a primitive type, as there's no such mechanism at the moment,
 * these might be defaulted to a string, or any json type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface Vetted {
	Class<?> value();
}
