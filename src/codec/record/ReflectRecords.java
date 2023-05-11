package io.immutables.codec.record;

import io.immutables.common.Unreachable;
import java.lang.reflect.*;
import java.util.Arrays;

public final class ReflectRecords {
	private ReflectRecords() {}

	public static Object newInstance(Constructor<?> constructor, Object... arguments) {
		try {
			return constructor.newInstance(arguments);
		} catch (IllegalAccessException | InstantiationException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			var cause = e.getCause();
			if (cause instanceof RuntimeException r) throw r;
			if (cause instanceof Error r) throw r;
			throw new RuntimeException(cause);
		}
	}

	public static Object getValue(RecordComponent component, Object instance) {
		return getValue(component.getAccessor(), instance);
	}

	public static Object getValue(Method accessor, Object instance) {
		try {
			return accessor.invoke(instance);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			var cause = e.getCause();
			if (cause instanceof RuntimeException r) throw r;
			if (cause instanceof Error r) throw r;
			throw new RuntimeException(cause);
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
			throw new IllegalArgumentException(constructorOrStaticMember + " is not applicable");
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			var cause = e.getCause();
			if (cause instanceof RuntimeException r) throw r;
			if (cause instanceof Error r) throw r;
			throw new RuntimeException(cause);
		}
	}

	public static Constructor<?> getCanonicalConstructor(Class<?> record) {
		assert record.isRecord();
		RecordComponent[] components = record.getRecordComponents();
		constructors: for (var c : record.getDeclaredConstructors()) {
			var parameterTypes = c.getParameterTypes();
			// we do quick comparison avoiding creating any objects inside this loop
			if (parameterTypes.length == components.length) {
				for (int i = 0; i < components.length; i++) {
					if (parameterTypes[i] != components[i].getType()) {
						continue constructors;
					}
				}
				return c;
			}
		}
		throw Unreachable.contractual();
	}
}
