package io.immutables.stencil;

import java.util.regex.Pattern;

public abstract class Glob {
	private Glob() {}

	public static Glob of(String pattern) {
		pattern = pattern.trim(); // trim whitespace, always

		int atAnyDirs = pattern.indexOf(ANY_DIRS);
		int afterAnyDirs = atAnyDirs + ANY_DIRS.length();

		if (atAnyDirs > NOPE) {
			// found multiple dirs pattern which must occur only once
			// this would split match in suffix/prefix
			int atNextAnyDirs = pattern.indexOf(ANY_DIRS, afterAnyDirs);
			if (atNextAnyDirs > NOPE) {
				if (pattern.startsWith(ANY_DIRS) && pattern.endsWith(ANY_DIRS)) {
					checkSlashes(pattern, atNextAnyDirs, afterAnyDirs);
					var middle = pattern.substring(afterAnyDirs + 1, atNextAnyDirs - 1);
					// +/- 1 is for slashes, we've checked they are ok
					if (!middle.contains(ANY_DIRS)) {
						// otherwise we're falling to the syntax exception below
						// think of it as goto, which is bad,
						// but solving might over-complicate even more

						return middlePattern(middle);
					}
				}
				throw new IllegalArgumentException(
					"Unsupported glob syntax, can occur only as " +
						"a (prefix) or a (suffix)" +
						"or (suffix and prefix) or in the (middle) " +
						"but was: " + pattern);
			}

			checkSlashes(pattern, atAnyDirs, afterAnyDirs);
			return prefixSuffixPattern(pattern, atAnyDirs, afterAnyDirs);
		}
		return wholePattern(toRegex("^", pattern, "$"));
	}

	private static Glob middlePattern(String pattern) {
		// couple of variants may match,
		// as there might be many matches,
		// and we don't want to take the wrong one
		var asMiddle = toRegex("/", pattern, "/");
		var asPrefix = toRegex("^", pattern, "/");
		var asSuffix = toRegex("/", pattern, "$");
		var asWhole= toRegex("^", pattern, "$");
		return new Glob() {
			@Override public boolean matches(String path) {
				return asMiddle.matcher(path).find()
					|| asPrefix.matcher(path).find()
					|| asSuffix.matcher(path).find()
					|| asWhole.matcher(path).find();
			}
		};
	}

	private static Glob wholePattern(Pattern full) {
		return new Glob() { // "just/this"
			@Override public boolean matches(String path) {
				return full.matcher(path).matches();
			}
		};
	}

	private static Glob prefixSuffixPattern(String pattern, int before, int after) {
		boolean noPrefix = before == 0;
		boolean noSuffix = after == pattern.length();
		if (noPrefix && noSuffix) {
			return new Glob() {// "**"
				@Override public boolean matches(String path) {
					return true;
				}
			};
		}
		if (noPrefix) {
			//special case where we wouldn't match slash itself
			var suffix = toRegex("", pattern.substring(after + 1), "$");
			return new Glob() {// "**/something
				@Override public boolean matches(String path) {
					return suffixMatches(suffix, path);
				}
			};
		}
		var prefixPart = pattern.substring(0, before);
		var prefix = toRegex("^", prefixPart, "");
		if (noSuffix) {// "something/**"
			return new Glob() {
				@Override public boolean matches(String path) {
					return prefixMatches(prefix,
						maybeIgnoringFrontSlash(prefixPart, path));
				}
			};
		}
		var suffix = toRegex("", pattern.substring(after + 1), "$");
		return new Glob() { // "something/**/else"
			@Override public boolean matches(String path) {
				return prefixMatches(prefix,
					maybeIgnoringFrontSlash(prefixPart, path))
					&& suffixMatches(suffix, path);
			}
		};
	}

	private static void checkSlashes(String pattern, int before, int after) {
		if (!hasSlashOrNothingInFront(pattern, before)
			|| !hasSlashOrNothingAtEnd(pattern, after)) {
			throw new IllegalArgumentException(
				"Unsupported glob syntax, ** signifies many dirs, " +
					"not part of the name, " +
					"need to have a slash separating it from other parts: "
					+ pattern);
		}
	}

	private static boolean hasSlashOrNothingAtEnd(String string, int after) {
		return after == string.length()
			|| string.charAt(after) == '/';
	}

	private static boolean hasSlashOrNothingInFront(String string, int before) {
		return before == 0
			|| string.charAt(before - 1) == '/';
	}

	private static String maybeIgnoringFrontSlash(String patternPrefix, String path) {
		if (!path.isEmpty() && path.charAt(0) == '/' && patternPrefix.charAt(0) != '/') {
			return path.substring(1);
		}
		return path;
	}

	private static boolean prefixMatches(Pattern prefix, String path) {
		return prefix.matcher(path).find();
	}

	private static boolean suffixMatches(Pattern suffix, String path) {
		var matcher = suffix.matcher(path);
		if (matcher.find()) {
			int start = matcher.start();
			return start == 0 || path.charAt(start - 1) == '/';
		}
		return false;
	}

	private static boolean middleMatches(Pattern suffix, String path) {
		var matcher = suffix.matcher(path);
		if (matcher.find()) {
			int start = matcher.start();
			return start == 0 || path.charAt(start - 1) == '/';
		}
		return false;
	}

	public abstract boolean matches(String path);

	private static Pattern toRegex(String prefix, String pattern, String suffix) {
		var regex = new StringBuilder(prefix);
		int i = 0;
		do {
			int atMany = pattern.indexOf("*", i);
			int atOne = pattern.indexOf("?", i);
			int min = NOPE;
			if (atMany > NOPE && atOne > NOPE) {
				min = Math.min(atMany, atOne);
			} else if (atMany > NOPE) {
				min = atMany;
				// below can be simplified to just 'else min = atOne;'
				// but it reads clearer
			} else if (atOne > NOPE) {
				min = atOne;
			}
			if (min == NOPE) break;
			if (min != i) {
				regex.append(Pattern.quote(
					pattern.substring(i, min)));
			}
			if (min == atMany) {
				regex.append("[^/]*"); // man non-slashes
			} else {
				regex.append("[^/]"); // single non-slash
			}
			i = min + 1; // next character after * or
		} while (true);

		if (i != pattern.length()) {
			// append tail if any
			regex.append(Pattern.quote(
				pattern.substring(i)));
		}
		regex.append(suffix);
		return Pattern.compile(regex.toString());
	}

	private static final String ANY_DIRS = "**";
	private static final int NOPE = -1;
}
