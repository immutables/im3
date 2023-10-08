package io.immutables.codec.jackson;

import io.immutables.codec.*;
import io.immutables.meta.Null;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import static com.fasterxml.jackson.core.JsonTokenId.*;

public final class JsonParserIn extends In {
	final JsonParser parser;

	public JsonParserIn(JsonParser parser) {
		this(parser, Problem.ThrowingHandler);
	}

	public JsonParserIn(JsonParser parser, Problem.Handler handler) {
		super(handler);
		this.parser = parser;
	}

	private int peeked = ID_NO_TOKEN;

	private int ensurePeeked() throws IOException {
		if (peeked == ID_NO_TOKEN) {
			var t = parser.nextToken();
			peeked = t != null ? t.id() : ID_NO_TOKEN;
		}
		return peeked;
	}

	private static Token asToken(int token) {
		return switch (token) {
			case ID_NULL -> Token.Null;
			case ID_TRUE -> Token.True;
			case ID_FALSE -> Token.False;
			case ID_NUMBER_FLOAT -> Token.Float;
			case ID_NUMBER_INT -> Token.Int;
			case ID_STRING -> Token.String;
			case ID_FIELD_NAME -> Token.Field;
			case ID_START_OBJECT -> Token.Struct;
			case ID_END_OBJECT -> Token.StructEnd;
			case ID_START_ARRAY -> Token.Array;
			case ID_END_ARRAY -> Token.ArrayEnd;
			default -> Token.Nope;
		};
	}

	@Override public Token peek() throws IOException {
		return asToken(ensurePeeked());
	}

	@Override public int takeInt() throws IOException {
		ensurePeeked();
		int i = switch (peeked) {
			case ID_NUMBER_INT -> parser.getIntValue();
			case ID_NUMBER_FLOAT -> {
				double d = parser.getDoubleValue();
				if (d % 1 == 0) {
					int asInt = (int) d;
					// round-trip to double
					if ((double) asInt == d) yield asInt;
				}
				expected("int number");
				yield Integer.MIN_VALUE;
			}
			default -> {
				expected("int number");
				parser.skipChildren();
				yield Integer.MIN_VALUE;
			}
		};
		peeked = ID_NO_TOKEN;
		return i;
	}

	@Override public long takeLong() throws IOException {
		long l = switch (ensurePeeked()) {
			case ID_NUMBER_INT -> parser.getLongValue();
			case ID_NUMBER_FLOAT -> {
				double d = parser.getDoubleValue();
				if (d % 1 == 0) {
					long asLong = (long) d;
					// roundtrip to double
					if ((double) asLong == d) yield asLong;
				}
				expected("long number");
				yield Long.MIN_VALUE;
			}
			default -> {
				expected("long number");
				parser.skipChildren();
				yield Long.MIN_VALUE;
			}
		};
		peeked = ID_NO_TOKEN;
		return l;
	}

	@Override public double takeDouble() throws IOException {
		double d = switch (ensurePeeked()) {
			case ID_NUMBER_INT, ID_NUMBER_FLOAT -> parser.getDoubleValue();
			default -> {
				expected("float number");
				parser.skipChildren();
				yield Double.NaN;
			}
		};
		peeked = ID_NO_TOKEN;
		return d;
	}

	@Override public boolean takeBoolean() throws IOException {
		boolean b = switch (ensurePeeked()) {
			case ID_TRUE -> true;
			case ID_FALSE -> false;
			default -> {
				expected("boolean");
				parser.skipChildren();
				yield false;
			}
		};
		peeked = ID_NO_TOKEN;
		return b;
	}

	@Override public void takeNull() throws IOException {
		if (ensurePeeked() != ID_NULL) {
			expected("null");
			parser.skipChildren();
		}
		peeked = ID_NO_TOKEN;
	}

	private void expected(String expected) throws IOException {
		problems.enque(new Problem.UnexpectedToken(
				path(), expected, parser.getText(), asToken(peeked)));
	}

	@Override public String takeString() throws IOException {
		String s;
		if (ensurePeeked() == ID_STRING) {
			s = parser.getText();
		} else {
			// every scalar can be a string? lax mode?
			s = NOT_A_STRING;
			expected("string");
			parser.skipChildren();
		}
		peeked = ID_NO_TOKEN;
		return s;
	}

