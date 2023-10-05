package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import static io.immutables.codec.Types.isRewrapped;
import static io.immutables.codec.Types.requireSpecific;
import static java.util.Objects.requireNonNull;

/**
 * Codec registry, which is a main reference implementation of {@link Codec.Resolver}.
 * Unlikely that any other implementation of {@code Code.Resolver} will exist
 * or will be needed, aside from any interceptors/proxies.
 */
public class Registry implements Codec.Resolver {
	/**
	 * {@code Fallback} is a synthetic medium used to register fallback factories.
	 * In all respects, it is an equivalent to {@link Medium#Any}
	 */
	private static final Medium<In, Out> Fallback = new Medium<>() {
		public String toString() {
			return Registry.class.getSimpleName() + ".Fallback";
		}
	};

	private final Map<Class<?>, List<Entry>> factoriesByType;
	private final List<Entry> factoriesCatchAll;
	private final ConcurrentMap<MemoKey, Codec<?, ?, ?>> memoisedCodecs = new ConcurrentHashMap<>();

	private record MemoKey(Type type, Medium<?, ?> medium) {}

	private record Entry(
		Codec.Factory<?, ?> factory,
		Medium<?, ?> medium,
		Class<?>[] rawTypes
	) {}

	private Registry(List<Entry> entries) {
		var catchAll = new ArrayList<Entry>();
		var byType = new HashMap<Class<?>, List<Entry>>();
		for (var e : entries) {
			if (e.rawTypes.length == 0) {
				// matching will be done in reverse,
				catchAll.add(0, e);
			} else for (var r : e.rawTypes) {
				byType.computeIfAbsent(r, k -> new ArrayList<>()).add(0, e);
			}
		}
		this.factoriesByType = byType;
		this.factoriesCatchAll = catchAll;
	}

	/**
	 * Same as {@link #resolve(Type, Medium)}, but with more convenience and type safety
	 * for non-generic types.
	 */
	public <T, I extends In, O extends Out>
	Optional<Codec<T, I, O>> resolve(Class<? extends T> type, Medium<I, O> medium) {
		return resolve((Type) type, medium);
	}

	@Override public <T, I extends In, O extends Out>
	Optional<Codec<T, I, O>> resolve(Type type, Medium<I, O> medium) {
		requireNonNull(medium);
		if (medium == Medium.Any) throw new IllegalArgumentException(
			"%s is only for registering/matchting codec factories, not for resolving codecs"
				.formatted(Medium.Any));
		// it is very important that we not only validated type for specific
		// but also re-wrapped all JVM provided reflected ParameterizedTypes to
		// our own implementation. JVM ones are never equal to custom implementations,
		// so we cannot mix them in maps/sets etc.
		type = requireSpecific(type);

		// Lookup callback used from within codec factories/codecs to lookup
		// codecs they delegate to for their fields etc.
		Codec.Lookup<I, O> lookup = new Codec.Lookup<>() {
			public <W> Codec<W, I, O> get(Type type) {
				assert isRewrapped(type); // assert because these calls can be made by "indecent" codecs
				@Null Codec<W, I, O> codec = Registry.this.resolve(type, medium, this);
				if (codec != null) return codec;
				// fallback codec used only to delegate from Codec.Lookup
				// externally we will not resolve these
				@SuppressWarnings("unchecked") // TODO explain safety
				@Null Codec<W, I, O> fallbackCodec =
						Registry.this.resolve(type, (Medium<I, O>) Fallback, this);
				if (fallbackCodec != null) return fallbackCodec;
				throw new NoSuchElementException("No such codec for type " + type);
			}

			@Override
			public <W, IN extends In, OUT extends Out> Optional<Codec<W, IN, OUT>> resolve(
					Type type, Medium<IN, OUT> medium) {
				return Registry.this.resolve(type, medium);
			}
		};
		return Optional.ofNullable(resolve(type, medium, lookup));
	}

	// holds thread local context, called Nesting which is used to detect
	// and resolve cycles when recursive data structures are used
	// such resolution will either succeed or we will have runtime exception
	// either as codecs not found during resolution
	// or during decoding-encoding (if used out-of-band)
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
			// are we looking for this type already (recursive cycle)?
			if (forType != null) {
				// creating and returning and instance of deferred assumes that the codec
				// for type is present, otherwise we wouldn't have any cycles/recursion.
				// before returning we add it, so we can quickly actualize deferred with
				// actual instance once completed with the initial - actual codec.
				// this would avoid unnecessary late (out of band) lookups from the deferred
				// instance lazily
				var deferred = new Deferred<T, I, O>(type, lookup);
				forType.deferred.add(deferred);
				// deferred instance is separate for each requester, we don't cache it!
				return deferred;
			}

