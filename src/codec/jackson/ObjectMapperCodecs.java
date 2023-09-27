package io.immutables.codec.jackson;

import io.immutables.codec.Codec;
import io.immutables.codec.In;
import io.immutables.codec.Medium;
import io.immutables.codec.Out;
import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.Type;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Codec that delegates to {@link ObjectMapper}.
 * It usually maps a broad spectrum of types, so maybe it's a good idea to keep it lower
 * priority codec factory or a fallback one. It also fully expects that {@link In}
 * and {@link Out} will be {@link JsonParserIn} and {@link JsonGeneratorOut} respectively.
 */
public class ObjectMapperCodecs implements Codec.Factory<In, Out> {
	private final ObjectMapper objectMapper;

	public ObjectMapperCodecs(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public @Null Codec<?, In, Out> tryCreate(
		Type type, Class<?> raw,
		Medium<? extends In, ? extends Out> medium,
		Codec.Lookup<In, Out> lookup) {

		JavaType javaType = objectMapper.constructType(type);

		boolean cannotRead = !objectMapper.canDeserialize(javaType);
		boolean cannotWrite = !objectMapper.canSerialize(javaType.getRawClass());

		if (cannotRead && cannotWrite) return null;

		var reader = objectMapper.readerFor(javaType);
		var writer = objectMapper.writerFor(javaType);

		return new Codec<>() {
			public @Null Object decode(In in) throws IOException {
				if (!(in instanceof JsonParserIn)) {
					throw new ClassCastException("In is not based on Jackson parser");
				}
				var parser = ((JsonParserIn) in).parser;
				if (cannotRead) {
					throw new JsonMappingException(parser, "Cannot read instance of " + javaType);
				}
				return reader.readValue(parser, javaType);
			}

			public void encode(Out out, Object instance) throws IOException {
				if (!(out instanceof JsonGeneratorOut)) {
					throw new ClassCastException("Out is not based on Jackson generator");
				}
				var generator = ((JsonGeneratorOut) out).generator;
				if (cannotWrite) {
					throw new JsonMappingException(generator, "Cannot write instance of " + javaType);
				}
				writer.writeValue(generator, instance);
			}

			public String toString() {
				return ObjectMapperCodecs.class.getSimpleName() + "[" + javaType + "]";
			}
		};
	}
}
