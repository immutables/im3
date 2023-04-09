package io.immutables.codec;

import io.immutables.common.Unreachable;
import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static io.immutables.codec.Types.requireSpecificTypes;
import static java.util.Objects.requireNonNull;

/**
 * Codec registry, which is a main/reference implementation of {@link Codec.Resolver}.
 * Unlikely that any other implementation of {@code Code.Resolver} will exist
 * or will be needed, aside from any interceptors/proxies.
 */
public class Registry implements Codec.Resolver {

	private final Map<Class<?>, FactoriesByType> factories;
	private final ConcurrentMap<MemoKey, Codec<?, ?, ?>> memoisedCodecs = new ConcurrentHashMap<>();

	private record MemoKey(Type type, Medium<?, ?> medium) {}

	private record Entry(
		Codec.Factory<?, ?> factory,
		Medium<?, ?> medium,
		Class<?>[] rawTypes
	) {}

	private Registry(List<Entry> entries) {
		this.factories = Map.copyOf(distributeByRawTypes(entries));
	}

	public <T, I extends In, O extends Out>
	Optional<Codec<T, I, O>> resolve(Type type, Medium<I, O> medium) {
		requireNonNull(medium);
		requireSpecificTypes(type);
		// Lookup callback used from within codec factories/codecs to lookup
		// codecs they delegate to for their fields etc.
		Codec.Lookup<I, O> lookup = new Codec.Lookup<>() {
			public <W> Codec<W, I, O> get(Type t) {
				@Null Codec<W, I, O> codec = resolve(t, medium, this);
				if (codec != null) return codec;
				throw new NoSuchElementException("No such codec for type " + t);
			}
		};
		return Optional.ofNullable(resolve(type, medium, lookup));
	}

	private static final ThreadLocal<Nesting> resolutionNesting = new ThreadLocal<>();

	// this class instance is accessed by only one thread,
	// and it's stored in a thread-local slot
	private static class Nesting {
		final Map<Type, ForType> byType = new HashMap<>();

		private static class ForType {
			final List<Deferred<?, ?, ?>> deferred = new ArrayList<>();
		}
	}

	// this is fundamental lookup procedure for the codecs.
	// probably it should not be decomposed into smaller functions,
	// as with all these try/finally it will break a flow
	@SuppressWarnings("unchecked")
	// safe unchecked: Guarded by DSL,
	// but requires Codec/Factory implementors to not trick either
	private <T, I extends In, O extends Out> @Null Codec<T, I, O> resolve(
		Type type, Medium<I, O> medium, Codec.Lookup<I, O> lookup) {

		var memoKey = new MemoKey(type, medium);

		// short circuit on memoised codec early on
		@Null var memoised = memoisedCodecs.get(memoKey);
		if (memoised != null) return (Codec<T, I, O>) memoised;

		@Null var nestingOnEntry = resolutionNesting.get();
		Nesting nesting;
		if (nestingOnEntry != null) {
			nesting = nestingOnEntry;
		} else {
			nesting = new Nesting();
			resolutionNesting.set(nesting);
		}

		try {
			@Null var forType = nesting.byType.get(type);
			if (forType != null) {
				// creating and returning and instance of deferred assumes that the codec
				// for type is present, otherwise we wouldn't have any cycles/recursion.
				// before returning we add it, so we can quickly actualize deferred with
				// actual instance once completed with the initial - actual codec.
				// this would avoid unnecessary late (out of band) lookups from the deferred
				// instance lazily
				var deferred = new Deferred<T, I, O>(type, lookup);
				forType.deferred.add(deferred);
				// and we don't memoise it!
				return deferred;
			}

			// we're starting fresh lookup with no cycles in sight
			// and so any possible cycle will have deferred codec returned
			forType = new Nesting.ForType();
			nesting.byType.put(type, forType);

			@Null Codec<T, I, O> codec = null;
			try {
				@Null var byType = factories.get(toRawType(type));
				if (byType != null) {
					@Null var factory = (Codec.Factory<I, O>) byType.lookup(medium);
					if (factory != null) {
						codec = (Codec<T, I, O>) factory.tryCreate(type, medium);
						if (codec != null) {
							memoisedCodecs.put(memoKey, codec);
							return codec;
						}
					}
				}
				return null;

			} finally {
				// here we actualize deferred instances to avoid lazy lookups from deferred
				if (codec != null) {
					for (var d : forType.deferred) {
						// this would not throw as just assigning references
						((Deferred<T, I, O>) d).setActual(codec);
					}
				}
				// and removing entry, since we've added it
				nesting.byType.remove(type);
			}

		} finally {
			if (nestingOnEntry == null) {
				// we created nesting on entry, so we'll clean it up after
				resolutionNesting.remove();
			}
		}
	}

