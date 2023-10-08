package io.immutables.codec;

import java.io.IOException;

public class Buffers {
	private Buffers() {}

	public static void pipeValue(In in, Out out) throws IOException {
		switch (in.peek()) { // @formatter:off
		case Null -> { in.takeNull(); out.putNull(); }
		case Int -> out.putInt(in.takeInt());
		case Long -> out.putLong(in.takeLong());
		case Float -> out.putDouble(in.takeDouble());
		case True -> out.putBoolean(true);
		case False -> out.putBoolean(false);
		case String -> out.putString(in.takeString());
		case Struct -> pipeStruct(out, in);
		case Array -> pipeArray(out, in);
		case Field, StructEnd, ArrayEnd, Nope, End -> throw new IllegalStateException();
		}	// @formatter:on
	}

	public static void pipeStruct(Out out, In in) throws IOException {
		var inFieldsUnknown = in.index();
		var sameForOutput = out.index();
		in.beginStruct(inFieldsUnknown);
		out.beginStruct(sameForOutput);
		while (in.hasNext()) {
			in.takeField();
			out.putField(in.name());
			pipeValue(in, out);
		}
		out.endStruct();
		in.endStruct();
	}

	public static void pipeArray(Out out, In in) throws IOException {
		in.beginArray();
		out.beginArray();
		while (in.hasNext()) {
			pipeValue(in, out);
		}
		out.endArray();
		in.endArray();
	}
}
