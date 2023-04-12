package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

final class CollectionCodecs implements Codec.Factory<In, Out> {
	@Null
	public Codec<?, In, Out> tryCreate(Type type, Class<?> raw,
		Medium<? extends In, ? extends Out> medium, Codec.Lookup<In, Out> lookup) {
		if (raw.isArray()) {
			Class<?> componentType = raw.getComponentType();
			return new ArrayCodec(componentType, lookup.get(componentType));
		}
		if (raw == List.class) {

		}
		if (raw == Set.class) {

		}
		if (raw.isAssignableFrom(Collection.class)) {

		}
		return null;
	}

	private static abstract class CollectionCodec extends Codec<Object, In, Out> {

	}

	private static final class ArrayCodec extends Codec<Object, In, Out> {
		private final Class<?> componentType;
		private final Codec<Object, In, Out> componentCodec;

		ArrayCodec(Class<?> componentType, Codec<Object, In, Out> componentCodec) {
			this.componentType = componentType;
			this.componentCodec = componentCodec;
		}

		public void encode(Out out, Object instance) throws IOException {
			out.beginArray();

			int length = Array.getLength(instance);
			for (int i = 0; i < length; i++) {
				componentCodec.encode(out, Array.get(instance, i));
			}

			out.endArray();
		}

		public @Null Object decode(In in) throws IOException {
			in.beginArray();

			var buffer = new ArrayList<>();
			while (in.hasNext()) {
				buffer.add(componentCodec.decode(in));
			}
			in.endArray();

			Object instance = Array.newInstance(componentType, buffer.size());
			for (int i = 0; i < buffer.size(); i++) {
				Array.set(instance, i, buffer.get(i));
			}
			return instance;
		}

		public Object defaultInstance() {
			return Array.newInstance(componentType, 0);
		}
	}
}
