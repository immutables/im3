package io.immutables.declaration.http;

/**
 * Returns annotation is to be implemented by exceptions and other return types to specify actual
 * response body type. In the case of exceptions, which cannot have own generic parameters,
 * even if we know value from the actual parameter of generic {@code T}, we still need to be
 * able to carry this response with the exception, for this you need to extend
 * {@link ReturnException} which has {@link ReturnException#initBody(Object)},
 * {@link ReturnException#getBody()} methods. Runtime tools should enforce that the body object is
 * the instance of {@code T} (including any erased generic arguments). But this cannot be
 * enforced at compile time (since Exceptions cannot have generic parameters).
 * @param <T> Should be specific type that can be marshalled from HTTP response.
 */
public interface Returns<T> {}
