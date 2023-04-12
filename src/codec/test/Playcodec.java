package io.immutables.codec.test;

import io.immutables.codec.*;
import io.immutables.codec.jackson.JsonGeneratorOut;
import io.immutables.codec.jackson.JsonParserIn;
import io.immutables.codec.record.RecordsFactory;
import io.immutables.meta.Null;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.junit.Test;

public class Playcodec {
/*	static class Base<X> {
		final Type type;

		Base() {
			assert this.getClass().getSuperclass() == Base.class;
			Type superclass = this.getClass().getGenericSuperclass();
			assert superclass instanceof ParameterizedType;
			type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
		}
	}*/

/*	public static void main(String[] args) {
		var t = new Base<Map<String, Object>>() {}.type;

		var t2 = new Base<Map<String, Object>>() {}.type;

		extracted();
	}*/

	/*
	private static <T> void extracted() {
		var t1 = (TypeVariable<?>) new Base<T>() {}.type;

		var t3 = (TypeVariable<?>) new Base<T>() {}.type;

		System.out.println(t1);
		System.out.println(t3);
		System.out.println(t1.equals(t3));
		System.out.println(t1.equals(TypeToken.of(t3).getType()));
	}*/

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

	static final JsonFactory factory = new JsonFactoryBuilder()
		.enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
		.disable(JsonWriteFeature.QUOTE_FIELD_NAMES)
		.build();

	public record Bui(
		Xul<String, Xul<Integer, Integer>> xui,
		int lilu,
		boolean bull,
		double[] ddd
	) {}

	public record Xul<E, H>(E a, H b) {}

	public static void main(String[] args) throws IOException {

		Registry registry = new Registry.Builder()
			.add(new RecordsFactory())
			.build();

		StringWriter w = new StringWriter();
		JsonGenerator g = factory.createGenerator(w);

		DefaultPrettyPrinter printer = new DefaultPrettyPrinter() {
			{
				_objectFieldValueSeparatorWithSpaces = ": ";
			}
		};

		g.setPrettyPrinter(printer);

		JsonGeneratorOut out = new JsonGeneratorOut(g);

		Type xui = Bui.class.getRecordComponents()[0].getGenericType();

		var codec = registry.resolve(Bui.class, Medium.Json).orElseThrow();
		var codec2 = registry.resolve(xui, Medium.Json).orElseThrow();

		codec.encode(out,
			new Bui(
				new Xul<>("Abc",
					new Xul<>(678, 93)
				), 123, true, new double[]{1.1, 4.2, 3.5, 2.1}));

		g.flush();
		System.out.println(w);

		JsonParser p = factory.createParser(w.toString());
		JsonParserIn in = new JsonParserIn(p);

		Bui bui = codec.decode(in);
		System.out.println(bui);

		in = new JsonParserIn(factory.createParser(w.toString()));

		@Null In.Buffer buffer = null;
		in.beginStruct(in.index());
		while (in.hasNext()) {
			in.takeField();
			if (in.name().equals("xui")) {
				buffer = in.takeBuffer();
			} else {
				System.out.println(in.name() + ": " + in.takeString());
			}
		}
		in.endStruct();

		if (buffer != null) {

			var xui2 = codec2.decode(buffer.in());

			System.out.println(xui2);

			xui2 = codec2.decode(buffer.in());

			System.out.println(xui2);

			xui2 = codec2.decode(buffer.in());

			System.out.println(xui2);
		}
	}
}
