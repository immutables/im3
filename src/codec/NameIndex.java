package io.immutables.codec;

public abstract class NameIndex {
	// enum Unknown { Ban, Allow, Skip }
	public static final int UNKNOWN = -1;

	public abstract String[] known();

	public abstract int index(String name);

	public abstract String name(int index);

	public static NameIndex unknown() {
		return new NameIndex() {
			final String[] known = new String[0];

			public String[] known() {
				return known;
			}

			public int index(String name) {
				return -1;
			}

			public String name(int index) {
				throw new ArrayIndexOutOfBoundsException(index);
			}

			public String toString() {
				return NameIndex.class.getSimpleName() + ".unknown()";
			}
		};
	}

	public static NameIndex known(String... strings) {
		return new NameIndex() {
			final String[] known = strings;

			public String[] known() {
				return known.clone();
			}

			public int index(String name) {
				int i = 0;
				for (var n : known) {
					if (n.equals(name)) return i;
					i++;
				}
				return UNKNOWN;
			}

			public String name(int index) {
				return known[index];
			}

			public String toString() {
				return NameIndex.class.getSimpleName() + ".known()";
			}
		};
	}
}
