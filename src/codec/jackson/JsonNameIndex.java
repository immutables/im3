package io.immutables.codec.jackson;

import io.immutables.codec.NameIndex;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.SerializedString;

public class JsonNameIndex extends NameIndex {
	private final String[] known;
	final SerializedString[] serialized;

	public JsonNameIndex(String[] known) {
		this.known = known;
		this.serialized = toSerialized(known);
	}

	public String[] known() {
		return known.clone();
	}

	public int index(String name) {
		// dumb unoptimized implementation,
		// I've seen how Jackson handles name symbols, it's quite incompatible
		// with what we expect from this abstraction,
		// so I give up for now, let's return to this later
		for (int i = 0; i < serialized.length; i++) {
			if (serialized[i].getValue().equals(name)) {
				return i;
			}
		}
		return UNKNOWN;
	}

	public String name(int index) {
		return known[index];
	}

	private static SerializedString[] toSerialized(String[] known) {
		var serialized = new SerializedString[known.length];
		for (int i = 0; i < known.length; i++) {
			serialized[i] = new SerializedString(known[i]);
		}
		return serialized;
	}
}
