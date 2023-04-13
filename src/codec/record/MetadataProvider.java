package io.immutables.codec.record;

import io.immutables.meta.Null;
import java.lang.reflect.Member;
import java.lang.reflect.RecordComponent;

public interface MetadataProvider {
	// bigger priority value wins
	int priority();

	boolean isInlineRecord(Class<?> record);

	boolean isInlineComponent(RecordComponent component);

	boolean isNullableComponent(RecordComponent component);

	/**
	 * Tries to find a reflective accessor for a default value.
	 * A {@link java.lang.reflect.Constructor} for subclass no-arg constructor.
	 * A {@link java.lang.reflect.Field} for a static field (and enum constant),
	 * a {@link java.lang.reflect.Method} for a no-arg static method to get a default instance.
	 * Or {@code null} if no such default exists.
	 */
	@Null Member findReflectiveDefault(Class<?> type);

	<E extends Enum<E>> @Null E findDefaultConstant(Class<E> type);
}
