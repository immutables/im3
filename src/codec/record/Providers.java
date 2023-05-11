package io.immutables.codec.record;

import java.util.Comparator;
import java.util.ServiceLoader;

// TODO support couple of providers at the same time,
// maybe by a composite provider iterating over may actual providers
public final class Providers {
	private Providers() {}

	private static final MetadataProvider metadata;
	static {
		metadata = ServiceLoader.load(MetadataProvider.class).stream()
			.map(ServiceLoader.Provider::get)
			.max(Comparator.comparingInt(MetadataProvider::priority))
			.orElseThrow(() -> new AssertionError("At least default provider required"));
	}

	public static MetadataProvider metadata() {
		return metadata;
	}
}
