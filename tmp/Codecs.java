package io.immutables.codec;

import io.immutables.Capacity;
import io.immutables.Unreachable;
import io.immutables.codec.Codec.*;
import io.immutables.collect.Vect;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import org.immutables.data.Datatype;

public final class Codecs {
	private Codecs() {}

	public static @Null Annotation findQualifier(AnnotatedElement element) {
		return findQualifier(element.getAnnotations(), element);
	}

	public static @Null Annotation findQualifier(Annotation[] annotations, Object element) {
		@Null Annotation found = null;
		for (Annotation a : annotations) {
			if (a.annotationType().isAnnotationPresent(CodecQualifier.class)) {
				if (found != null) {
					throw new IllegalArgumentException(
							element + " has more than one annotation (@CodecQualifier): " + found + ", " + a);
				}
				found = a;
			}
		}
		return found;
	}

	public static Resolver.Compound builtin() {
		return new Resolver.Compound()
				.add(TEMPORAL, null, Resolver.Compound.LOWEST_PRIORITY + 2)
				.add(MISCELLANEOUS, null, Resolver.Compound.LOWEST_PRIORITY + 2)
				.add(SCALARS, null, Resolver.Compound.LOWEST_PRIORITY + 1)
				.add(ENUMS, null, Resolver.Compound.LOWEST_PRIORITY + 1)
				.add(COLLECTIONS, null, Resolver.Compound.LOWEST_PRIORITY + 1)
				.add(OPTIONALS, null, Resolver.Compound.LOWEST_PRIORITY + 1)
				.add(DATATYPES, null, Resolver.Compound.LOWEST_PRIORITY);
	}

	public static boolean isUnsupported(Codec<?> codec) {
		return codec instanceof UnsupportedCodec;
	}

	private static final class UnsupportedCodec<T> extends Codec<T> {
		private final TypeToken<T> type;
		private final Annotation qualifier;

		public UnsupportedCodec(TypeToken<T> type, Annotation qualifier) {
			this.type = type;
			this.qualifier = qualifier;
		}

		@Override
		public T decode(In in) {
			// TODO better reporting / message
			throw new UnsupportedOperationException();
		}

		@Override
		public void encode(Out out, T instance) {
			// TODO better reporting / message
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return "Codec.unsupported(" + type + (qualifier != null ? " @" + qualifier : "") + ")";
		}
	}

	private static final Codec.Factory DATATYPES = new Codec.Factory() {
		@Override
		public @Null <T> Codec<T> get(Resolver lookup, TypeToken<T> type) {
			@Null Datatype<T> t = null;
			try {
				t = Datatypes.construct(type);
			} catch (Exception cannotConstructDatatype) {
				// FIXME...
				cannotConstructDatatype.printStackTrace();
				return null;
			}
			if (!t.cases().isEmpty()) {
				return new DatatypeCaseCodec<>(t, lookup);
			}
			return new DatatypeCodec<>(t, lookup);
		}

		@Override
		public String toString() {
			return "Codec.Factory for Datatypes";
		}
	};

	private static final Codec.Factory ENUMS = new Codec.Factory() {
		@SuppressWarnings("unchecked")
		@Override
		public @Null <T> Codec<T> get(Resolver lookup, TypeToken<T> type) {
			Class<?> c = type.getRawType();
			if (c.isEnum()) {
				return (Codec<T>) new EnumCodec<>((Class<Enum<?>>) c, false);
			}
			return null;
		}

		@Override
		public String toString() {
			return "Codec.Factory for enums";
		}
	};

	public static <T> Codec<T> unsupported(TypeToken<T> type, @Null Annotation qualifier) {
		return new UnsupportedCodec<>(type, qualifier);
	}

	@FunctionalInterface
	interface CollectionConstructor {
		<E, C> C construct(Iterable<E> elements);
	}

