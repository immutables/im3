package io.immutables.lang.node;

import io.immutables.lang.back.Output;
import java.io.IOException;

public final class SourceSpan extends CharSpan implements Output.Aware, Node.TextElement {
/*
	public int termBegin;
	public int termEnd;
*/
	public void output(Output o) throws IOException {
		o.raw(buffer, offset, length);
	}

	public SourceSpan nudge(int offsetDelta, int lengthDelta) {
		offset += offsetDelta;
		length += lengthDelta;
		assert offset >= 0 && length >= 0;
		return this;
	}

	public String toString() {
		return String.valueOf(buffer, offset, length);
	}

	public static final class Preallocated {
		private final SourceSpan[] spans = new SourceSpan[BATCH_SIZE];
		private int index;

		public Preallocated() {
			preallocateNewBatch();
		}

		public SourceSpan create(char[] buffer, int offset, int length) {
			var span = spans[index++];
			if (index == BATCH_SIZE) {
				preallocateNewBatch();
				index = 0;
			}
			span.buffer = buffer;
			span.offset = offset;
			span.length = length;
			return span;
		}

		private void preallocateNewBatch() {
			for (int i = 0; i < BATCH_SIZE; i++) {
				spans[i] = new SourceSpan();
			}
		}

		private static final int BATCH_SIZE = 128;
	}
}
