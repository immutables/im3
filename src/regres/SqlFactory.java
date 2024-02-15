package io.immutables.regres;

import io.immutables.codec.Codec;
import io.immutables.codec.Types;
import io.immutables.common.Source;
import io.immutables.meta.Null;
import io.immutables.meta.NullUnknown;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import static io.immutables.regres.IdentifierPatterns.*;
import static java.util.Objects.requireNonNull;

public interface SqlFactory {
	/**
	 * Creates new sql statement, statement is a mutable "builder" which that can be user
	 * to call execute update or query.
	 */
	SqlStatement statement();

	<T> T transaction(Supplier<T> inTransaction);

	<T> T readonly(Supplier<T> inTransaction);

	default void transaction(Runnable inTransaction) {
		transaction(() -> {
			inTransaction.run();
			return null;
		});
	}

	default SqlStatement sql(CharSequence template) {
		return statement().sql(template);
	}

	default SqlStatement sql(CharSequence template, Consumer<SqlStatement.Parameters> binder) {
		return statement().sql(template, binder);
	}

	ConnectionProvider.Handle handle() throws SQLException;

	final class SqlStatement {
		private final Codec.Resolver codecs;
		private final ConnectionProvider provider;

		private final Source.Buffer buffer = new Source.Buffer();
		private boolean wasLexem;

		private final List<Parameter<?>> parameters = new ArrayList<>(4);
		private final Set<String> parameterNames = new HashSet<>();

		private int unnamedParameterCounter;
		private int batchIndex = ABSENT;

		public final Parameters params = new Parameters();

		SqlStatement(Codec.Resolver codecs, ConnectionProvider provider) {
			this.codecs = codecs;
			this.provider = provider;
		}

		/**
		 * Appends SQL template.
		 * @param template whole or part of the SQL template
		 * @return {@code this} instance
		 */
		public SqlStatement sql(CharSequence template) {
			if (!template.isEmpty() && Character.isAlphabetic(template.charAt(0))) {
				maybePrependSpace(true);
			}
			this.buffer.append(template);
			return this;
		}

		public SqlStatement sql(CharSequence template, Consumer<Parameters> parameterBinder) {
			sql(template);
			parameterBinder.accept(params);
			return this;
		}

		public SqlStatement params(Consumer<Parameters> parameterBinder) {
			parameterBinder.accept(params);
			return this;
		}

		/**
		 * Appends parts of the SQL template. This method, albeit convenient to put
		 * parts together, at the same time only allow {@link String}/{@link CharSequence}
		 * instances for SQL text parts and instances of {@link AsSqlLexem} instances
		 * which are known to be sanitized and contain no injections. To avoid injections with
		 * plain concatenation - avoid using plain string formatting,
		 * use DSL methods to construct SQL templates.
		 * @param parts parts of the html template to be concatenated.
		 * @return {@code this} instance
		 */
		public SqlStatement sql(Object... parts) {
			for (var part : parts) {
				if (part instanceof CharSequence s) {
					maybePrependSpace(false);
					buffer.append(s);
				} else if (part instanceof AsSqlLexem p) {
					maybePrependSpace(true);
					buffer.append(p.asSqlPart());
				} else throw new IllegalArgumentException(
					("Cannot use potentially unsafe instance of %s for template. " +
						"Use only %s or %s instance").formatted(
						part.getClass().getName(),
						CharSequence.class.getSimpleName(),
						AsSqlLexem.class.getSimpleName()));
			}
			return this;
		}

		public SqlStatement in(Object... values) {
			return in(List.of(values));
		}

		public SqlStatement in(Collection<?> values) {
			return sql("in (").separating(", ", values, (v, p) -> p.next(v)).sql(")");
		}

		public <T> SqlStatement separating(
			CharSequence separator,
			Collection<T> elements,
			BiFunction<? super T, Parameters, AsSqlLexem> consumer) {
			int i = 0;
			for (var e : elements) {
				if (i++ > 0) sql(separator);
				var lexem = consumer.apply(e, params);
				sql(lexem.asSqlPart());
			}
			return this;
		}

		// this is small auto-convenience, but is made in the best effort
		// to minimize annoyance of accidental mistakes of gluing identifier to
		// the preceding keyword or number etc
		private void maybePrependSpace(boolean onLexem) {
			if (!buffer.isEmpty()) {
				if (onLexem || wasLexem) {
					char c = buffer.charAt(buffer.length() - 1);
					if (Character.isDigit(c) || Character.isAlphabetic(c)) {
						buffer.append(' ');
					}
				}
			}
			wasLexem = onLexem;
		}