	private static final Codec.Factory COLLECTIONS = new Codec.Factory() {
		private final Type listTypeParameter = List.class.getTypeParameters()[0];
		private final Type collTypeParameter = Collection.class.getTypeParameters()[0];
		private final Type setTypeParameter = Set.class.getTypeParameters()[0];
		private final Type vectTypeParameter = Vect.class.getTypeParameters()[0];
		private final Type mapKeyTypeParameter = Map.class.getTypeParameters()[0];
		private final Type mapValueTypeParameter = Map.class.getTypeParameters()[1];

		private final CollectionConstructor listConstructor = new CollectionConstructor() {
			@SuppressWarnings("unchecked")
			@Override
			public <E, C> C construct(Iterable<E> elements) {
				return (C) ImmutableList.copyOf(elements);
			}
		};

		private final CollectionConstructor setConstructor = new CollectionConstructor() {
			@SuppressWarnings("unchecked")
			@Override
			public <E, C> C construct(Iterable<E> elements) {
				return (C) ImmutableSet.copyOf(elements);
			}
		};

		private final CollectionConstructor vectConstructor = new CollectionConstructor() {
			@SuppressWarnings("unchecked")
			@Override
			public <E, C> C construct(Iterable<E> elements) {
				return (C) Vect.from(elements);
			}
		};

		@SuppressWarnings("unchecked") // runtime token + checks
		@Override
		public @Null <T> Codec<T> get(Resolver lookup, TypeToken<T> type) {
			Class<?> rawType = type.getRawType();
			if (rawType == List.class || rawType == ImmutableList.class) {
				Codec<?> codec = lookup.get(type.resolveType(listTypeParameter));
				return (Codec<T>) new ArrayCodec<>((Codec<Object>) codec, listConstructor);
			}
			if (rawType == Collection.class) {
				Codec<?> codec = lookup.get(type.resolveType(collTypeParameter));
				return (Codec<T>) new ArrayCodec<>((Codec<Object>) codec, listConstructor);
			}
			if (rawType == Set.class || rawType == ImmutableSet.class) {
				Codec<?> codec = lookup.get(type.resolveType(setTypeParameter));
				return (Codec<T>) new ArrayCodec<>((Codec<Object>) codec, setConstructor);
			}
			if (rawType == Vect.class) {
				Codec<?> codec = lookup.get(type.resolveType(vectTypeParameter));
				return (Codec<T>) new ArrayCodec<>((Codec<Object>) codec, vectConstructor);
			}
			if (rawType == Map.class || rawType == ImmutableMap.class) {
				Codec<?> forKey = lookup.get(type.resolveType(mapKeyTypeParameter));
				Codec<?> forValue = lookup.get(type.resolveType(mapValueTypeParameter));
				return (Codec<T>) new MapCodec<>(
						(Codec<Object>) forKey,
						(Codec<Object>) forValue,
						MapCodec.immutableMapSupplier());
			}
			return null;
		}

		@Override
		public String toString() {
			return "Codec.Factory for List<T>, Set<T>, Map<K,V>";
		}
	};

	public static final class MapCodec<K, V, M extends Map<K, V>> extends Codec<M> {
		private final Codec<K> forKey;
		private final Codec<V> forValue;
		private final Supplier<MapBuilder<K, V, M>> builderSupplier;

		public MapCodec(Codec<K> forKey, Codec<V> forValue, Supplier<MapBuilder<K, V, M>> builderSupplier) {
			this.forKey = forKey;
			this.forValue = forValue;
			this.builderSupplier = builderSupplier;
		}

		@Override
		public M decode(In in) throws IOException {
			FieldIndex fields = Codec.arbitraryFields();
			in.beginStruct(fields);
			MapBuilder<K, V, M> builder = builderSupplier.get();
			StringValueIo keyIo = new StringValueIo();
			while (in.hasNext()) {
				keyIo.putString(fields.indexToName(in.takeField()));
				builder.put(forKey.decode(keyIo), forValue.decode(in));
			}
			in.endStruct();
			return builder.build();
		}

		@Override
		public void encode(Out out, M instance) throws IOException {
			FieldIndex fields = Codec.arbitraryFields();
			out.beginStruct(fields);
			StringValueIo keyIo = new StringValueIo();
			for (Entry<K, V> e : instance.entrySet()) {
				forKey.encode(keyIo, e.getKey());
				out.putField(fields.nameToIndex(keyIo.takeString()));
				forValue.encode(out, e.getValue());
			}
			out.endStruct();
		}

		public interface MapBuilder<K, V, M extends Map<K, V>> {
			void put(K k, V v);
			M build();
		}

		public static <K, V> Supplier<MapBuilder<K, V, ImmutableMap<K, V>>> immutableMapSupplier() {
			return () -> new MapBuilder<>() {
				final ImmutableMap.Builder<K, V> b = ImmutableMap.builder();

				@Override
				public void put(K k, V v) {
					b.put(k, v);
				}

				@Override
				public ImmutableMap<K, V> build() {
					return b.build();
				}
			};
		}
	}

	public static class ArrayCodec<E, C extends Iterable<E>> extends ContainerCodec<E, C> {
		private final Codec<E> elementCodec;
		private final CollectionConstructor constructor;

		public ArrayCodec(Codec<E> elementCodec, CollectionConstructor constructor) {
			this.elementCodec = elementCodec;
			this.constructor = constructor;
		}

		@Override
		public C decode(In in) throws IOException {
			List<E> elements = new ArrayList<>();
			in.beginArray();
			while (in.hasNext()) {
				elements.add(elementCodec.decode(in));
			}
			in.endArray();
			return constructor.construct(elements);
		}

