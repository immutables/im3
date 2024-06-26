package io.immutables.lang.node;

import io.immutables.lang.back.Output;
import io.immutables.lang.back.Output.Aware;
import io.immutables.meta.Null;
import java.io.IOException;
import java.util.*;

public final class Identifier extends CharSpan implements Aware {
	int hashCode;
	// id within pool of identifiers
	int id;
	// Backend can mark it as a keyword.
	// No thought have been put about having many distinct
	// backends in a single compiler runtime, so don't ask
	public boolean backendKeyword;

	static Identifier create(char[] buffer, int offset, int length) {
		var identifier = new Identifier();
		identifier.init(buffer, offset, length);
		return identifier;
	}

	void init(char[] buffer, int offset, int length) {
		this.buffer = buffer;
		this.offset = offset;
		this.length = length;

		int h = 5381;
		for (int i = 0; i < this.length; i++) {
			h += (h << 5) + this.buffer[this.offset + i];
		}
		this.hashCode = h;
	}

	private void init(char[] buffer, int offset, int length, int hashCode) {
		this.buffer = buffer;
		this.offset = offset;
		this.length = length;
		this.hashCode = hashCode;
	}

	public int hashCode() {
		return hashCode;
	}

	public boolean equals(Object obj) {
		return this == obj || ((obj instanceof Identifier i)
			&& Arrays.equals(
			i.buffer, i.offset, i.offset + i.length,
			buffer, offset, offset + length));
	}

	public String toString() {
		return String.valueOf(buffer, offset, length);
	}

	public void output(Output output) throws IOException {
		output.raw(buffer, offset, length);
		if (backendKeyword && output.screenBackendKeyword) output.raw('_');
	}

	private static class CharPage {
		final char[] content = new char[CHAR_PAGE_SIZE];
		private int limit = 0;

		int put(char[] buffer, int offset, int length) {
			assert length <= CHAR_PAGE_SIZE;
			int pageOffset = limit;
			if (pageOffset + 1 + length > content.length) return CHAR_PAGE_OVERFLOW;
			System.arraycopy(buffer, offset, content, pageOffset, length);
			limit += length + 1; // +1 - for \0 at the end
			return pageOffset;
		}
	}

	public static final class Pool {
		// it's not strictly necessary to track filled pages now
		// content char[] references will be retained by used identifier
		// objects, but for we can store all pages here
		private final List<CharPage> pages = new ArrayList<>();
		private CharPage currentPage = new CharPage();
		{
			pages.add(currentPage);
		}
		private final Map<Identifier, Identifier> internedIdentifiers = new HashMap<>(POOL_INITIAL_SIZE);
		private final Identifier probeInstance = new Identifier();
		private final Identifier[] preallocatedIdentifiers = new Identifier[POOL_PREALLOCATED_BATCH_SIZE];
		private int preallocatedIndex = 0;
		{
			preallocateNewBatch();
		}

		public void markBackendKeywords(String[] keywords) {
			for (var k : keywords) id(k).backendKeyword = true;
		}

		private void preallocateNewBatch() {
			for (int i = 0; i < POOL_PREALLOCATED_BATCH_SIZE; i++) {
				preallocatedIdentifiers[i] = new Identifier();
			}
		}

		public Collection<Identifier> values() {
			return Collections.unmodifiableCollection(internedIdentifiers.values());
		}

		private final char[] charBuffer = new char[CHAR_BUFFER_SIZE];

		public Identifier id(String string) {
			int length = string.length();
			char[] chars = charBuffer;
			if (length > CHAR_BUFFER_SIZE) {
				chars = string.toCharArray();
			} else {
				string.getChars(0, length, chars, 0);
			}
			return interned(chars, 0, length);
		}

		public Identifier interned(char[] buffer, int offset, int length) {
			assert length > 0;
			probeInstance.init(buffer, offset, length);

			@Null Identifier existing = internedIdentifiers.get(probeInstance);
			if (existing != null) return existing;

			int pageOffset = currentPage.put(buffer, offset, length);
			if (pageOffset == CHAR_PAGE_OVERFLOW) {
				currentPage = new CharPage();
				pages.add(currentPage);
				pageOffset = currentPage.put(buffer, offset, length);
				assert pageOffset == 0; // beginning of a new page
			}

			Identifier freshIdentifier = preallocatedIdentifiers[preallocatedIndex++];
			if (preallocatedIndex == POOL_PREALLOCATED_BATCH_SIZE) {
				preallocatedIndex = 0;
				preallocateNewBatch();
			}

			freshIdentifier.init(currentPage.content, pageOffset, length, probeInstance.hashCode);
			@Null var preexisting = internedIdentifiers.put(freshIdentifier, freshIdentifier);
			assert preexisting == null;
			return freshIdentifier;
		}
	}

	public static final Pool StaticPool = new Pool();

	private static final int POOL_INITIAL_SIZE = 512;
	private static final int POOL_PREALLOCATED_BATCH_SIZE= 128;
	private static final int CHAR_PAGE_SIZE = 1024;
	private static final int CHAR_PAGE_OVERFLOW = -1;
	private static final int CHAR_BUFFER_SIZE = 128;
}