		public final class Parameters {
			/**
			 * Plain identifier to be used in SQL template as a name of schema element
			 * (table, column, aliases, etc.), guaranteed to not contain any injection.
			 * @param identifier string matching {@value IdentifierPatterns#DESCRIBE_PLAIN}
			 * @return object, which toString will produce properly
			 */
			public SqlIdentifier identifier(String identifier) {
				return new SqlIdentifier(false, identifier);
			}

			/**
			 * Quoted identifiers will be enclosed in double quotes. We allow very constrained set
			 * of characters. Please skip the quotes, which will be added automatically.
			 * @param identifier string matching {@value IdentifierPatterns#DESCRIBE_QUOTED}
			 * @return object, which toString will produce properly
			 */
			public SqlIdentifier quoted(String identifier) {
				return new SqlIdentifier(true, identifier);
			}

			private String checkParamName(String name) {
				if (name.startsWith(":")) {
					throw new IllegalArgumentException(
						"Drop ':' when specify parameter name (`%s`), leave just name."
							.formatted(name));
				}
				if (!param.matcher(name).matches()) {
					throw new IllegalArgumentException(
						"Parameter `%s` name is not legal, should be: %s"
							.formatted(name, DESCRIBE_PARAM));
				}
				if (!parameterNames.add(name)) {
					throw new IllegalStateException(
						"Parameter `" + name + "` name is already defined");
				}
				return name;
			}

			public <V> Parameter<V> next(@Null V value) {
				return add(new Parameter<>(
					false, checkParamName(String.valueOf(unnamedParameterCounter++)), value));
			}

			public <V> Parameter<V> bind(String name, @Null V value) {
				return add(new Parameter<>(false, checkParamName(name), value));
			}

			@SafeVarargs
			public final <V> Parameter<V> batch(String name, V... values) {
				checkParamName(name);
				var batchValues = List.of(values);
				checkBatch(batchValues);
				batchIndex = parameters.size();
				return add(new Parameter<>(true, name, batchValues));
			}

			// would use iterable, but it seems that we better force sized collection
			public <V> Parameter<V> batch(String name, Collection<V> values) {
				checkParamName(name);
				checkBatch(values);
				batchIndex = parameters.size();
				return add(new Parameter<>(true, name, List.copyOf(values)));
			}

			private void checkBatch(Collection<?> values) {
				if (batchIndex >= 0) throw new IllegalStateException(
					"Cannot have more than one batch parameter in query");
			}

			public <V> SpreadParameter<V> spread(@Null V value) {
				var index = parameters.size();
				return add(new SpreadParameter<>(false, index, value));
			}

			@SafeVarargs
			public final <V> SpreadParameter<V> batchSpread(V... values) {
				var batchValues = List.of(values);
				checkBatch(batchValues);
				var index = batchIndex = parameters.size();
				return add(new SpreadParameter<>(true, index, batchValues));
			}

			public <V> SpreadParameter<V> batchSpread(Collection<V> values) {
				checkBatch(values);
				var index = parameters.size();
				return add(new SpreadParameter<>(true, index, List.copyOf(values)));
			}

			private <V, P extends Parameter<V>> P add(P parameter) {
				parameters.add(parameter);
				return parameter;
			}
		}