		@Override
		public void encode(Out out, C instance) throws IOException {
			out.beginArray();
			for (E e : instance) {
				elementCodec.encode(out, e);
			}
			out.endArray();
		}

		@Override
		public Codec<E> element() {
			return elementCodec;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <R, K> ContainerCodec<R, K> withElement(Codec<R> newElementCodec) {
			return (ContainerCodec<R, K>) new ArrayCodec<>(newElementCodec, constructor);
		}
	}

	public static final class EnumCodec<E extends Enum<?>> extends Codec<E> implements NullAware {
		private final ImmutableBiMap<String, E> constants;
		private final boolean supportsNull;
		private EnumCodec<E> nullableCounterpart;
		private final Class<E> type;

		public EnumCodec(Class<E> type, boolean supportsNull) {
			this(type, indexConstants(type), supportsNull);
		}

		EnumCodec(Class<E> type, ImmutableBiMap<String, E> constants, boolean supportsNull) {
			this.type = type;
			this.constants = constants;
			this.supportsNull = supportsNull;
		}

		private static <E extends Enum<?>> ImmutableBiMap<String, E> indexConstants(Class<E> type) {
			var annotation = type.getAnnotation(FieldFormat.class);
			@Null CaseFormat format = annotation != null ? annotation.value() : null;

			ImmutableBiMap.Builder<String, E> builder = ImmutableBiMap.builder();
			for (E e : type.getEnumConstants()) {
				var name = e.name();
				if (format != null) {
					CaseFormat sourceFormat = CaseFormat.UPPER_UNDERSCORE;
					if (!name.contains("_") && !name.toUpperCase().equals(name)) {
						sourceFormat = Character.isUpperCase(name.charAt(0)) ? CaseFormat.UPPER_CAMEL : CaseFormat.LOWER_CAMEL;
					}
					name = sourceFormat.to(format, name);
				}
				builder.put(name, e);
			}
			return builder.build();
		}

		@Override
		public boolean supportsNull() {
			return supportsNull;
		}

		@Override
		public Codec<E> toNullable() {
			if (supportsNull) return this;
			return nullableCounterpart == null
					? nullableCounterpart = new EnumCodec<>(type, constants, true)
					: nullableCounterpart;
		}

		@Override
		public E decode(In in) throws IOException {
      if (in.peek() == At.NULL) {
        in.takeNull();
        return null;
      }
			String name = in.takeString().toString();
			E e = constants.get(name);
			if (e == null) in.unexpected(
					"Cannot read " + type + ". Was " + name + " while supported only " + constants.keySet());
			return e;
		}

		@Override
		public void encode(Out out, E instance) throws IOException {
      if (instance == null && supportsNull) {
        out.putNull();
        return;
      }
			String name = constants.inverse().get(instance);
			if (name == null) out.unexpected("Wrong instance of " + type + ": " + instance);
			out.putString(name);
		}
	}

	@SuppressWarnings({"unchecked", "raw"})
	private static final Codec.Factory TEMPORAL = new Codec.Factory() {
		private final ImmutableMap<Class<?>, Codec<?>> codes = ImmutableMap.<Class<?>, Codec<?>>builder()
				.put(Instant.class, parseStringifyCodec(Instant::parse, Instant::toString))
				.put(OffsetDateTime.class, parseStringifyCodec(OffsetDateTime::parse, OffsetDateTime::toString))
				.put(LocalDateTime.class, parseStringifyCodec(LocalDateTime::parse, LocalDateTime::toString))
				.put(LocalDate.class, parseStringifyCodec(LocalDate::parse, LocalDate::toString))
				.put(LocalTime.class, parseStringifyCodec(LocalTime::parse, LocalTime::toString))
				.put(Duration.class, parseStringifyCodec(Duration::parse, Duration::toString))
				.put(Period.class, parseStringifyCodec(Period::parse, Period::toString))
				.build();

		@Override public <T> Codec<T> get(Resolver lookup, TypeToken<T> type) {
			return (Codec<T>) codes.get(type.getRawType());
		}
	};

	@SuppressWarnings({"unchecked", "raw"})
	private static final Codec.Factory MISCELLANEOUS = new Codec.Factory() {
		private final ImmutableMap<Class<?>, Codec<?>> codes = ImmutableMap.<Class<?>, Codec<?>>builder()
				.put(URI.class, parseStringifyCodec(URI::create, URI::toString))
				.build();

		@Override public <T> Codec<T> get(Resolver lookup, TypeToken<T> type) {
			return (Codec<T>) codes.get(type.getRawType());
		}
	};

	private static <T> Codec<T> parseStringifyCodec(
			java.util.function.Function<String, T> parse,
			java.util.function.Function<T, String> stringify) {
		return new Codec<>() {
			@Override
			public T decode(In in) throws IOException {
				return parse.apply(in.takeString().toString());
			}

			@Override
			public void encode(Out out, T instance) throws IOException {
				out.putString(stringify.apply(instance));
			}
		};
	}

