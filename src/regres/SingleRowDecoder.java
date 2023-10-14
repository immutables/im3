package io.immutables.regres;

import io.immutables.codec.Codec;
import io.immutables.codec.DefaultingCodec;
import io.immutables.codec.In;
import io.immutables.codec.Out;
import io.immutables.meta.Null;
import java.io.IOException;

final class SingleRowDecoder extends Codec<Object, In, Out> {
	private final Codec<Object, In, Out> codec;
	private final SqlAccessor.Single single;

	SingleRowDecoder(Codec<Object, In, Out> codec, SqlAccessor.Single single) {
		this.codec = codec;
		this.single = single;
	}

	@Override
	public Object decode(In in) throws IOException {
		@Null Object returnValue = null;

		in.beginArray();

		if (in.hasNext()) {
			returnValue = codec.decode(in);

			if (in.hasNext()) {
				if (!single.ignoreMore()) throw new SqlException(
					"More than one row available, use @Single(ignoreMore=true) to skip the rest");
				do {
					in.skip();
				} while (in.hasNext());
			}
		} else {
			if (!single.optional()) throw new SqlException(
				"Exactly one row expected as result, was none. " +
					"Use @Single(optional=true) to allow no results.");

			if (codec instanceof DefaultingCodec<Object, In, Out> defaulting
				&& defaulting.hasDefault()) {
				returnValue = defaulting.getDefault(in);
			} else throw new SqlException("Cannot provide default/null value for the missing row");
		}

		in.endArray();
		return returnValue;
	}

	@Override
	public void encode(Out out, Object instance) {
		throw new UnsupportedOperationException("This codec in only for decoding");
	}
}
