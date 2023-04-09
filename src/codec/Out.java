package io.immutables.codec;

import java.io.IOException;

public abstract class Out extends StreamBase {

	public abstract void putNull() throws IOException;

	public abstract void putInt(int i) throws IOException;

	public abstract void putLong(long l) throws IOException;

	public abstract void putDouble(double d) throws IOException;

	public abstract void putBoolean(boolean b) throws IOException;

	public abstract void putString(CharSequence s) throws IOException;

	public abstract void putChars(char[] chars, int offset, int length) throws IOException;

	public abstract void beginArray() throws IOException;

	public abstract void endArray() throws IOException;

	public abstract void beginStruct() throws IOException;//FieldIndex f

	public abstract void putField(int fieldIndex) throws IOException;

	public abstract void endStruct() throws IOException;
}