	private static final Codec.Factory SCALARS = new Codec.Factory() {
		private final ScalarCodec<Integer> forInt = new ScalarCodec<>(ScalarCodec.INT, false);
		private final ScalarCodec<Long> forLong = new ScalarCodec<>(ScalarCodec.LONG, false);
		private final ScalarCodec<Double> forDouble = new ScalarCodec<>(ScalarCodec.DOUBLE, false);
		private final ScalarCodec<Float> forFloat = new ScalarCodec<>(ScalarCodec.FLOAT, false);
		private final ScalarCodec<Number> forNumber = new ScalarCodec<>(ScalarCodec.DOUBLE, false);
		private final ScalarCodec<Boolean> forBoolean = new ScalarCodec<>(ScalarCodec.BOOLEAN, false);
		private final ScalarCodec<String> forString = new ScalarCodec<>(ScalarCodec.STRING, false);

		private final ImmutableMap<Class<?>, Codec<?>> codecs = ImmutableMap.<Class<?>, Codec<?>>builder() // @formatter:off
				.put(int.class, forInt).put(Integer.class, forInt)
				.put(long.class, forLong).put(Long.class, forLong)
				.put(double.class, forDouble).put(Double.class, forDouble)
				.put(float.class, forFloat).put(Float.class, forFloat)
				.put(boolean.class, forBoolean).put(Boolean.class, forBoolean)
				.put(Number.class, forNumber)
				.put(String.class, forString)
				.build(); // @formatter:on

		@SuppressWarnings("unchecked")
		@Override
		public @Null <T> Codec<T> get(Resolver lookup, TypeToken<T> type) {
			return (Codec<T>) codecs.get(type.getRawType());
		}

		@Override
		public String toString() {
			return "Codec.Factory for int, long, double, boolean, String";
		}
	};

	// Albeit a lot of codecs for structures/objects will decide to encode decode
	// scalars by themselves, there's built-in codec for scalar (primitives + wrappers + string)
	// to implement reflective codecs.
	@SuppressWarnings("unchecked")
	static final class ScalarCodec<T> extends Codec<T> implements NullAware {
		static final int INT = 0;
		static final int LONG = 1;
		static final int FLOAT = 2;
		static final int DOUBLE = 3;
		static final int BOOLEAN = 4;
		static final int STRING = 5;

		private final boolean supportsNull;
		private final int type;
		private ScalarCodec<T> nullableCounterpart;

		ScalarCodec(int type, boolean supportsNull) {
			this.type = type;
			this.supportsNull = supportsNull;
		}

		@Override
		public boolean supportsNull() {
			return supportsNull;
		}

		@Override
		public Codec<T> toNullable() {
			if (supportsNull) return this;
			return nullableCounterpart == null
					? nullableCounterpart = new ScalarCodec<>(type, true)
					: nullableCounterpart;
		}

		// this call forces autoboxing of argument
		private T box(Object value) {
			return (T) value;
		}

		@Override
		public T decode(In in) throws IOException {
			if (supportsNull && in.peek() == At.NULL) {
				in.takeNull();
				return null;
			}
			switch (type) { // @formatter:off
			case INT: return box(in.takeInt());
			case LONG: return box(in.takeLong());
			case FLOAT: {
				Float f = (float) in.takeDouble();
				return (T) f;
			}
			case DOUBLE: return box(in.takeDouble());
			case BOOLEAN: return box(in.takeBoolean());
			case STRING: return box(in.takeString());
			default: throw Unreachable.exhaustive();
			} // @formatter:on
		}

		@Override
		public void encode(Out out, T instance) throws IOException {
			if (instance == null) {
				if (!supportsNull) {
					out.unexpected("codec doesn't support null values");
				}
				out.putNull();
				return;
			}
			switch (type) { // @formatter:off
			case INT: out.putInt(((Number) instance).intValue()); break;
			case LONG: out.putLong(((Number) instance).longValue()); break;
			case FLOAT:
			case DOUBLE: out.putDouble(((Number) instance).doubleValue()); break;
			case BOOLEAN: out.putBoolean((Boolean) instance); break;
			case STRING: out.putString(instance.toString()); break;
			} // @formatter:on
		}
	}