			// we're starting fresh lookup with no cycles in sight
			// and so any possible cycle will have deferred codec returned: see above
			forType = new Nesting.ForType();
			nesting.byType.put(type, forType);

			Class<?> raw = Types.toRawType(type);

			class FactoryMatcher {
				@Null Codec<T, I, O> created;

				void tryCreateWith(List<Entry> entries, Medium<?, ?> medium) {
					for (var e : entries) {
						if (e.medium == medium) {
							// this wrestling with generics is not sound,
							// but it works because we match by specific medium or
							// by any, factories working with Any medium are able to handle
							// medium with I, O subclasses
							var factory = (Codec.Factory<I, O>) e.factory;
							created = (Codec<T, I, O>) factory.tryCreate(
								type, raw, (Medium<I, O>) medium, lookup);

							if (created != null) break;
						}
					}
				}
			}

			// this matcher instance will be used to call tryCreateWith
			// and will hold result in its `created` field.
			var matcher = new FactoryMatcher();

			try {
				@Null var factoryByType = factoriesByType.get(raw);
				if (factoryByType != null) {
					// trying for the specific medium
					matcher.tryCreateWith(factoryByType, medium);
					// trying for any medium
					if (matcher.created == null) {
						matcher.tryCreateWith(factoryByType, Medium.Any);
					}
					// if created matching codec, we memoise it and return
					if (matcher.created != null) {
						memoisedCodecs.put(memoKey, matcher.created);
						return matcher.created;
					}
				} else {
					// we only have non-type specific codecs here,
					// so we just try for specific and then any medium
					matcher.tryCreateWith(factoriesCatchAll, medium);
					if (matcher.created == null) {
						matcher.tryCreateWith(factoriesCatchAll, Medium.Any);
					}
					// if created matching codec, we memoise it and return
					if (matcher.created != null) {
						memoisedCodecs.put(memoKey, matcher.created);
						return matcher.created;
					}
				}
				// nothing found
				return null;
			} finally {
				// here we actualize deferred instances to avoid lazy lookups from deferred
				if (matcher.created != null) {
					for (var d : forType.deferred) {
						// this would not throw as just assigning references
						((Deferred<T, I, O>) d).setActual(matcher.created);
					}
				}
				// we've fulfilled any deferred proxies,
				// so we can remove this nesting entry
				nesting.byType.remove(type);
			}
		} finally {
			if (nestingOnEntry == null) {
				// we created nesting on entry, so we'll clean it up after
				resolutionNesting.remove();
			}
		}
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
				// will throw NoSuchElementException if not found and called out of band
				setActual(lookup.get(type));
				assert actual != null;
			}
			return actual;
		}

		void setActual(Codec<T, I, O> actual) {
			this.actual = actual;
			// not sure that this helps much, but it will release that context lookup
			this.lookup = null;
		}
	}

	/**
	 * Collects factories and configuration to build an instance of {@link Registry}.
	 */
	public static final class Builder {
		private final List<Entry> entries = new ArrayList<>();
		private boolean useBuiltin = true;

		public Builder add(Codec.Factory<In, Out> factory) {
			return add(factory, Medium.Any);
		}

		public <I extends In, O extends Out> Builder add(
			Codec.Factory<I, O> factory, Medium<I, O> medium, Class<?>... rawTypes) {
			if (rawTypes.length == 0 && factory instanceof Codec.SupportedTypes s) {
				rawTypes = s.supportedRawTypes().toArray(new Class<?>[0]);
			}
			entries.add(new Entry(factory, medium, rawTypes));
			return this;
		}

		public <T> Builder add(
			Function<T, String> toString,
			Function<String, T> fromString,
			Class<? extends T> type) {

			var codec = FromToStringCodec.from(toString, fromString, type);
			var classes = new Class<?>[] { codec.rawClass() };

			entries.add(new Entry(
				(t, r, m, l) -> r == type ? codec : null,
					Medium.Any, classes));

			return this;
		}

		public Builder fallback(Codec.Factory<In, Out> factory) {
			return add(factory, Fallback);
		}

		public Builder noBuiltin() {
			useBuiltin = false;
			return this;
		}

		public Registry build() {
			if (useBuiltin) {
				entries.addAll(0, builtin());
			}
			return new Registry(entries);
		}
	}

	private static List<Entry> builtin() {
		return List.of(
			new Entry(ScalarCodecs.Factory, Medium.Any, ScalarCodecs.classes()),
			new Entry(ContainerCodecs.GenericFactory, Medium.Any, ContainerCodecs.classes()),
			new Entry(ContainerCodecs.ArraysFactory, Medium.Any, new Class<?>[0])
		);
	}
}
