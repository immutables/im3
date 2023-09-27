package io.immutables.common;

import io.immutables.meta.Null;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;

/**
 * Base abstract class for non-value interface proxies.
 * Non-value here means that equality is based on the object identity.
 */
public abstract class ProxyHandler implements InvocationHandler {
	/** Override to handle relevant methods. */
	protected abstract @Null Object handleInterfaceMethod(
			Object proxy, Method method, Object[] arguments) throws Throwable;

	@Override
	public final @Null Object invoke(
			Object proxy, Method method, @Null Object[] arguments) throws Throwable {
		if (arguments == null) arguments = NO_ARGS;

		if (isHashCode(method)) return System.identityHashCode(proxy);
		if (isEquals(method)) return proxy == arguments[0];
		if (isToString(method)) return toString(proxy);

		return handleInterfaceMethod(proxy, method, arguments);
	}

	/**
	 * Use this method in subclasses if you want to delegate call to a target instance.
	 * This does reflective call and unwrap exceptions.
	 */
	protected final @Null Object delegate(
			Object target, Method method, Object... arguments) throws Throwable {
		assert method.getDeclaringClass().isInstance(target);
		try {
			return method.invoke(target, arguments);
		} catch (InvocationTargetException e) {
			var cause = e.getCause();
			if (cause instanceof Error error) throw error;
			if (cause instanceof RuntimeException exception) throw exception;
			if (cause instanceof Exception maybeDeclared
					&& declaredInThrows(method, maybeDeclared)) throw maybeDeclared;

			throw new UndeclaredThrowableException(cause);
		}
	}

	private static boolean declaredInThrows(Method method, Exception exception) {
		return Arrays.stream(method.getExceptionTypes())
				.anyMatch(thrown -> thrown.isInstance(exception));
	}

	protected Object toString(Object proxy) {
		return "proxy" + Arrays.stream(proxy.getClass().getInterfaces())
				.map(c -> {
					// we would not allow interfaces which have no canonical
					// name, but because we're not generally enforcing it (at least here)
					// we just allow it defaulting to regular name
					@Null String name = c.getCanonicalName();
					return name != null ? name : c.getName();
				}).toList();
	}

	/** Always delegates to {@link Object#equals(Object)} */
	@Override
	public final boolean equals(@Null Object obj) {
		return super.equals(obj);
	}

	/** Always delegates to {@link Object#hashCode()} */
	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	public static boolean isToString(Method method) {
		return method.getParameterCount() == 0
				&& method.getName().equals("toString");
	}

	public static boolean isEquals(Method method) {
		return method.getParameterCount() == 1
				&& method.getName().equals("equals")
				&& method.getParameterTypes()[0] == Object.class;
	}

	public static boolean isHashCode(Method method) {
		return method.getParameterCount() == 0
				&& method.getName().equals("hashCode");
	}

	private static final Object[] NO_ARGS = {};
}
