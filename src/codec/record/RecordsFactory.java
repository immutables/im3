package io.immutables.codec.record;

import io.immutables.codec.Codec;
import io.immutables.codec.In;
import io.immutables.codec.Medium;
import io.immutables.codec.Out;
import io.immutables.meta.Null;
import java.lang.reflect.Type;

public final class RecordsFactory implements Codec.Factory<In, Out> {
	public @Null Codec<?, In, Out> tryCreate(
		Type type,
		Class<?> raw,
		Medium<? extends In, ? extends Out> medium,
		Codec.Lookup<In, Out> lookup) {

		if (raw.isRecord()) {
			return new RecordCodec<>(type, raw, lookup);
			// TODO inline records
		}
		if (raw.isInterface() && raw.isSealed()) {
			// TODO
		}
		return null;
	}
}
