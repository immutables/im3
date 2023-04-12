package io.immutables.codec;

import io.immutables.common.Unreachable;
import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static io.immutables.codec.Types.isRewrapped;
import static io.immutables.codec.Types.requireSpecificTypes;
import static java.util.Objects.requireNonNull;

/**
 * Codec registry, which is a main/reference implementation of {@link Codec.Resolver}.
 * Unlikely that any other implementation of {@code Code.Resolver} will exist
 * or will be needed, aside from any interceptors/proxies.
 */
public class Registry implements Codec.Resolver {

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

	public <T, I extends In, O extends Out>
	Optional<Codec<T, I, O>> resolve(Class<? extends T> type, Medium<I, O> medium) {
		return resolve((Type) type, medium);
	}

	public <T, I extends In, O extends Out>
	Optional<Codec<T, I, O>> resolve(Type type, Medium<I, O> medium) {
		requireNonNull(medium);
		if (medium == Medium.Any) throw new IllegalArgumentException(
			"%s is only for registering/matchting codec factories, not for resolving codecs"
				.formatted(Medium.Any));
		// it is very important that we not only validated type for specific
		// but also rewrapped all JVM provided reflected ParameterizedTypes to
		// our own implementation. JVM ones are never equal to custom implementations,
		// so we cannot mix them in maps/sets etc.
		type = requireSpecificTypes(type);

		// Lookup callback used from within codec factories/codecs to lookup
		// codecs they delegate to for their fields etc.
		Codec.Lookup<I, O> lookup = new Codec.Lookup<>() {
			public <W> Codec<W, I, O> get(Type t) {
				assert isRewrapped(t); // assert here because this could be a call not from here
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

			Class<?> raw = toRawType(type);

			class FactoryMatcher {
				@Null Codec<T, I, O> codec;

				void tryCreateWith(List<Entry> entries, Medium<?, ?> medium) {
					for (var e : entries) {
						if (e.medium == medium) {
							// this wrestling with generics is not sound,
							// but it works because we match by specific medium or
							// by any, factories working with Any medium are able to handle
							// medium with I, O subclasses
							var factory = (Codec.Factory<I, O>) e.factory;
							codec = (Codec<T, I, O>) factory.tryCreate(
								type, raw, (Medium<I, O>) medium, lookup);

							if (codec != null) break;
						}
					}
				}
			}

			var matcher = new FactoryMatcher();

			try {
				@Null var factoryByType = factoriesByType.get(raw);
				if (factoryByType != null) {
					matcher.tryCreateWith(factoryByType, medium);
					if (matcher.codec == null) {
						matcher.tryCreateWith(factoryByType, Medium.Any);
					}

					if (matcher.codec != null) {
						memoisedCodecs.put(memoKey, matcher.codec);
						return matcher.codec;
					}
				} else {
					matcher.tryCreateWith(factoriesCatchAll, medium);
					if (matcher.codec == null) {
						matcher.tryCreateWith(factoriesCatchAll, Medium.Any);
					}

					if (matcher.codec != null) {
						memoisedCodecs.put(memoKey, matcher.codec);
						return matcher.codec;
					}
				}
				return null;
			} finally {
				// here we actualize deferred instances to avoid lazy lookups from deferred
				if (matcher.codec != null) {
					for (var d : forType.deferred) {
						// this would not throw as just assigning references
						((Deferred<T, I, O>) d).setActual(matcher.codec);
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

		public Builder add(Codec.Factory<In, Out> factory) {
			return add(factory, Medium.Any);
		}

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
				entries.addAll(0, builtin());
			}
			return new Registry(entries);
		}
	}

	private static List<Entry> builtin() {
		ScalarCodecs scalars = new ScalarCodecs();
		return List.of(
			new Entry(scalars, Medium.Any, scalars.classes()),
			new Entry(new OptionalCodecs(), Medium.Any, new Class<?>[]{Optional.class}),
			new Entry(new CollectionCodecs(), Medium.Any, new Class<?>[0])
		);
	}
}