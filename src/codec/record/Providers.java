package io.immutables.codec.record;

import io.immutables.meta.Null;
import java.lang.reflect.Member;
import java.lang.reflect.RecordComponent;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

final class Providers {
	private Providers() {}

	private static final MetadataProvider metadata;
	static {
		metadata = new CompoundMetadataProvider(
			ServiceLoader.load(MetadataProvider.class).stream()
				.map(ServiceLoader.Provider::get)
				.sorted(Comparator.comparingInt(p -> -p.priority()))
				.toList());
	}

	public static MetadataProvider metadata() {
		return metadata;
	}

	private static class CompoundMetadataProvider implements MetadataProvider {
		private final List<MetadataProvider> providers;

		public CompoundMetadataProvider(List<MetadataProvider> providers) {
			this.providers = providers;
		}

		public int priority() {
			return 0;
		}

		@Override public boolean isInlineRecord(Class<?> record) {
			for (var p : providers) {
				if (p.isInlineRecord(record)) return true;
			}
			return false;
		}

		@Override public boolean isInlineComponent(RecordComponent component) {
			for (var p : providers) {
				if (p.isInlineComponent(component)) return true;
			}
			return false;
		}

		@Override public boolean isNullableComponent(RecordComponent component) {
			for (var p : providers) {
				if (p.isNullableComponent(component)) return true;
			}
			return false;
		}

		@Override public @Null CaseTag findCaseTag(Class<?> record, @Null Class<?> sealed) {
			for (var p : providers) {
				@Null var tag = p.findCaseTag(record, sealed);
				if (tag != null) return tag;
			}
			return null;
		}

		@Override public @Null Member findReflectiveDefault(Class<?> type) {
			for (var p : providers) {
				@Null var member = p.findReflectiveDefault(type);
				if (member != null) return member;
			}
			return null;
		}

		@Override public <E extends Enum<E>> @Null E findDefaultConstant(Class<E> type) {
			for (var p : providers) {
				@Null var value = p.findDefaultConstant(type);
				if (value != null) return value;
			}
			return null;
		}
	}
}
