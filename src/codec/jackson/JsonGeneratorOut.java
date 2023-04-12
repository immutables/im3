package io.immutables.codec.jackson;

import io.immutables.codec.NameIndex;
import io.immutables.codec.Out;
import java.io.IOException;
import java.util.Arrays;
import com.fasterxml.jackson.core.JsonGenerator;

public class JsonGeneratorOut extends Out {

	private final JsonGenerator out;

	public JsonGeneratorOut(JsonGenerator out) {
		this.out = out;
	}

	public void putNull() throws IOException {
		out.writeNull();
	}

	public void putInt(int i) throws IOException {
		out.writeNumber(i);
	}

	public void putLong(long l) throws IOException {
		out.writeNumber(l);
	}

	public void putDouble(double d) throws IOException {
		out.writeNumber(d);
	}

	public void putBoolean(boolean b) throws IOException {
		out.writeBoolean(b);
	}

	public void putString(String s) throws IOException {
		out.writeString(s);
	}

	public void putChars(char[] chars, int offset, int length) throws IOException {
		out.writeString(chars, offset, length);
	}

	public void putString(NameIndex names, int index) throws IOException {
		out.writeString(((JsonNameIndex) names).serialized[index]);
	}

	public void beginArray() throws IOException {
		out.writeStartArray();
	}

	public void endArray() throws IOException {
		out.writeEndArray();
	}

	public void beginStruct(NameIndex names) throws IOException {
		if (++framePointer > frames.length) {
			frames = Arrays.copyOf(frames, frames.length * 2);
		}
		frames[framePointer] = (JsonNameIndex) names;

		out.writeStartObject();
	}

	public void putField(int index) throws IOException {
		var f = frames[framePointer];
		out.writeFieldName(f.serialized[index]);
	}

	public void putField(String name) throws IOException {
		out.writeFieldName(name);
	}

	public void endStruct() throws IOException {
		frames[framePointer--] = null;

		out.writeEndObject();
	}

	public NameIndex index(String... known) {
		return new JsonNameIndex(known);
	}

	private int framePointer = -1;
	private JsonNameIndex[] frames = new JsonNameIndex[10];
}