	private static Codec.Factory OPTIONALS = new Codec.Factory() {
		private final Type optionalTypeParameter = Optional.class.getTypeParameters()[0];

		@SuppressWarnings("unchecked") // runtime token + checks
		@Override
		public @Null <T> Codec<T> get(Resolver lookup, TypeToken<T> type) {
			Class<?> rawType = type.getRawType();
			if (rawType == Optional.class) {
				Codec<?> codec = lookup.get(type.resolveType(optionalTypeParameter));
				return (Codec<T>) new OptionalCodec<>((Codec<Object>) codec);
			}
			if (rawType == OptionalInt.class) {
				return (Codec<T>) new OptionalPrimitive(OptionalPrimitive.Variant.INT);
			}
			if (rawType == OptionalLong.class) {
				return (Codec<T>) new OptionalPrimitive(OptionalPrimitive.Variant.LONG);
			}
			if (rawType == OptionalDouble.class) {
				return (Codec<T>) new OptionalPrimitive(OptionalPrimitive.Variant.DOUBLE);
			}
			return null;
		}

		@Override
		public String toString() {
			return "Codec.Factory for Optional<E>, Optional{Int|Long|Double}";
		}
	};

	public static class OptionalPrimitive extends Codec<Object> implements NullAware {
		enum Variant {
			INT, LONG, DOUBLE
		}

		private final Variant type;

		public OptionalPrimitive(Variant type) {
			this.type = type;
		}

		@Override
		public Object decode(In in) throws IOException {
			if (in.peek() == At.NULL) {
				in.takeNull();
				switch (type) { // @formatter:off
				case INT: return OptionalInt.empty();
				case LONG: return OptionalLong.empty();
				case DOUBLE: return OptionalDouble.empty();
				default: throw Unreachable.exhaustive();
				}
			}
			switch (type) { // @formatter:off
			case INT: return OptionalInt.of(in.takeInt());
			case LONG: return OptionalLong.of(in.takeLong());
			case DOUBLE: return OptionalDouble.of(in.takeDouble());
			default: throw Unreachable.exhaustive();
			} // @formatter:on
		}

		@Override
		public void encode(Out out, Object instance) throws IOException {
			if (instance != null) {
				switch (type) {
				case INT:
					OptionalInt i = ((OptionalInt) instance);
					if (i.isPresent()) {
						out.putInt(i.getAsInt());
						return;
					}
					break;
				case LONG:
					OptionalLong l = ((OptionalLong) instance);
					if (l.isPresent()) {
						out.putLong(l.getAsLong());
						return;
					}
					break;
				case DOUBLE:
					OptionalDouble d = ((OptionalDouble) instance);
					if (d.isPresent()) {
						out.putDouble(d.getAsDouble());
						return;
					}
					break;
				}
			}
			// every present case should return above
			out.putNull();
		}

		@Override
		public boolean supportsNull() {
			return true;
		}

		@Override
		public String toString() {
			switch (type) { // @formatter:off
			case INT: return "Codec<OptionalInt>";
			case LONG: return "Codec<OptionalLong>";
			case DOUBLE: return "Codec<OptionalDouble>";
			default: throw Unreachable.exhaustive();
			} // @formatter:on
		}
	}

	public static class OptionalCodec<E> extends ContainerCodec<E, Optional<E>> implements NullAware {
		private final Codec<E> elementCodec;

		public OptionalCodec(Codec<E> elementCodec) {
			this.elementCodec = elementCodec;
		}

		@Override
		public Optional<E> decode(In in) throws IOException {
			if (in.peek() == At.NULL) {
				in.takeNull();
				return Optional.empty();
			}
			return Optional.ofNullable(elementCodec.decode(in)); // Optional.of ?
		}

		@Override
		public boolean supportsNull() {
			return true;
		}

		@Override
		public void encode(Out out, Optional<E> instance) throws IOException {
			if (instance != null && instance.isPresent()) {
				elementCodec.encode(out, instance.get());
			} else {
				out.putNull();
			}
		}

		@Override
		public String toString() {
			return "Codec<Optional<E>> for " + elementCodec;
		}