	@Override public int takeString(NameIndex names) throws IOException {
		int i;
		if (ensurePeeked() == ID_STRING) {
			i = names.index(parser.getText());
		} else {
			i = NameIndex.UNKNOWN;
			expected("name string");
			parser.skipChildren();
		}
		peeked = ID_NO_TOKEN;
		return i;
	}

	@Override public int takeField() throws IOException {
		int i;
		if (ensurePeeked() == ID_FIELD_NAME) {
			i = frames[framePointer].index(parser.getCurrentName());
		} else {
			i = NameIndex.UNKNOWN;
			expected("field name");
			// should be skip children? would it be a syntax problem only
			//parser.skipChildren();
		}
		peeked = ID_NO_TOKEN;
		return i;
	}

	@Override public void skip() throws IOException {
		ensurePeeked();
		parser.skipChildren();
		peeked = ID_NO_TOKEN;
	}

	@Override public boolean hasNext() throws IOException {
		ensurePeeked();
		// not clearing peeked!
		// this logic doesn't look if we're inside an object or an array.
		// is it a problem? Essentially if something is wrong,
		// it would be a syntax problem and IOException would be fine
		return peeked != ID_END_ARRAY && peeked != ID_END_OBJECT;
	}

	@Override public void beginArray() throws IOException {
		if (ensurePeeked() == ID_START_ARRAY) {
			peeked = ID_NO_TOKEN;
		} else {
			expected("'[' array");
			parser.skipChildren();
			// artificially injecting end array after skipping
			// anything which is not array
			// this will force hasNext to return false,
			// and endArray to work
			peeked = ID_END_ARRAY;
		}
	}

	@Override public void endArray() throws IOException {
		if (ensurePeeked() != ID_END_ARRAY) {
			expected("']' array end");
			// this is very hard case to get out bad state
			// if this will ever occur. Skipping children
			// might not do what we want, but that would be a syntax
			// problem, so our worries might be void
			parser.skipChildren();
		}
		peeked = ID_NO_TOKEN;
	}

	@Override public void beginStruct(NameIndex names) throws IOException {
		// even if token mismatch, due to how we handle below,
		// we still need to move pointer and allocate array,
		// so logic in endStruct will not fail
		if (++framePointer > frames.length) {
			frames = Arrays.copyOf(frames, frames.length * 2);
		}

		if (ensurePeeked() == ID_START_OBJECT) {
			frames[framePointer] = (JsonNameIndex) names;
			peeked = ID_NO_TOKEN;
		} else {
			expected("'{' struct");
			parser.skipChildren();
			// injecting end object so hasNext will return false
			// and endStruct will silently succeed
			peeked = ID_END_OBJECT;
		}
	}

	@Override public void endStruct() throws IOException {
		if (ensurePeeked() == ID_END_OBJECT) {
			frames[framePointer--] = null;
		} else {
			expected("'}' struct end");
			// see endArray for thoughts about that
			parser.skipChildren();
		}
		peeked = ID_NO_TOKEN;
	}

	@Override public NameIndex index(String... known) {
		return new JsonNameIndex(known);
	}

	@Override public String name() throws IOException {
		return parser.getText();
	}

	@Override public Buffer takeBuffer() throws IOException {
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

		@Override public In in() {
			return new JsonParserIn(buffer.asParser());
		}
	}

	private int framePointer = -1;
	private JsonNameIndex[] frames = new JsonNameIndex[8];

	@Override public AtPath path() {
		// we can build/maintain path of our own,
		// but we will just reconstruct it from Jackson's context
		// first we will unwind contexts
		var contexts = new ArrayList<JsonStreamContext>();

		for (@Null var c = parser.getParsingContext(); c != null && !c.inRoot(); c = c.getParent()) {
			contexts.add(0, c);
		}
		// then will construct properly linked path
		AtPath current = AtPath.Root.Root;
		for (var c : contexts) {
			if (c.inObject()) {
				current = new AtPath.FieldOf(current, c.getCurrentName());
			} else if (c.inArray()) {
				current = new AtPath.ElementAt(current, c.getCurrentIndex());
			}
		}
		return current;
	}
}
