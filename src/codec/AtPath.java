package io.immutables.codec;

import java.util.ArrayList;
import java.util.List;

/**
 * Path expressed as singly linked list of field and element positions.
 * Starts at {@link Root#Root}, then proceeds
 */
public sealed interface AtPath {
	enum Root implements AtPath {
		Root;
		@Override public String toString() {
			return stringify(this);
		}
	}

	record ElementAt(AtPath path, int index) implements AtPath {
		@Override public String toString() {
			return stringify(this);
		}
	}

	record FieldOf(AtPath path, String name) implements AtPath {
		@Override public String toString() {
			return stringify(this);
		}
	}

	default List<AtPath> unwind() {
		var list = new ArrayList<AtPath>();
		append(list, this);
		return List.copyOf(list);
	}

	private static void append(List<AtPath> list, AtPath path) {
		if (path instanceof ElementAt e) {
			append(list, e.path);
		} else if (path instanceof FieldOf f) {
			append(list, f.path);
		}
		list.add(path);
	}

	/** Prints {@link AtPath} to string, roughly, JSON-path notation. */
	private static String stringify(AtPath path) {
		var builder = new StringBuilder();
		append(builder, path);
		return builder.toString();
	}

	private static void append(StringBuilder builder, AtPath path) {
		// we recurse first, then appending own
		if (path instanceof ElementAt e) {
			append(builder, e.path);
			builder.append('[').append(e.index).append(']');
		} else if (path instanceof FieldOf f) {
			append(builder, f.path);
			builder.append('.').append(f.name);
		} else if (path == Root.Root) {
			builder.append('$');
		}
	}
}