		private @NullUnknown Object execute(ExpectedResult mode, Type returnType) {
			var filename = "<inline>";
			var methodName = "";
			var lines = Source.Lines.from(buffer);
			var source = new SqlSource(filename, buffer, lines);
			var placeholders = new ArrayList<MethodSnippet.Placeholder>();
			var statements = Snippets.extractStatements(buffer, 0, lines, placeholders::add);
			var zeroPosition = lines.get(0);
			var identifierRange = Source.Range.of(zeroPosition, zeroPosition);
			var statementsRange = Source.Range.of(zeroPosition, lines.get(buffer.length()));
			var snippet = new MethodSnippet(
				methodName, source, placeholders, identifierRange, statementsRange, statements);

			var builder = new MethodProfile.Builder();
			builder.name = methodName;
			builder.batchParameter = batchIndex;

			switch (mode) {
			case Update -> {
				builder.returnUpdateCount = true;
				builder.returnType = int.class;
			}
			case Single -> {
				builder.returnType = returnType;

				var codec = codecs.resolve(returnType, JdbcMedium.Internal)
					.orElseThrow(() -> noCodecFor(returnType));

				builder.returnTypeCodec = new SingleRowDecoder(codec, false, false);
			}
			case First, Optional -> {
				var parameterized = Types.newParameterized(Optional.class, returnType);
				builder.returnType = parameterized;
				var codec = codecs.resolve(parameterized, JdbcMedium.Internal)
					.orElseThrow(() -> noCodecFor(returnType));

				builder.returnTypeCodec = new SingleRowDecoder(
					codec, true, mode == ExpectedResult.First);
			}
			case List -> {
				var parameterized = Types.newParameterized(List.class, returnType);
				builder.returnType = parameterized;
				builder.returnTypeCodec = codecs.resolve(parameterized, JdbcMedium.Internal)
					.orElseThrow(() -> noCodecFor(returnType));
			}
			}

			profileParameters(codecs, builder);

			var profile = builder.build();

			try {
				return Regresql.invokeSqlSnippet(provider, /*no reflective method*/null,
					source, snippet, profile, collectArguments());
			} catch (RuntimeException e) {
				throw e;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private Object[] collectArguments() {
			return parameters.stream().map(p -> p.value).toArray();
		}

		private static IllegalStateException noCodecFor(Type returnType) {
			return new IllegalStateException("Cannot find codec based on "
				+ returnType.getTypeName());
		}

		private void profileParameters(Codec.Resolver codecs, MethodProfile.Builder builder) {
			for (var p : parameters) {
				var spread = p instanceof SpreadParameter<?> sp
					? Optional.of(sp.prefix)
					: Optional.<String>empty();

				Type type;
				if (p.type != null) {
					type = p.type;
				} else if (p.value != null) {
					if (p.batch) {
						type = ((List<?>) p.value).get(0).getClass();
					} else {
						type = p.value.getClass();
					}
				} else {
					type = Object.class;
				}
				// if value is null and type is null, Object might be bad assumption,
				// but let it slide and see what's happens

				var codec = codecs.resolve(type, JdbcMedium.Internal)
					.orElseThrow(() -> new IllegalStateException(
						"Cannot obtain JDBC codec for type " + type.getTypeName()));

				var profile = new ParameterProfile(p.name, p.batch, spread, codec, type);

				builder.parametersByName.put(p.name, profile);
				builder.parameters.add(profile);
			}
		}

		public int update() {
			return (int) execute(ExpectedResult.Update, int.class);
		}

		public <T> List<T> list(Class<T> type) {
			return list((Type) type);
		}

		@SuppressWarnings("unchecked")
		public <T> List<T> list(Type type) {
			var result = execute(ExpectedResult.List, type);
			assert result != null;
			return (List<T>) result;
		}

		public <T> T single(Class<T> type) {
			return single((Type) type);
		}

		@SuppressWarnings("unchecked")
		public <T> T single(Type type) {
			var result = execute(ExpectedResult.Single, type);
			assert result != null;
			return (T) result;
		}

		public <T> Optional<T> optional(Class<T> type) {
			return optional((Type) type);
		}

		@SuppressWarnings("unchecked")
		public <T> Optional<T> optional(Type type) {
			var result = execute(ExpectedResult.Optional, type);
			assert result != null;
			return (Optional<T>) result;
		}

		public <T> Optional<T> first(Class<T> type) {
			return first((Type) type);
		}

		@SuppressWarnings("unchecked")
		public <T> Optional<T> first(Type type) {
			var result = execute(ExpectedResult.First, type);
			assert result != null;
			return (Optional<T>) result;
		}

		public static final int ABSENT = -1;
	}

	class SpreadParameter<V> extends Parameter<V> {
		String prefix = "";

		SpreadParameter(boolean batch, int index, @Null Object value) {
			super(batch, "#" + index, value);
		}

		/**
		 * While spreading field (record components) values as parameters, we can add
		 * prefix to each of the field name.
		 * @param prefix prefix to add to each field coming from this value.
		 * @return {@code this} instance
		 */
		public SpreadParameter<V> prefix(String prefix) {
			if (!param.matcher(prefix).matches()) throw new IllegalStateException(
				"Prefix contains illegal characters, should be " + DESCRIBE_PARAM);
			this.prefix = prefix;
			return this;
		}

		@Override public SpreadParameter<V> parameterizedType(Class<?> raw, Type... arguments) {
			return (SpreadParameter<V>) super.parameterizedType(raw, arguments);
		}

		@Override public SpreadParameter<V> type(Type type) {
			return (SpreadParameter<V>) super.type(type);
		}

		@Override public SpreadParameter<V> type(Class<? super V> type) {
			return (SpreadParameter<V>) super.type(type);
		}

		@Override public String asSqlPart() {
			throw new IllegalStateException(
				"Parameter '" + name + "' cannot be used as placeholder directly,"
					+ " as it 'spreads' to multiple parameters at a time");
		}
	}

	/**
	 * SQL parameter is builder like object and once created, it registers (named or multiple).
	 * It is expected then, that template will contain matching placeholders (in the form
	 * of {@code :name}), and to expedite imperative (dynamic) building of SQL statements,
	 * such {@code SqlParameter} object can be inserted as SQL part and automatically
	 * turned into placeholder via {@link SqlStatement#sql(Object...)} (where only
	 * Strings/CharSequences and {@link AsSqlLexem} instances are allowed)
	 * @param <V> type of the value
	 */
	class Parameter<V> implements AsSqlLexem {
		final boolean batch;
		final String name;
		final @Null Object value;

		@Null Type type;

		Parameter(boolean batch, String name, @Null Object value) {
			this.batch = batch;
			this.name = name;
			this.value = value;
		}

		/**
		 * Not always needed as often runtime type is sufficient for many simple cases.
		 * Explicitly specifying type may help to find appropriate codec. These cases can
		 * include situation when there's a codec for an abstract datatype but not for
		 * implementation type. When parameterized type is used with type parameters,
		 * please use {@link #type(Type)} overload taking parameterized type.
		 */
		public Parameter<V> type(Class<? super V> type) {
			return type((Type) type);
		}

		public Parameter<V> parameterizedType(Class<?> raw, Type... arguments) {
			var type = Types.newParameterized(raw, arguments);
			Types.requireSpecific(type);
			return type(type);
		}

		/**
		 * Not always needed as often runtime type is sufficient for many simple cases.
		 * Explicitly specifying type may help to find appropriate codec. These cases can
		 * include situation when there's a codec for an abstract datatype but not for
		 * implementation type or in case of parameterized type, where type parameters should
		 * be specified. This type is used for the codec lookup.
		 *
		 * <p>Note: in the case of batch parameter this is the type of element, not a collection
		 * type.
		 * @param type {@link Class} instance or generic {@link Type} if parameterized.
		 * @return {@code this} instance
		 */
		public Parameter<V> type(Type type) {
			if (this.type != null) throw new IllegalStateException(
				"Type already specified for parameter `%s`, current %s, duplicate %s"
					.formatted(name, this.type.getTypeName(), type.getTypeName()));

			this.type = requireNonNull(type);
			return this;
		}

		@Override public String asSqlPart() {
			return ":" + name;
		}
	}

	/**
	 * Elements implementing this interface are considered safe to be added to SQL
	 * template text. Here parameters in form ({@code :name}) or identifiers ({@code ident}
	 * or {@code "Quoted Identifier"}).
	 */
	interface AsSqlLexem {
		/**
		 * prints parameter as placeholder. i.e. for parameter named {@code my},
		 * this will output {@literal ":my"} to be used as part SQL template.
		 */
		String asSqlPart();
	}

	/**
	 * Identifiers are meant to be inserted as SQL part via {@link SqlStatement#sql(Object...)}
	 * as properly sanitized identifiers.
	 */
	final class SqlIdentifier implements AsSqlLexem {
		public final boolean quoted;
		public final String identifier;

		private SqlIdentifier(boolean quoted, String identifier) {
			this.quoted = quoted;
			this.identifier = identifier;

			if (quoted) {
				// this clause improves diagnostics for a common case
				if (identifier.contains("\"")) throw new IllegalArgumentException(
					"`%s` for quoted identifiers, quotes will be added automatically, skip quotes"
						.formatted(identifier));

				if (!IdentifierPatterns.quoted.matcher(identifier).matches()) {
					throw new IllegalArgumentException(
						"`%s` identifier contains illegal characters, should be: %s"
							.formatted(identifier, DESCRIBE_QUOTED));
				}
			} else if (!plain.matcher(identifier).matches()) {
				throw new IllegalArgumentException(
					"`%s` identifier contains illegal characters, should be: %s"
						.formatted(identifier, DESCRIBE_PLAIN));
			}
		}

		@Override public String asSqlPart() {
			// it's safe to concat here, because identifier is sanitized to not contain
			// any double (or other) quotes
			return quoted ? ("\"" + identifier + "\"") : identifier;
		}
	}
}
