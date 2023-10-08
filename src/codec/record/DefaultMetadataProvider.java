package io.immutables.codec.record;

import io.immutables.meta.Default;
import io.immutables.meta.Inline;
import io.immutables.meta.Null;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.RecordComponent;

public class DefaultMetadataProvider implements MetadataProvider {
	@Override public int priority() {
		return Integer.MIN_VALUE;
	}

	@Override public boolean isInlineRecord(Class<?> record) {
		return record.isRecord() && record.isAnnotationPresent(Inline.class);
	}

	@Override public boolean isInlineComponent(RecordComponent component) {
		return component.isAnnotationPresent(Inline.class);
	}

	@Override public boolean isNullableComponent(RecordComponent component) {
		return component.isAnnotationPresent(Null.class);
	}

	// TODO big? bad metadata reporter ? or just logging
	@Override public @Null CaseTag findCaseTag(Class<?> record, @Null Class<?> sealed) {
		if (record.isRecord() && (sealed == null || sealed.isSealed())) {
			@Null Tagged taggedOnRecord = record.getAnnotation(Tagged.class);
			@Null Tagged taggedOnSealed = null;

			sealed = getUnambiguousSealed(record, sealed);
			if (sealed != null) {
				taggedOnSealed = sealed.getAnnotation(Tagged.class);
			}

			if (taggedOnSealed != null) {
				if (!taggedOnSealed.value().equals(Tagged.AUTO)) {
					throw new AssertionError(("@Tagged on %s must not specify value, " +
							"only annotation on cases can do this").formatted(sealed));
				}
			}

			if (taggedOnRecord != null && taggedOnSealed != null) {
				var field = taggedOnRecord.field();
				if (field.equals(Tagged.AUTO_FIELD)) {
					// in case sealed provides field name
					field = taggedOnSealed.field();
				}
				// value from taggedOnSealed is irrelevant, and must be AUTO
				var value = taggedOnRecord.value();
				if (value.equals(Tagged.AUTO)) {
					value = record.getSimpleName();
				}
				return new CaseTag(field, value);
			}

			if (taggedOnRecord != null) {
				var field = taggedOnRecord.field();
				var value = taggedOnRecord.value();
				if (value.equals(Tagged.AUTO)) {
					value = record.getSimpleName();
				}
				return new CaseTag(field, value);
			}

			if (taggedOnSealed != null) {
				var field = taggedOnSealed.field();
				var value = record.getSimpleName();
				// value from taggedOnSealed is irrelevant, and must be AUTO
				// but this is not validated here (yet?)
				return new CaseTag(field, value);
			}
		}
		// silently return null/not found if our preconditions failed
		return null;
	}

	private @Null Class<?> getUnambiguousSealed(Class<?> record, @Null Class<?> sealed) {
		if (sealed != null) return sealed;
		// this routine doesn't consider intermediate/superinterfaces
		// just directly implemented interfaces,
		// it's unclear if that ever will need that.
		for (var implemented : record.getInterfaces()) {
			if (implemented.isSealed()) {
				// not a single ambiguous, return null
				if (sealed != null) return null;
				sealed = implemented;
			}
		}
		return sealed;
	}

	@Override public @Null Member findReflectiveDefault(Class<?> type) {
		return null;
	}

	protected boolean isDefaultField(Field f) {
		return f.isAnnotationPresent(Default.class);
	}

	@Override public <E extends Enum<E>> @Null E findDefaultConstant(Class<E> type) {
		if (!type.isEnum()) throw new IllegalArgumentException("Not enum type: " + type);

		for (var f : type.getDeclaredFields()) {
			if (f.isEnumConstant() && isDefaultField(f)) {
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
