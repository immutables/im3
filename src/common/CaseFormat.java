// Copyright (C) 2006 The Guava Authors
// Copyright 2023 Immutables Authors and Contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package io.immutables.common;

import static java.util.Objects.requireNonNull;

import io.immutables.meta.Null;

/**
 * Utility class for converting between various case formats. The difference from Guava's
 * version is that it can work with regular unicode-ish support of {@link Character#isUpperCase(char)}/{@link String#toLowerCase()}/{@link String#toUpperCase()}
 * and it will still work probably fine with latin-1 available in new JDKs.
 *
 * @author Mike Bostock (original version from Guava)
 * @author Ievgen Lukash (refinements, simplified and modernized)
 */
public enum CaseFormat {
  /** Hyphenated variable naming convention, e.g., "lower-hyphen". */
  LowerKebob(c -> c == '-', "-") {
    @Override String normalizeWord(String word) {
			return word.toLowerCase();
		}

    @Override String convert(CaseFormat format, String s) {
			return switch (format) {
				case LowerSnake -> s.replace('-', '_');
				case UpperSnake -> s.replace('-', '_').toUpperCase();
				default -> super.convert(format, s);
			};
    }
  },

  /** C++ variable naming convention, e.g., "lower_underscore". */
  LowerSnake(c -> c == '_', "_") {
    @Override String normalizeWord(String word) {
			return word.toLowerCase();
		}

    @Override String convert(CaseFormat format, String s) {
			return switch (format) {
				case LowerKebob -> s.replace('_', '-');
				case UpperSnake -> s.toUpperCase();
				default -> super.convert(format, s);
			};
    }
  },

  /** Java variable naming convention, e.g., "lowerCamel". */
  LowerCamel(Character::isUpperCase, "") {
    @Override String normalizeWord(String word) {
      return firstCharOnlyToUpper(word);
    }

    @Override String normalizeFirstWord(String word) {
      return word.toLowerCase();
    }
  },

  /** Java and C++ class naming convention, e.g., "UpperCamel". */
  UpperCamel(Character::isUpperCase, "") {
    @Override String normalizeWord(String word) {
      return firstCharOnlyToUpper(word);
    }
  },

  /** Java and C++ constant naming convention, e.g., "UPPER_UNDERSCORE". */
  UpperSnake(c -> c == '_', "_") {
    @Override String normalizeWord(String word) {
      return word.toUpperCase();
    }

    @Override String convert(CaseFormat format, String s) {
			return switch (format) {
				case LowerKebob -> s.replace('_', '-').toLowerCase();
				case LowerSnake -> s.toLowerCase();
				default -> super.convert(format, s);
			};
    }
  };

  private final CharMatcher boundary;
  private final String separator;

  CaseFormat(CharMatcher boundary, String separator) {
    this.boundary = boundary;
    this.separator = separator;
  }

	abstract String normalizeWord(String word);

	String normalizeFirstWord(String word) {
		return normalizeWord(word);
	}

	/**
   * Converts the specified {@code String str} from this format to the specified {@code format}.
	 * A "best effort" approach is taken; if {@code str} does not conform to the assumed format,
	 * then the behavior of this method is undefined, but we make a reasonable effort at
	 * converting anyway.
   */
  public final String to(CaseFormat format, String s) {
		requireNonNull(format);
    requireNonNull(s);
    return format == this ? s : convert(format, s);
  }

  /** Enum values can override for performance reasons. */
  String convert(CaseFormat format, String s) {
    // deal with camel conversion
    @Null StringBuilder out = null;
    int i = 0;
    int j = -1;
    while ((j = indexNextBoundary(s, ++j)) != -1) {
      if (i == 0) {
        // include some extra space for separators
        out = new StringBuilder(s.length() + 4 * format.separator.length());
        out.append(format.normalizeFirstWord(s.substring(i, j)));
      } else {
        requireNonNull(out).append(format.normalizeWord(s.substring(i, j)));
      }
      out.append(format.separator);
      i = j + separator.length();
    }
    return (i == 0)
        ? format.normalizeFirstWord(s)
        : requireNonNull(out).append(format.normalizeWord(s.substring(i))).toString();
  }

	private int indexNextBoundary(String s, int startIndex) {
		for (int i = startIndex; i < s.length(); i++) {
			if (boundary.matches(s.charAt(i))) return i;
		}
		return -1;
	}

  private static String firstCharOnlyToUpper(String word) {
    return word.isEmpty()
        ? word
        : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
  }

	@FunctionalInterface
	private interface CharMatcher {
		boolean matches(char c);
	}
}
