package io.immutables.codec.record;

import io.immutables.meta.Null;
import java.lang.reflect.Member;
import java.lang.reflect.RecordComponent;
import java.util.ServiceLoader;

// TODO support couple of providers at the same time,
// maybe by a composite provider iterating over many actual providers
final class Providers {
	private Providers() {}

	private static final MetadataProvider metadata;
	static {
		metadata = new CompoundMetadataProvider(
			ServiceLoader.load(MetadataProvider.class).stream()
				.map(ServiceLoader.Provider::get)
				.toList());
	}
	public static MetadataProvider metadata() {
		return metadata;
	}

	private static class CompoundMetadataProvider implements MetadataProvider {
		@Override public int priority() {
			return 0;
		}

		@Override public boolean isInlineRecord(Class<?> record) {
			return false;
		}

		@Override public boolean isInlineComponent(RecordComponent component) {
			return false;
		}

		@Override public boolean isNullableComponent(RecordComponent component) {
			return false;
		}

		@Null @Override public CaseTag findCaseTag(Class<?> record, @Null Class<?> sealed) {
			return null;
		}

		@Null @Override public Member findReflectiveDefault(Class<?> type) {
			return null;
		}

		@Override public <E extends Enum<E>> @Null E findDefaultConstant(Class<E> type) {
			return null;
		}
	}
}
