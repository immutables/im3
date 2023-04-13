package io.immutables.codec;

import java.io.IOException;

public abstract class Out {
	public abstract NameIndex index(String... known);

	public abstract void putNull() throws IOException;

	public abstract void putInt(int i) throws IOException;

	public abstract void putLong(long l) throws IOException;

	public abstract void putDouble(double d) throws IOException;

	public abstract void putBoolean(boolean b) throws IOException;

	public abstract void putString(String s) throws IOException;

	public abstract void putChars(char[] chars, int offset, int length) throws IOException;

	public abstract void putString(NameIndex names, int index) throws IOException;

	public abstract void beginArray() throws IOException;

	public abstract void endArray() throws IOException;

	public abstract void beginStruct(NameIndex names) throws IOException;

	public abstract void putField(int index) throws IOException;

	public abstract void putField(String name) throws IOException;

	public abstract void endStruct() throws IOException;
}
