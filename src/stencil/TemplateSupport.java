package io.immutables.stencil;

import io.immutables.meta.Null;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import static io.immutables.stencil.Output.isSimpleWhitespace;

@SuppressWarnings("all")
public class TemplateSupport extends Stencil.Raw {
	public Runnable bl(Runnable b) {
		return () -> {
			dl();
			b.run();
			dl();
		};
	}

	public void $(String s) {
		out().put(s);
	}

	public void $(CharSequence s) {
		out().put(s);
	}

	public void $(@Null Object object) {
		// delegate to specialized methods for consistency
		if (object instanceof String s) $(s);
		else if (object instanceof CharSequence s) $(s);
		else if (object instanceof Supplier<?> s) $(s);
		else if (object instanceof Runnable r) $(r);
		else out().put(object);
	}

	public void dl() {
		out().deline();
	}

	public void tb() {
		out().trimWhitespaceBefore();
	}

	public void ta() {
		out().trimWhitespaceAfter();
	}

	public StringBuilder let(Runnable runnable) {
		var o = out();
		int wasLength = o.raw.length();

		runnable.run();

		int nowLength = o.raw.length();
		var captured = new StringBuilder(nowLength - wasLength);
		captured.append(o.raw, wasLength, nowLength);
		o.raw.setLength(wasLength);
		return captured;
	}

	public void $(Supplier<?> s) {
		out().put(s.get());
	}

	public void $(Runnable r) {
		var o = out();
		var line = o.currentLine();
		retainOnlyLeadingWhitespace(line);
		var prefix = line.toString();

		@Null var wasPrefix = o.indentPrefix;
		o.indentPrefix = prefix;
		try {
			r.run();
		} finally {
			// we don't care if 'out()' was changed (not re-querying it),
			// we put back indent prefix on the instance we set it
			o.indentPrefix = wasPrefix;
		}
	}


	private void retainOnlyLeadingWhitespace(StringBuilder line) {
		int i = 0;
		for (; i < line.length(); i++) {
			char c = line.charAt(i);
			if (!isSimpleWhitespace(c)) break;
		}
		line.setLength(i);
	}

	public static boolean $if(boolean value) {
		return value;
	}

	public static boolean $if(@Null Boolean v) {
		return v == Boolean.TRUE;
	}

	public static boolean $if(@Null String v) {
		return v != null && !v.isEmpty();
	}

	public static boolean $if(@Null Collection<?> c) {
		return c != null && !c.isEmpty();
	}

	public static boolean $if(@Null Map<?, ?> m) {
		return m != null && !m.isEmpty();
	}

	public static boolean $if(@Null Object v) {
		if (v == null) return false;
		if (v instanceof Boolean b) return b;
		if (v instanceof String s) return !s.isEmpty();
		if (v instanceof Number n) return n.intValue() != 0;
		if (v instanceof Optional<?> o) return o.isPresent();
		return true;
	}

	public static boolean[] $for(boolean[] b) {return b;}
	public static byte[] $for(byte[] b) {return b;}
	public static char[] $for(char[] c) {return c;}
	public static short[] $for(short[] s) {return s;}
	public static int[] $for(int[] i) {return i;}
	public static long[] $for(long[] l) {return l;}
	public static float[] $for(float[] f) {return f;}
	public static double[] $for(double[] d) {return d;}

	public static <E> Iterable<E> $for(Iterator<E> e) {return () -> e;}
	public static <E> Iterable<E> $for(Iterable<E> e) {return e;}
	public static <E> Iterable<E> $for(Stream<E> s) {return s::iterator;}

	public static Iterable<Integer> $for(IntStream s) {return s::iterator;}
	public static Iterable<Long> $for(LongStream s) {return s::iterator;}
	public static Iterable<Double> $for(DoubleStream s) {return s::iterator;}

	public static <E> Iterable<E> $for(Optional<E> o) {return o.stream()::iterator;}
	public static Iterable<Integer> $for(OptionalInt o) {return o.stream()::iterator;}
	public static Iterable<Long> $for(OptionalLong o) {return o.stream()::iterator;}
	public static Iterable<Double> $for(OptionalDouble o) {return o.stream()::iterator;}

	public static <E> Iterable<E> $for(@Null E nullable) {
		return nullable != null ? List.of(nullable) : List.of();
	}
}
