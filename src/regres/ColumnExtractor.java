package io.immutables.regres;

import io.immutables.codec.Codec;
import io.immutables.codec.In;
import io.immutables.codec.NameIndex;
import io.immutables.codec.Out;
import io.immutables.meta.Null;
import java.io.IOException;

final class ColumnExtractor extends Codec<Object, In, Out> {
	private final Codec<Object, In, Out> codec;
	private final SqlAccessor.Column column;

	ColumnExtractor(Codec<Object, In, Out> codec, SqlAccessor.Column column) {
		this.codec = codec;
		this.column = column;
	}

	@Override
	public Object decode(In in) throws IOException {
		@Null Object columnValue = null;
		boolean matched = false;
		boolean failed = false;
		String columnName = column.value();
		int columnIndex = column.index();

		in.beginStruct(NameIndex.unknown());

		int i = 0;
		while (in.hasNext()) {
			in.takeField();
			if (!columnName.isEmpty()) {
				if (columnName.equals(in.name())) {
					columnValue = codec.decode(in);
					matched = true;
					failed = in.problems.raised();
					break;
				} else in.skip();
			} else {
				if (i == columnIndex) {
					columnValue = codec.decode(in);
					matched = true;
					failed = in.problems.raised();
					break;
				} else in.skip();
			}
			i++;
		}

		// politely consume the row to the end
		while (in.hasNext()) {
			in.takeField();
			in.skip();
		}
		in.endStruct();

		if (!matched) throw new IOException("No column matched for " + column);

		// FIXME how to best integrate with problem reporting?
		if (failed) throw new IOException("Decoding column failed: " + in.name());

		return columnValue;
	}

	@Override
	public void encode(Out out, Object instance) {
		throw new UnsupportedOperationException();
	}
}
