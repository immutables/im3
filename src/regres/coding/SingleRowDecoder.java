package io.immutables.regres.coding;

import io.immutables.codec.Codec;
import io.immutables.codec.DefaultingCodec;
import io.immutables.codec.In;
import io.immutables.codec.Out;
import io.immutables.meta.Null;
import io.immutables.regres.SqlAccessor;
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
				if (!single.ignoreMore()) throw new IOException(
					"More than one row available, use @Single(ignoreMore=true) to skip the rest");
				do {
					in.skip();
				} while (in.hasNext());
			}
		} else {
			if (!single.optional()) throw new IOException(
				"Exactly one row expected as result, was none. " +
					"Use @Single(optional=true) to allow no results.");

			if (codec instanceof DefaultingCodec<Object, In, Out> defaulting
				&& defaulting.providesDefault()) {
				returnValue = defaulting.getDefault();
			} else throw new IOException("Cannot provide default/null value for the missing row");
		}

		in.endArray();
		return returnValue;
	}

	@Override
	public void encode(Out out, Object instance) {
		throw new UnsupportedOperationException("This codec in only for decoding");
	}
}
