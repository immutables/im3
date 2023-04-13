package io.immutables.codec.record;

import io.immutables.codec.In;
import io.immutables.codec.Out;
import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.Type;

final class InlineRecordCodec extends CaseCodec<Object, In, Out> {

	InlineRecordCodec(Type type, Class<?> raw, Lookup<In, Out> lookup) {

	}

	public void encode(Out out, Object instance) throws IOException {

	}

	public @Null Object decode(In in) throws IOException {
		return null;
	}

	public boolean mayConform(In in) throws IOException {
		return super.mayConform(in);
	}
}
