package io.immutables.codec.record;

import io.immutables.meta.Null;
import java.lang.reflect.Member;
import java.lang.reflect.RecordComponent;

public class DefaultMetadataProvider implements MetadataProvider {
	public int priority() {
		return Integer.MIN_VALUE;
	}

	public boolean isInlineRecord(Class<?> record) {
		return record.isAnnotationPresent(Inline.class);
	}

	public boolean isInlineComponent(RecordComponent component) {
		return component.isAnnotationPresent(Inline.class);
	}

	public boolean isNullableComponent(RecordComponent component) {
		return component.isAnnotationPresent(Null.class);
	}

	public @Null Member findReflectiveDefault(Class<?> type) {
		return null;
	}

	public <E extends Enum<E>> @Null E findDefaultConstant(Class<E> type) {
		if (!type.isEnum()) throw new IllegalArgumentException("Not enum type: " + type);

		for (var f : type.getDeclaredFields()) {
			if (f.isEnumConstant() && f.isAnnotationPresent(Default.class)) {
				var n = f.getName();
				for (var c : type.getEnumConstants()) {
					if (c.name().equals(n)) return c;
				}
				break;
			}
		}
		return null;
	}
}