		@Override
		public Codec<E> element() {
			return elementCodec;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <R, K> ContainerCodec<R, K> withElement(Codec<R> newElement) {
			return (ContainerCodec<R, K>) new OptionalCodec<>(newElement);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object & In & Out> T stringIo() {
		return (T) new StringValueIo();
	}

	private static class StringValueIo implements In, Out {
		private @Null String value;

		private void onlyStringExpected() throws IOException {
			unexpected("Only takeString() expected");
		}

		@Override
		public String getPath() {
			return "$0";
		}

		@Override
		public void expect(boolean condition, Supplier<String> message) throws IOException {
			if (!condition) throw new IOException(message.get());
		}

		@Override
		public Object adapts() {
			return value;
		}

		@Override
		public int takeInt() throws IOException {
			return Integer.parseInt(value);
		}

		@Override
		public long takeLong() throws IOException {
			return Long.parseLong(value);
		}

		@Override
		public double takeDouble() throws IOException {
			return Long.parseLong(value);
		}

		@Override
		public boolean takeBoolean() throws IOException {
			return Boolean.parseBoolean(value);
		}

		@Override
		public void takeNull() throws IOException {
			if (value != null) onlyStringExpected();
		}

		@Override
		public void skip() throws IOException {
			value = null;
		}

		@Override
		public CharSequence takeString() throws IOException {
			expectStringValue();
			return value;
		}

		private void expectStringValue() throws IOException {
			if (value == null) unexpected("no string available");
		}

		@Override
		public Object takeSpecial() throws IOException {
			return value;
		}

		@Override
		public boolean hasNext() throws IOException {
			onlyStringExpected();
			return false;
		}

		@Override
		public void beginArray() throws IOException {
			onlyStringExpected();
		}

		@Override
		public void endArray() throws IOException {
			onlyStringExpected();
		}

		@Override
		public @Field int takeField() throws IOException {
			onlyStringExpected();
			throw Unreachable.contractual();
		}

		@Override
		public void endStruct() throws IOException {
			onlyStringExpected();
		}

		@Override
		public void putInt(int i) throws IOException {
			value = String.valueOf(i);
		}

		@Override
		public void putLong(long l) throws IOException {
			value = String.valueOf(l);
		}

		@Override
		public void putDouble(double d) throws IOException {
			value = String.valueOf(d);
		}

		@Override
		public void putBoolean(boolean b) throws IOException {
			value = String.valueOf(b);
		}

		@Override
		public void putSpecial(Object o) throws IOException {
			value = o.toString();
		}

		@Override
		public void putNull() throws IOException {
			value = null;
		}

		@Override
		public void putString(CharSequence s) throws IOException {
			value = s.toString();
		}

		@Override
		public void putField(@Field int field) throws IOException {
			onlyStringExpected();
		}

		@Override
		public At peek() throws IOException {
			return value == null ? At.NULL : At.STRING;
		}

		@Override
		public void beginStruct(FieldIndex f) throws IOException {
			onlyStringExpected();
		}
	}

	private static abstract class ForwardingBase implements Err, Adapter {
		private FieldIndex[] mappersStack = new FieldIndex[4];
		private int mapperCount;

		protected abstract Err delegate();

		protected final FieldIndex topMapper() {
			return mappersStack[mapperCount - 1];
		}

		final void pushMapper(FieldIndex mapper) {
			mappersStack = Capacity.ensure(mappersStack, mapperCount, 1);
			mappersStack[mapperCount++] = mapper;
		}

		final void popMapper() {
			mappersStack[--mapperCount] = null;
		}

		@Override
		public String getPath() {
			return delegate().getPath();
		}

		@Override
		public void expect(boolean condition, Supplier<String> message) throws IOException {
			delegate().expect(condition, message);
		}

		@Override
		public void unexpected(String message) throws IOException {
			delegate().unexpected(message);
		}

		@Override final public Object adapts() {
			Err d = delegate();
			return d instanceof Adapter ? ((Adapter) d).adapts() : d;
		}
	}

	public static abstract class ForwardingOut extends ForwardingBase implements Out {
		@Override
		protected abstract Out delegate();

		@Override
		public void putInt(int i) throws IOException {
			delegate().putInt(i);
		}

		@Override
		public void putLong(long l) throws IOException {
			delegate().putLong(l);
		}

		@Override
		public void putDouble(double d) throws IOException {
			delegate().putDouble(d);
		}

		@Override
		public void putBoolean(boolean b) throws IOException {
			delegate().putBoolean(b);
		}

		@Override
		public void putSpecial(Object o) throws IOException {
			delegate().putSpecial(o);
		}

		@Override
		public void putNull() throws IOException {
			delegate().putNull();
		}

		@Override
		public void putString(CharSequence s) throws IOException {
			delegate().putString(s);
		}

		@Override
		public void endArray() throws IOException {
			delegate().endArray();
		}

		@Override
		public void beginArray() throws IOException {
			delegate().beginArray();
		}

		@Override
		public void beginStruct(FieldIndex f) throws IOException {
			pushMapper(f);
			delegate().beginStruct(f);
		}

		@Override
		public void putField(@Field int field) throws IOException {
			delegate().putField(field);
		}

		@Override
		public void endStruct() throws IOException {
			delegate().endStruct();
			popMapper();
		}
	}

	public static abstract class ForwardingIn extends ForwardingBase implements In {
		@Override
		protected abstract In delegate();

		@Override
		public At peek() throws IOException {
			return delegate().peek();
		}

		@Override
		public int takeInt() throws IOException {
			return delegate().takeInt();
		}

		@Override
		public long takeLong() throws IOException {
			return delegate().takeLong();
		}

		@Override
		public double takeDouble() throws IOException {
			return delegate().takeDouble();
		}

		@Override
		public boolean takeBoolean() throws IOException {
			return delegate().takeBoolean();
		}

		@Override
		public void takeNull() throws IOException {
			delegate().takeNull();
		}

		@Override
		public void skip() throws IOException {
			delegate().skip();
		}

		@Override
		public CharSequence takeString() throws IOException {
			return delegate().takeString();
		}

		@Override
		public Object takeSpecial() throws IOException {
			return delegate().takeSpecial();
		}

		@Override
		public boolean hasNext() throws IOException {
			return delegate().hasNext();
		}

		@Override
		public @Field int takeField() throws IOException {
			return delegate().takeField();
		}

		@Override
		public void endArray() throws IOException {
			delegate().endArray();
		}

		@Override
		public void beginArray() throws IOException {
			delegate().beginArray();
		}

		@Override
		public void beginStruct(FieldIndex f) throws IOException {
			pushMapper(f);
			delegate().beginStruct(f);
		}

		@Override
		public void endStruct() throws IOException {
			delegate().endStruct();
			popMapper();
		}
	}

  public static class BufferOut implements Out {

    private final List<At> tokens = new ArrayList<>();
    private final List<Object> values = new ArrayList<>();

    private FieldIndex[] mappersStack = new FieldIndex[4];
    private int mapperCount;

    private FieldIndex topMapper() {
      return mappersStack[mapperCount - 1];
    }

    private void pushMapper(FieldIndex mapper) {
      mappersStack = Capacity.ensure(mappersStack, mapperCount, 1);
      mappersStack[mapperCount++] = mapper;
    }

    private void popMapper() {
      mappersStack[--mapperCount] = null;
    }

    @Override
    public void putInt(int i) {
      tokens.add(At.INT);
      values.add(i);
    }

    @Override
    public void putLong(long l) {
      tokens.add(At.LONG);
      values.add(l);
    }

    @Override
    public void putDouble(double d) {
      tokens.add(At.DOUBLE);
      values.add(d);
    }

    @Override
    public void putBoolean(boolean b) {
      tokens.add(At.BOOLEAN);
      values.add(b);
    }

    @Override
    public void putSpecial(Object o) {
      tokens.add(null);
      values.add(o);
    }

    @Override
    public void putNull() {
      tokens.add(At.NULL);
      values.add(null);
    }

    @Override
    public void putString(CharSequence s) {
      tokens.add(At.STRING);
      values.add(s);
    }

    @Override
    public void endArray() {
      tokens.add(At.ARRAY_END);
      values.add(null);
    }

    @Override
    public void beginArray() {
      tokens.add(At.ARRAY);
      values.add(null);
    }

    @Override
    public void beginStruct(FieldIndex f) {
      tokens.add(At.STRUCT);
      values.add(null);
      pushMapper(f);
    }

    @Override
    public void putField(@Field int field) {
      tokens.add(At.FIELD);
      values.add(topMapper().indexToName(field));
    }

    @Override
    public void endStruct() {
      tokens.add(At.STRUCT_END);
      values.add(null);
      popMapper();
    }

    @Override
    public String getPath() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void expect(boolean condition, Supplier<String> message) throws IOException {
      if (!condition) throw new IOException(message.get());
    }

    @Override
    public @Null Object adapts() {
      return null;
    }

    public In toIn() {
      return new In() {
        private int index = -1;

        private PathBuilder path = new PathBuilder();

        private FieldIndex[] mappersStack = new FieldIndex[4];
        private int mapperCount;

        private FieldIndex topMapper() {
          return mappersStack[mapperCount - 1];
        }

        private void pushMapper(FieldIndex mapper) {
          mappersStack = Capacity.ensure(mappersStack, mapperCount, 1);
          mappersStack[mapperCount++] = mapper;
        }

        private void popMapper() {
          mappersStack[--mapperCount] = null;
        }

        @Override
        public At peek() {
          if (index + 1 >= tokens.size()) return At.EOF;
          return tokens.get(index + 1);
        }

        @Override
        public int takeInt() throws IOException {
          At peek = peek();
          expect(peek == At.INT || peek == At.LONG || peek == At.DOUBLE, () -> "not a number value " + getPath());
          Object o = values.get(++index);
          path.onValue();
          switch (peek) {
            case INT:
              return ((Integer) o);
            case LONG:
              return ((Long) o).intValue();
            case DOUBLE:
              return ((Double) o).intValue();
            default:
              throw Unreachable.exhaustive();
          }
        }

        @Override
        public long takeLong() throws IOException {
          At peek = peek();
          expect(peek == At.INT || peek == At.LONG || peek == At.DOUBLE, () -> "not a number value " + getPath());
          Object o = values.get(++index);
          path.onValue();
          switch (peek) {
            case INT:
              return ((Integer) o).longValue();
            case LONG:
              return (Long) o;
            case DOUBLE:
              return ((Double) o).longValue();
            default:
              throw Unreachable.exhaustive();
          }
        }

        @Override
        public double takeDouble() throws IOException {
          At peek = peek();
          expect(peek == At.INT || peek == At.LONG || peek == At.DOUBLE, () -> "not a number value " + getPath());
          Object o = values.get(++index);
          path.onValue();
          switch (peek) {
            case INT:
              return ((Integer) o).doubleValue();
            case LONG:
              return ((Long) o).doubleValue();
            case DOUBLE:
              return (Double) o;
            default:
              throw Unreachable.exhaustive();
          }
        }

        @Override
        public boolean takeBoolean() throws IOException {
          expect(peek() == At.BOOLEAN, () -> "not at boolean value " + getPath());
          path.onValue();
          return (boolean) values.get(++index);
        }

        @Override
        public void takeNull() throws IOException {
          expect(peek() == At.NULL, () -> "not at null value " + getPath());
          path.onValue();
          index++;
        }

        @Override
        public void skip() throws IOException {
          expect(peek() != At.EOF, () -> "can not skip: EOF");
          index++;
        }

        @Override
        public CharSequence takeString() throws IOException {
          expect(peek() == At.STRING, () -> "not at string value " + getPath());
          path.onValue();
          return (CharSequence) values.get(++index);
        }

        @Override
        public Object takeSpecial() {
          path.onValue();
          return values.get(++index);
        }

        @Override
        public boolean hasNext() {
          return peek() != At.ARRAY_END && peek() != At.STRUCT_END && peek() != At.EOF;
        }

        @Override
        public void beginArray() throws IOException {
          expect(peek() == At.ARRAY, () -> "not at beginning of array " + getPath());
          index++;
          path.beginArray();
        }

        @Override
        public void endArray() throws IOException {
          expect(peek() == At.ARRAY_END, () -> "not at the end of array " + getPath());
          index++;
          path.endArray();
        }

        @Override
        public void beginStruct(FieldIndex f) throws IOException {
          expect(peek() == At.STRUCT, () -> "not at beginning of struct " + getPath());
          index++;
          pushMapper(f);
          path.beginStruct();
        }

        @Override
        public @Field int takeField() throws IOException {
          expect(peek() == At.FIELD, () -> "not at field " + getPath());
          CharSequence name = (CharSequence) values.get(++index);
          path.onField(name.toString());
          return topMapper().nameToIndex(name);
        }

        @Override
        public void endStruct() throws IOException {
          expect(peek() == At.STRUCT_END, () -> "not at the end of the struct " + getPath());
          index++;
          popMapper();
          path.endStruct();
        }

        @Override
        public @Null Object adapts() {
          return null;
        }

        @Override
        public String getPath() {
          return path.build();
        }

        @Override
        public void expect(boolean condition, Supplier<String> message) throws IOException {
          if (!condition) throw new IOException(message.get());
        }
      };
    }

    private class PathBuilder {

      private Stack<String> path = new Stack<>();
      private Stack<Level> levels = new Stack<>();
      private Stack<Integer> arrayIndexes = new Stack<>();
      private String currentField = null;

      public void beginStruct() {
        Level current = peekOrNull(levels);
        if (current == Level.A) {
          Integer index = arrayIndexes.pop();
          arrayIndexes.push(index + 1);
          path.push("[" + (index + 1) + "]");
        }
        if (current == Level.S) {
          path.push(currentField);
        }
        currentField = null;
        levels.push(Level.S);
      }

      public void beginArray() {
        Level current = peekOrNull(levels);
        if (current == Level.A) {
          Integer index = arrayIndexes.pop();
          arrayIndexes.push(index + 1);
          path.push("[" + (index + 1) + "]");
        }
        if (current == Level.S) {
          path.push(currentField);
        }
        currentField = null;
        levels.push(Level.A);
        arrayIndexes.push(-1);
      }

      public void endStruct() {
        levels.pop();
        currentField = null;
        if (!path.isEmpty()) {
          path.pop();
        }
      }

      public void endArray() {
        levels.pop();
        arrayIndexes.pop();
        if (!path.isEmpty()) {
          path.pop();
        }
      }

      public void onField(String name) {
        currentField = "." + name;
      }

      public void onValue() {
        if (peekOrNull(levels) == Level.A) {
          Integer index = arrayIndexes.pop();
          arrayIndexes.push(index + 1);
        }
      }

      public String build() {
        return "$" + String.join("", path) +
            (peekOrNull(levels) == Level.A ? ("[" + (arrayIndexes.peek() + 1) + "]") : "") +
            (peekOrNull(levels) == Level.S && currentField != null ? currentField : "");
      }

      private <T> T peekOrNull(Stack<T> stack) {
        return stack.size() > 0 ? stack.peek() : null;
      }

    }

    private enum Level {S, A}

  }

}
