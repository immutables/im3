package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.*;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.google.common.reflect.TypeToken;

class Compound {
	// The opposite of order, the bigger - the higher priority.
	@Target(ElementType.TYPE_USE)
	@interface Priority {}

	public static final @Priority int DEFAULT_PRIORITY = 0;
	public static final @Priority int LOWEST_PRIORITY = Integer.MIN_VALUE;
	private final List<FactoryEntry> factories = new ArrayList<>();

	private static class FactoryEntry implements Comparable<FactoryEntry> {
		Codec.Factory factory;
		@Null
		Annotation qualifier;
		@Priority
		int priority = DEFAULT_PRIORITY;

		@Override
		public int compareTo(FactoryEntry o) {
			return o.priority - priority;
		}
	}

	public Compound add(Codec.Factory factory) {
		return add(factory, null, DEFAULT_PRIORITY);
	}

	public Compound add(Codec.Factory factory, @Null Annotation qualifier, @Priority int priority) {
		FactoryEntry e = new FactoryEntry();
		e.factory = factory;
		e.qualifier = qualifier;
		e.priority = priority;
		factories.add(e);
		Collections.sort(factories);
		return this;
	}

	static Object qualifierKey(@Null Annotation qualifier) {
		return qualifier != null ? qualifier : UNQUALIFIED;
	}

	private static final ThreadLocal<Set<TypeToken<?>>> resolveChain =
		ThreadLocal.withInitial(HashSet::new);

	private static class DeferredCodec<T> extends Codec<T> {
		private final TypeToken<T> token;
		private final Resolver resolver;
		private @Null Codec<T> codec;

		DeferredCodec(Resolver resolver, TypeToken<T> token) {
			this.resolver = resolver;
			this.token = token;
		}

		private Codec<T> advance() {
			return codec != null ? codec : (codec = resolver.get(token));
		}

		@Override
		public T decode(In in) throws IOException {
			return advance().decode(in);
		}

		@Override
		public void encode(Out out, T instance) throws IOException {
			advance().encode(out, instance);
		}
	}

	public Resolver toResolver() {
		return new Resolver() {
			private final Table<TypeToken<?>, Object, Codec<?>> memoised = HashBasedTable.create();
			private final List<FactoryEntry> factories =
				Ordering.natural().sortedCopy(Compound.this.factories);

			@SuppressWarnings("unchecked")
			@Override
			public <T> Codec<T> get(TypeToken<T> type, @Null Annotation qualifier) {
				if (resolveChain.get().add(type)) {
					try {
						Codec<T> codec = (Codec<T>) memoised.get(type, qualifierKey(qualifier));
						if (codec == null) {
							codec = findBestUncontested(type, qualifier);
							memoised.put(type, qualifierKey(qualifier), codec);
						}
						return codec;
					} finally {
						resolveChain.get().remove(type);
					}
				}
				return new DeferredCodec<>(this, type);
			}

			@SuppressWarnings("null") // cannot be null, bestEntry assigned with best
			private <T> Codec<T> findBestUncontested(TypeToken<T> type, @Null Annotation qualifier) {
				@Null List<FactoryEntry> contesters = null;
				@Null FactoryEntry bestEntry = null;
				@Null Codec<T> best = null;

				for (FactoryEntry e : factories) {
					// short circuit search if we already found something and priority is lower now
					if (bestEntry != null && bestEntry.priority > e.priority) break;

					Codec.Factory f = e.factory;
					if (Objects.equals(qualifier, e.qualifier)) {
						@Null Codec<T> c = f.get(this, type);
						if (c != null) {
							if (best != null) {
								assert bestEntry.priority == e.priority;
								if (contesters == null) {
									contesters = new ArrayList<>(2);
									contesters.add(bestEntry);
								}
								contesters.add(e);
							} else {
								best = c;
								bestEntry = e;
							}
						}
					}
				}
				if (contesters != null) {
					throw new RuntimeException(String.format(
						"More than one applicable adapter founds for %s @%s. Factories with priority %s: %s",
						type, qualifier, bestEntry.priority, contesters));
				}
				return best != null ? best : Codecs.unsupported(type, qualifier);
			}
		};
	}

	@Override
	public String toString() {
		return "Codec.Compound(" + factories.size() + " factories)";
	}

	private static final Object UNQUALIFIED = new Object();
}
