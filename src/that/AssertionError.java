package io.immutables.that;

import io.immutables.meta.Null;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Special extension of {@link java.lang.AssertionError} to provide convenience formatting
 * of mismatch information and stack trace.
 */
final class AssertionError extends java.lang.AssertionError {
	private final String sourceLine;

	AssertionError(String... mismatch) {
		super(join(mismatch));
		StackTraceElement[] stack = getStackTrace();
		this.sourceLine = findSourceLine(getStackTrace()).orElse("");
		if (!fullStackTrace) {
			this.setStackTrace(trimStack(stack));
		}
	}

	private Optional<String> findSourceLine(StackTraceElement[] trace) {
		var packagePrefix = getClass().getPackageName() + ".";
		int i = 0;
		while (i < trace.length) {
			if (!trace[i].getClassName().contains(packagePrefix)) break;
			i++;
		}
		int firstThatCallerIndex = i;
		var frame = trace[i];

		// > 0: we don't care about, usually synthetic, 0 index line
		if (frame.getFileName() != null && frame.getLineNumber() > 0) {
			var filename = frame.getFileName();
			var classname = frame.getClassName();
			var walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
			// need proper classloader to load resource
			var foundClass = walker.walk(s -> s.dropWhile(f ->
					!(f.getClassName().equals(classname) && f.getFileName().equals(filename)))
				.map(StackWalker.StackFrame::getDeclaringClass)
				.findFirst());
			return foundClass.flatMap(f ->
					readLine(foundClass.get(), toResourceName(frame), frame.getLineNumber()));
		}
		return Optional.empty();
	}

	private static String join(String... mismatch) {
		return Stream.of(mismatch).collect(Collectors.joining("\n\t", "\n\t", ""));
	}

	@Override
	public String getMessage() {
		return !replaceErrorMessage.isEmpty() ? replaceErrorMessage : super.getMessage();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + showSourceHint() + super.getMessage();
	}

	private String showSourceHint() {
		return !sourceLine.isEmpty() ? "~~~> " + sourceLine.trim() : "";
	}

	private static StackTraceElement[] trimStack(StackTraceElement[] stack) {
		return Arrays.stream(stack)
			.dropWhile(e -> isThatPackage(e))
			.takeWhile(e -> !isFrameworkPackage(e))
			.toArray(StackTraceElement[]::new);
	}

	private static boolean isFrameworkPackage(StackTraceElement e) {
		for (var prefix : frameworkPackagePrefixes) {
			if (e.getClassName().startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isThatPackage(StackTraceElement e) {
		return e.getClassName().startsWith(AssertionError.class.getPackage().getName());
	}

	private static String toResourceName(StackTraceElement e) {
		return "/" + getPackagePath(e.getClassName()) + "/" + e.getFileName();
	}

	private static String getPackagePath(String qualifiedName) {
		// in binary-form qualified name, nested classes use '$' separator,
		// so we should be ok with last dot
		int lastDot = qualifiedName.lastIndexOf('.');
		return lastDot < 0 ? "" : qualifiedName.substring(0, lastDot).replace('.', '/');
	}

	private static Optional<String> readLine(Class<?> forClass, String resourceName,
		int lineNumber) {
		assert lineNumber > 0;

		try (
			var s = forClass.getModule().getResourceAsStream(resourceName);
			var in = new InputStreamReader(s, StandardCharsets.UTF_8);
			var r = new BufferedReader(in)) {
			return r.lines()
				.skip(lineNumber - 1)
				.findFirst();
		} catch (NullPointerException | IOException sourceCannotBeRead) {
			return Optional.empty();
		}
	}

	private static final String[] frameworkPackagePrefixes = {"org.junit.", "org.intellij."};

	/**
	 * Some test reporting systems output both message and toString if this replacement
	 * is enabled, we're outputting it when getMessage is called.
	 * But toString will still output our message.
	 */
	private static final String replaceErrorMessage =
		System.getProperty("io.immutables.that.replace-error-message", "");

	/**
	 * If full stack trace should always be available, with no trimming it for convenient output
	 */
	private static final boolean fullStackTrace =
		Boolean.getBoolean("io.immutables.that.full-stack-trace");
}
