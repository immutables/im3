package io.immutables.codec.record;

import io.immutables.codec.Codec;
import io.immutables.codec.In;
import io.immutables.codec.Medium;
import io.immutables.codec.Out;
import io.immutables.meta.Null;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.ServiceLoader;

public final class RecordsFactory implements Codec.Factory<In, Out> {
	public @Null Codec<?, In, Out> tryCreate(
		Type type,
		Class<?> raw,
		Medium<? extends In, ? extends Out> medium,
		Codec.Lookup<In, Out> lookup) {

		if (raw.isEnum()) {
			return new EnumCodec<>(raw);
		}
		if (raw.isRecord()) {
			if (Providers.metadata().isInlineRecord(raw)) {
				RecordComponent[] components = raw.getRecordComponents();
				if (components.length == 1) {
					return new InlineRecordCodec(type, raw, components[0], lookup);
				}
				return new ProductRecordCodec<>(type, raw, lookup);
			}
			return new RecordCodec<>(type, raw, lookup);
		}
		if (raw.isInterface() && raw.isSealed()) {
			return new SealedInterfaceCodec(type, raw, lookup);
		}
		return null;
	}
}
