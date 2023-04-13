package io.immutables.codec.record;

import io.immutables.codec.DefaultingCodec;
import io.immutables.codec.In;
import io.immutables.codec.Out;
import java.io.IOException;

abstract class CaseCodec<T, I extends In, O extends Out> extends DefaultingCodec<T, I, O> {
	/**
	 * Checks input (should be a buffered input) if it may conform.
	 * If {@code true} is returned, this is not 100% guarantee that construction parsing
	 * will succeed, however {@code false} indicates that we should not expect it
	 * will succeed, so we will go to a next case to try. It is expected that this check
	 * would be at least somewhat easier, quicker than an attempt to actually decode
	 * an instance. In case true is returned it is expected that structure will be properly
	 * consumed. This can help arranging nested checks. For when we about to return {@code false},
	 * it would be not that important, and we can leave buffered input partially consumed/broken.
	 */
	public boolean mayConform(I in) throws IOException {return false;}
}
