package io.immutables.codec.jackson;

import io.immutables.codec.NameIndex;
import io.immutables.codec.Out;
import java.io.IOException;
import java.util.Arrays;
import com.fasterxml.jackson.core.JsonGenerator;

public final class JsonGeneratorOut extends Out {
	final JsonGenerator generator;

	public JsonGeneratorOut(JsonGenerator generator) {
		this.generator = generator;
	}

	public void putNull() throws IOException {
		generator.writeNull();
	}

	public void putInt(int i) throws IOException {
		generator.writeNumber(i);
	}

	public void putLong(long l) throws IOException {
		generator.writeNumber(l);
	}

	public void putDouble(double d) throws IOException {
		generator.writeNumber(d);
	}

	public void putBoolean(boolean b) throws IOException {
		generator.writeBoolean(b);
	}

	public void putString(String s) throws IOException {
		generator.writeString(s);
	}

	public void putString(char[] chars, int offset, int length) throws IOException {
		generator.writeString(chars, offset, length);
	}

	public void putString(NameIndex names, int index) throws IOException {
		generator.writeString(((JsonNameIndex) names).serialized[index]);
	}

	public void beginArray() throws IOException {
		generator.writeStartArray();
	}

	public void endArray() throws IOException {
		generator.writeEndArray();
	}

	public void beginStruct(NameIndex names) throws IOException {
		if (++framePointer > frames.length) {
			frames = Arrays.copyOf(frames, frames.length * 2);
		}
		frames[framePointer] = (JsonNameIndex) names;

		generator.writeStartObject();
	}

	public void putField(int index) throws IOException {
		var f = frames[framePointer];
		generator.writeFieldName(f.serialized[index]);
	}

	public void putField(String name) throws IOException {
		generator.writeFieldName(name);
	}

	public void endStruct() throws IOException {
		frames[framePointer--] = null;

		generator.writeEndObject();
	}

	public NameIndex index(String... known) {
		return new JsonNameIndex(known);
	}

	private int framePointer = -1;
	private JsonNameIndex[] frames = new JsonNameIndex[10];
}
