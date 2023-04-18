package io.immutables.codec.jackson;

import io.immutables.codec.In;
import io.immutables.codec.NameIndex;
import java.io.IOException;
import java.util.Arrays;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import static com.fasterxml.jackson.core.JsonTokenId.*;

public final class JsonParserIn extends In {

	private final JsonParser parser;

	public JsonParserIn(JsonParser parser) {
		this.parser = parser;
	}

	private int peeked = ID_NO_TOKEN;

	public At peek() throws IOException {
		return atToken(ensurePeeked());
	}

	private int ensurePeeked() throws IOException {
		if (peeked == ID_NO_TOKEN) {
			var t = parser.nextToken();
			peeked = t != null ? t.id() : ID_NO_TOKEN;
		}
		return peeked;
	}

	private static At atToken(int token) {
		return switch (token) {
			case ID_NULL -> At.Null;
			case ID_TRUE -> At.True;
			case ID_FALSE -> At.False;
			case ID_NUMBER_FLOAT -> At.Float;
			case ID_NUMBER_INT -> At.Int;
			case ID_STRING -> At.String;
			case ID_FIELD_NAME -> At.Field;
			case ID_START_OBJECT -> At.Struct;
			case ID_END_OBJECT -> At.StructEnd;
			case ID_START_ARRAY -> At.Array;
			case ID_END_ARRAY -> At.ArrayEnd;
			default -> At.Nope;
		};
	}

	public int takeInt() throws IOException {
		ensurePeeked();
		int i = parser.getIntValue();
		peeked = ID_NO_TOKEN;
		return i;
	}

	public long takeLong() throws IOException {
		ensurePeeked();
		long l = parser.getLongValue();
		peeked = ID_NO_TOKEN;
		return l;
	}

	public double takeDouble() throws IOException {
		ensurePeeked();
		double d = parser.getDoubleValue();
		peeked = ID_NO_TOKEN;
		return d;
	}

	public boolean takeBoolean() throws IOException {
		ensurePeeked();
		boolean b = parser.getBooleanValue();
		peeked = ID_NO_TOKEN;
		return b;
	}

	public void takeNull() throws IOException {
		ensurePeeked();
		if (peeked != ID_NULL) throw unexpected(
			"Expected `null`, but was %s", parser.getCurrentToken());
		peeked = ID_NO_TOKEN;
	}

	private IOException unexpected(String template, Object... args) {
		return new JsonParseException(parser, template.formatted(args));
	}

	public String takeString() throws IOException {
		ensurePeeked();
		var s = parser.getValueAsString();
		peeked = ID_NO_TOKEN;
		return s;
	}

	public int takeString(NameIndex names) throws IOException {
		ensurePeeked();
		if (peeked != ID_STRING) throw unexpected(
			"Name is supposed to be a string, but was %s", parser.getCurrentToken());

		int i = names.index(parser.getText());
		peeked = ID_NO_TOKEN;
		return i;
	}

	public int takeField() throws IOException {
		ensurePeeked();
		if (peeked != ID_FIELD_NAME) throw unexpected(
			"Field name expected but was, but was %s", parser.getCurrentToken());

		int i = frames[framePointer].index(parser.getCurrentName());
		peeked = ID_NO_TOKEN;
		return i;
	}

	public void skip() throws IOException {
		ensurePeeked();
		parser.skipChildren();
		peeked = ID_NO_TOKEN;
	}

	public boolean hasNext() throws IOException {
		ensurePeeked();
		// not clearing peeked !
		return peeked != ID_END_ARRAY && peeked != ID_END_OBJECT;
	}

	public void beginArray() throws IOException {
		ensurePeeked();
		if (peeked != ID_START_ARRAY) throw unexpected(
			"Expected '[' but was ", parser.getCurrentToken());
		peeked = ID_NO_TOKEN;
	}

	public void endArray() throws IOException {
		ensurePeeked();
		if (peeked != ID_END_ARRAY) throw unexpected(
			"Expected ']' but was ", parser.getCurrentToken());
		peeked = ID_NO_TOKEN;
	}

	public void beginStruct(NameIndex names) throws IOException {
		if (++framePointer > frames.length) {
			frames = Arrays.copyOf(frames, frames.length * 2);
		}
		frames[framePointer] = (JsonNameIndex) names;
		ensurePeeked();
		if (peeked != ID_START_OBJECT) throw unexpected(
			"Expected '{' but was ", parser.getCurrentToken());
		peeked = ID_NO_TOKEN;
	}

	public void endStruct() throws IOException {
		ensurePeeked();
		frames[framePointer--] = null;
		if (peeked != ID_END_OBJECT) throw unexpected(
			"Expected '}' but was ", parser.getCurrentToken());
		peeked = ID_NO_TOKEN;
	}

	public NameIndex index(String... known) {
		return new JsonNameIndex(known);
	}

	public String name() throws IOException {
		return parser.getText();
	}

	public Buffer takeBuffer() throws IOException {
		ensurePeeked();
		TokenBuffer buffer = new TokenBuffer(parser);
		buffer.copyCurrentStructure(parser);
		peeked = ID_NO_TOKEN;
		return new JsonBuffer(buffer);
	}

	private static class JsonBuffer extends Buffer {
		private final TokenBuffer buffer;

		public JsonBuffer(TokenBuffer buffer) {
			this.buffer = buffer;
		}

		public In in() {
			return new JsonParserIn(buffer.asParser());
		}
	}

	private int framePointer = -1;
	private JsonNameIndex[] frames = new JsonNameIndex[8];

	public String path() throws IOException {
		var path = new StringBuilder();
		for (var c = parser.getParsingContext(); c != null; c = c.getParent()) {
			// prepending text to path at 0 index, in reverse
			if (c.inObject()) {
				path.insert(0, c.getCurrentName())
					.insert(0, '.');
			} else if (c.inArray()) {
				path.insert(0, ']')
					.insert(0, c.getCurrentIndex())
					.insert(0, '[');
			} else if (c.inRoot()) break;
		}
		return path.insert(0, "$").toString();
	}
}
