package io.immutables.stencil;

import io.immutables.meta.Null;
import java.util.function.Supplier;

/**
 * Current context
 */
public abstract class Current {
	// default output just buffers things
	@Null Output out = new Output();

	static Current current() {
		@Null var c = currentThreadLocal.get();
		return c != null ? c : new Current() {};
	}

	private static final ThreadLocal<Current> currentThreadLocal = new ThreadLocal<>();

	public static <S extends Stencil> S use(Current c, Supplier<? extends S> supplier) {
		try {
			currentThreadLocal.set(c);
			return supplier.get();
		} finally {
			currentThreadLocal.remove();
		}
	}
}
