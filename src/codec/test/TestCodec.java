package io.immutables.codec.test;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.google.common.reflect.TypeToken;
import org.junit.Test;

public class TestCodec {
	static class Base<X> {
		final Type type;

		Base() {
			assert this.getClass().getSuperclass() == Base.class;
			Type superclass = this.getClass().getGenericSuperclass();
			assert superclass instanceof ParameterizedType;
			type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
		}
	}

/*	public static void main(String[] args) {
		var t = new Base<Map<String, Object>>() {}.type;

		var t2 = new Base<Map<String, Object>>() {}.type;

		extracted();
	}*/

	private static <T> void extracted() {
		var t1 = (TypeVariable<?>) new Base<T>() {}.type;

		var t3 = (TypeVariable<?>) new Base<T>() {}.type;

		System.out.println(t1);
		System.out.println(t3);
		System.out.println(t1.equals(t3));
		System.out.println(t1.equals(TypeToken.of(t3).getType()));
	}

	@Test public void output() throws IOException {

		JsonFactory factory = new JsonFactoryBuilder()
			.enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
			.disable(JsonWriteFeature.QUOTE_FIELD_NAMES)
			.quoteChar('\'')
			.build();

		StringWriter w = new StringWriter();

		JsonParser p = factory.createParser("{abc: 'A'}".replace('\'', '"'));
		JsonGenerator g = factory.createGenerator(w);

		JsonToken t = p.nextToken();
		assert t == JsonToken.START_OBJECT;
		t = p.nextToken();
		assert t == JsonToken.FIELD_NAME;

		char[] chars = p.getTextCharacters();
		int offset = p.getTextOffset();
		int length = p.getTextLength();

		System.out.println(String.valueOf(chars, offset, length));
	}

	public static void main(String[] args) {



	}
}