	private static Class<?> toRawType(Type type) {
		return switch (type) {
			case Class<?> c -> c;
			case ParameterizedType p -> (Class<?>) p.getRawType();
			default -> throw Unreachable.contractual(); // requireSpecificTypes
		};
	}

	public <T, I extends In, O extends Out>
	Optional<Codec<T, I, O>> memoising(Type type, Medium<I, O> medium) {
		return Optional.empty();
	}

	private static class Deferred<T, I extends In, O extends Out> extends Codec<T, I, O> {
		private final Type type;
		private @Null Lookup<I, O> lookup;
		private @Null Codec<T, I, O> actual;

		Deferred(Type type, Lookup<I, O> lookup) {
			this.type = type;
			this.lookup = lookup;
		}

		public void encode(O out, T instance) throws IOException {
			actual().encode(out, instance);
		}

		public @Null T decode(I in) throws IOException {
			return actual().decode(in);
		}

		private Codec<T, I, O> actual() {
			if (actual == null) {
				assert lookup != null;
				setActual(lookup.get(type));
				assert actual != null;
			}
			return actual;
		}

		void setActual(Codec<T, I, O> actual) {
			this.actual = actual;
			this.lookup = null; // not sure that this helps much, but it will release that context lookup
		}
	}

	public static final class Builder {
		private final List<Entry> entries = new ArrayList<>();
		private boolean useBuiltin = true;

		public <I extends In, O extends Out> Builder add(
			Codec.Factory<I, O> factory, Medium<I, O> medium, Class<?>... rawTypes) {
			entries.add(new Entry(factory, medium, rawTypes));
			return this;
		}

		public Builder noBuiltin() {
			useBuiltin = false;
			return this;
		}

		public Registry build() {
			if (useBuiltin) {
				//entries.add(0, new Entry());
			}
			return new Registry(entries);
		}
	}

	private static Map<Class<?>, FactoriesByType> distributeByRawTypes(List<Entry> entries) {
		var factories = new HashMap<Class<?>, FactoriesByType>();
		for (var e : entries) {
			for (var r : e.rawTypes) {
				factories.computeIfAbsent(r, k -> new FactoriesByType())
					.add(e);
			}
		}
		return factories;
	}

	// Models a "smart entry" for factories,
	// differentiates and overrides by medium and looks up accordingly
	private static final class FactoriesByType {
		private @Null Entry anyMedium;
		// this is by design a list, not a map, number of different Medium
		// will be small in any reasonable system
		private @Null List<Entry> forMediums;
		// so far we track overridden entries only for troubleshooting,
		// but might allow access to it later
		private @Null List<Entry> overridden;

		void add(Entry e) {
			if (e.medium == Medium.Any) {
				if (anyMedium != null) addOverridden(anyMedium);
				anyMedium = e;
			} else {
				forMedium(e);
			}
		}

		@Null Codec.Factory<?, ?> lookup(Medium<?, ?> medium) {
			// trying first a specific medium
			if (forMediums != null) for (var m : forMediums) {
				if (m.medium == medium) {
					return m.factory;
				}
			}
			// resort to any media, which is not guaranteed either
			return anyMedium != null ? anyMedium.factory : null;
		}

		private void forMedium(Entry e) {
			if (forMediums == null) forMediums = new ArrayList<>();
			displaceOverridden(e.medium);
			forMediums.add(e);
		}

		private void addOverridden(Entry e) {
			if (overridden == null) overridden = new ArrayList<>();
			overridden.add(e);
		}

		private void displaceOverridden(Medium<?, ?> medium) {
			assert forMediums != null;
			for (Iterator<Entry> it = forMediums.iterator(); it.hasNext(); ) {
				var m = it.next();
				if (m.medium == medium) {
					addOverridden(m);
					it.remove();
				}
			}
		}
	}
}
