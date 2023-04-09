package io.immutables.codec.builtin;

import io.immutables.codec.Codec;
import io.immutables.codec.In;
import io.immutables.codec.Medium;
import io.immutables.codec.Out;
import io.immutables.meta.Null;
import java.lang.reflect.Type;

public class CollectionCodecs implements Codec.Factory<In, Out> {
	public @Null Codec<?, In, Out> tryCreate(Type type, Medium<In, Out> medium) {
		return null;
	}
}
