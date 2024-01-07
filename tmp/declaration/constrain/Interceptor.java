package io.immutables.declaration.constrain;

import java.util.List;

public interface Interceptor {
	/**
	 * Reports violation to the interceptor and interceptor replies
	 * TODO
	 * @param type class (raw type if parameterized)
	 * @param violations constraint violations
	 * @return {@code true} if exception have to be thrown.
	 */
	boolean violated(Class<?> type, List<Violation> violations);
}
