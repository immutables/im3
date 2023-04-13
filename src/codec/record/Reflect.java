package io.immutables.codec.record;

import io.immutables.meta.Null;
import java.lang.reflect.*;

final class Reflect {
	private Reflect() {}

	static Object newInstance(Constructor<?> constructor, Object[] arguments) {
		try {
			return constructor.newInstance(arguments);
		} catch (IllegalAccessException | InstantiationException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	static Object getValue(Method accessor, Object instance) {
		try {
			return accessor.invoke(instance);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	static Object constructValue(Member constructorOrStaticMember) {
		try {
			if (constructorOrStaticMember instanceof Constructor<?> c) {
				return c.newInstance();
			}
			if (constructorOrStaticMember instanceof Field f) {
				return f.get(null);
			}
			if (constructorOrStaticMember instanceof Method m) {
				return m.invoke(null);
			}
			throw new IllegalArgumentException(constructorOrStaticMember + " is not applicable ");
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getCause());
		}
	}
}
