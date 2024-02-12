package io.immutables.regres;

import io.immutables.codec.Codec;
import io.immutables.codec.RemapContainerCodec;
import io.immutables.codec.Types;
import io.immutables.common.ProxyHandler;
import io.immutables.common.Source;
import io.immutables.meta.Null;
import io.immutables.regres.SqlAccessor.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.joining;

/**
 * Minimalistic toolkit for SQL access which is based on reflection proxies of interfaces defining
 * data access methods
 * and underlying SQL code snippets (Dao.java + Dao.sql in classpath). In the SQL file
 * {@code --.<methodName>} comments
 * are used to lead sections of SQL which corresponds to specific methods in Java interface.
 */
// TODO Transactions/Control handle/with lambda
// TODO GetGeneratedKeys (maybe someday)
public final class Regresql {
	private Regresql() {}

	@SuppressWarnings("unchecked") // cast guaranteed by Proxy contract, runtime verified
	public static <T> T create(
		Class<T> accessor,
		Codec.Resolver codecs,
		ConnectionProvider connections) {
		if (!accessor.isInterface()
			|| accessor.getCanonicalName() == null
			|| !SqlAccessor.class.isAssignableFrom(accessor)) {
			throw new WrongDeclaration(
				"%s is not valid SQL access interface (top level interface extending %s)"
					.formatted(accessor, SqlAccessor.class));
		}

		return (T) Proxy.newProxyInstance(
			accessor.getClassLoader(),
			new Class<?>[]{accessor},
			handlerFor(accessor, codecs, connections));
	}

	public static SqlFactory factory(Codec.Resolver codecs, ConnectionProvider connections) {
		return new SqlFactory() {
			@Override public SqlStatement statement() {
				return new SqlStatement(codecs, connections);
			}

			@Override public ConnectionProvider.Handle handle() throws SQLException {
				return ConnectionHandle.get(connections);
			}

			@Override public <T> T transaction(Supplier<T> inTransaction) {
				return inTransaction(this::handle, false, false, inTransaction);
			}

			@Override public <T> T inNewTransaction(Supplier<T> inTransaction) {
				return inTransaction(this::handle, false, true, inTransaction);
			}

			@Override public <T> T readonly(Supplier<T> inTransaction) {
				return inTransaction(this::handle, true, false, inTransaction);
			}
		};
	}

	interface GetHandle {
		ConnectionProvider.Handle handle() throws SQLException;
	}

	private static <T> T inTransaction(GetHandle handler,
		boolean readonly, boolean requireNew, Supplier<T> inTransaction) {
		List<SQLException> suppressed = new ArrayList<>(0);

		try (var handle = handler.handle()) {
			var c = handle.connection();
			boolean wasAutoCommits = c.getAutoCommit();
			boolean wasReadOnly = c.isReadOnly();
			if (readonly && !wasReadOnly) c.setReadOnly(true);
			if (wasAutoCommits) c.setAutoCommit(false);
			try {
				var result = inTransaction.get();
				if (wasAutoCommits || requireNew) {
					c.commit();
				}
				return result;
			} catch (RuntimeException e) {
				if (wasAutoCommits || requireNew) {
					c.rollback();
				}
				throw e;
			} finally {
				if (wasAutoCommits) try {
					c.setAutoCommit(true);
				} catch (SQLException e) {
					suppressed.add(e);
				}
				if (readonly && !wasReadOnly) try {
					c.setReadOnly(false);
				} catch (SQLException e) {
					suppressed.add(e);
				}
			}
		} catch (RuntimeException e) {
			for (var s : suppressed) e.addSuppressed(s);
			throw e;
		} catch (SQLException e) {
			for (var s : suppressed) e.addSuppressed(s);
			throw new SqlException(e.getMessage(), e);
		}
	}

	private static Map<String, MethodProfile> compileProfiles(
		Class<?> accessor,
		Set<String> methods,
		Codec.Resolver codecs) {

		var map = new HashMap<String, MethodProfile>();

		for (Method m : accessor.getMethods()) {
			var name = methodName(m);
			if (methods.contains(name)) {
				map.put(name, profileMethod(m, codecs));
			}
			// ignore here everything else, either assume these kind of mismatches handled
			// elsewhere or just wish it will be ok.
		}
		return map;
	}

	private static MethodProfile profileMethod(Method method, Codec.Resolver codecs) {
		@Null UpdateCount updateCount = method.getAnnotation(UpdateCount.class);
		@Null Column column = method.getAnnotation(Column.class);
		@Null Single single = method.getAnnotation(Single.class);

		var builder = new MethodProfile.Builder();
		builder.name = methodName(method);
		builder.extractColumn = column != null;
		Type returnType = method.getGenericReturnType();
		builder.returnType = returnType;

		if (updateCount != null && (column != null || single != null))
			throw new WrongDeclaration(
				"@UpdateCount and (@Column extraction or @Single result)" +
					" cannot be used together on " + method);

		if (updateCount != null
			&& returnType != int.class
			&& returnType != int[].class
			&& returnType != long.class
			&& returnType != long[].class) {
			throw new WrongDeclaration(
				"@UpdateCount requires return type int, int[], long, or long[] on " + method);
		}

		profileParameters(method, codecs, builder);

		boolean returnUpdateCount = updateCount != null || returnType == void.class;
		builder.returnUpdateCount = returnUpdateCount;

		boolean useBatch = determineBatchParameter(builder);

		if (useBatch && !returnUpdateCount) throw new WrongDeclaration(
			"@Batch requires returning @UpdateCount or void return type" + method);

		if (!returnUpdateCount) {
			var maybeCodec = codecs.resolve(returnType, JdbcMedium.Internal);
			if (maybeCodec.isEmpty()) throw new WrongDeclaration(
				"No JDBC codec registered for return type %s in method %s"
					.formatted(returnType, method));

			var codec = maybeCodec.get();

			if (column != null) {
				var columnName = column.value();
				var columnIndex = column.index();
				if (single != null) {
					codec = new ColumnExtractor(codec, columnName, columnIndex);
				} else if (codec instanceof RemapContainerCodec remapping) {
					codec = remapping.remap(element ->
						new ColumnExtractor(element, columnName, columnIndex));
				} else throw new WrongDeclaration(
					"@Column can only be used with @Single for " + method + ". "
						+ "The codec for " + returnType + " is not known to support such "
						+ "extraction (a List, Set, Optional, can, for example)");
			}

			if (single != null) {
				codec = new SingleRowDecoder(codec, single.optional(), single.ignoreMore());
			}

			builder.returnTypeCodec = codec;
		}

		return builder.build();
	}

	private static boolean determineBatchParameter(MethodProfile.Builder builder) {
		boolean useBatch = false;
		var parameters = builder.parameters;
		for (int i = 0; i < parameters.size(); i++) {
			if (parameters.get(i).batch()) {
				builder.batchParameter = i;
				useBatch = true;
				break;
			}
		}
		return useBatch;
	}

	private static void profileParameters(
		Method m, Codec.Resolver codecs, MethodProfile.Builder builder) {

		Type[] types = m.getGenericParameterTypes();

		int batchCount = 0;
		var parameters = m.getParameters();

		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];

			@Null var namedAnnotation = parameter.getAnnotation(Named.class);
			@Null var batchAnnotation = parameter.getAnnotation(Batch.class);
			@Null var spreadAnnotation = parameter.getAnnotation(Spread.class);

			boolean hasName = parameter.isNamePresent();

			if (spreadAnnotation == null && namedAnnotation == null && !hasName) {
				throw new WrongDeclaration(
					"Parameter #" + i + " of "
						+ m.getDeclaringClass() + "." + methodName(m)
						+ " must have @Named annotation");
			}

			Type type = types[i];

			if (batchAnnotation != null) {
				batchCount++;

				Class<?> raw = Types.toRawType(type);
				if (raw.isArray()) {
					type = raw.getComponentType();
				} else if (Iterable.class.isAssignableFrom(raw)) {
					type = Types.getFirstArgument(type);
				} else throw new WrongDeclaration(
					"@Batch parameter must an Iterable or an array, but was " + type);
			}

			var codec = codecs.resolve(type, JdbcMedium.Internal);

			if (codec.isEmpty()) throw new WrongDeclaration(
				"No JDBC codec can be obtained for " + type);

			{
				var spreadWith = Optional.ofNullable(spreadAnnotation).map(Spread::prefix);
				boolean useBatch = batchAnnotation != null;

				String name;
				if (namedAnnotation != null) {
					name = namedAnnotation.value();
					if (!IdentifierPatterns.param.matcher(name).matches()) {
						throw new WrongDeclaration(
							"@Named(" + name + ") has illegal value. Should be: "
								+ IdentifierPatterns.DESCRIBE_PARAM);
					}
				} else if (parameter.isNamePresent()) {
					name = parameter.getName();
				} else if (spreadWith.isPresent()) {
					name = "<unspecified-" + i + ">";
				} else {
					throw new WrongDeclaration(
						"Parameter #" + i + " should have @Named annotation to provide a name"
							+ " when parameter names are not available after compilation"
							+ "(-parameters javac flag)");
				}

				var profile = new ParameterProfile(name, useBatch, spreadWith, codec.get(), type);

				@Null var existing = builder.parametersByName.put(name, profile);
				if (existing != null) throw new WrongDeclaration(
					"Method parameters have duplicate names, check @Named annotations: " + name);

				builder.parameters.add(profile);
			}
		}

		if (batchCount > 1) throw new WrongDeclaration(
			"@Batch should not be present on more than one parameter on " + m);
	}

	static InvocationHandler handlerFor(
		Class<?> accessor,
		Codec.Resolver codecs,
		ConnectionProvider provider) {

		Set<String> methods = uniqueAccessMethods(accessor);

		// We allow "empty" interfaces as a way to access SQL (via ConnectionProvider etc)
		// and don't load/require SQL sources in this case
		@Null SqlSource source;
		Map<String, MethodSnippet> snippets;
		Map<String, MethodProfile> profiles;
		if (!methods.isEmpty()) {
			source = loadSqlSource(accessor);
			snippets = parseSnippets(source, methods);
			profiles = compileProfiles(accessor, methods, codecs);
		} else {
			source = null;
			snippets = Map.of();
			profiles = Map.of();
		}

		return new ProxyHandler() {
			@Override
			protected @Null Object handleInterfaceMethod(
				Object proxy, Method method, Object[] arguments) throws Throwable {
				if (isConnectionHandleMethod(method)) return provider.handle();

				var name = methodName(method);
				var snippet = snippets.get(name);
				var profile = profiles.get(name);

				assert profile != null;

				return invokeSqlSnippet(provider, method, source, snippet, profile, arguments);
			}
		};
	}

	static @Null Object invokeSqlSnippet(
		ConnectionProvider provider,
		@Null Method method,
		@Null SqlSource source,
		MethodSnippet snippet,
		MethodProfile profile,
		Object[] arguments) throws Exception {

		try (
			var handle = provider.handle();
			var statement = handle.connection().prepareStatement(snippet.statements())) {

			prepareStatement(statement, profile, snippet, arguments);

			@Null Object result;

			try {
				result = executeStatement(statement, profile);
			} catch (SQLException exception) {
				throw Exceptions.refineException(source, method, snippet, exception);
			}

			if (profile.returnType() == void.class) {
				// for the case where update count is to be ignored (void method)
				// we still carry update count up to here,
				// maybe will use it for debugging/logging
				assert profile.returnsUpdateCount();
				result = null;
			}

			return result;
		}
	}

	private static boolean isConnectionHandleMethod(Method method) {
		return SqlAccessor.class.isAssignableFrom(method.getDeclaringClass())
			&& methodName(method).equals("handle")
			&& method.getParameterCount() == 0;
	}

	private static void prepareStatement(
		PreparedStatement statement,
		MethodProfile profile,
		MethodSnippet snippet,
		Object[] args) throws SQLException, IOException {

		var parameters = profile.parameters();
		var out = new StatementOut(profile.parameterIndex());

		if (profile.batchParameter().isPresent()) {
			int batchIndex = profile.batchParameter().getAsInt();
			for (int i = 0; i < parameters.size(); i++) {
				if (i != batchIndex) {
					putArgument(out, parameters.get(i), i, args[i]);
				}
			}
			ParameterProfile batcher = parameters.get(batchIndex);
			Object batch = args[batchIndex];
			if (batch instanceof Iterable<?>) {
				for (Object o : (Iterable<?>) batch) {
					putArgument(out, batcher, batchIndex, o);
					out.fillStatement(statement, snippet.placeholders());
					statement.addBatch();
				}
			} else if (batch.getClass().isArray()) {
				int length = Array.getLength(batch);
				for (int i = 0; i < length; i++) {
					Object value = Array.get(batch, i);
					putArgument(out, batcher, batchIndex, value);
					out.fillStatement(statement, snippet.placeholders());
					statement.addBatch();
				}
			} else throw new WrongDeclaration(
				"Batch parameter should be Iterable or an array");
		} else {
			for (int i = 0; i < parameters.size(); i++) {
				putArgument(out, parameters.get(i), i, args[i]);
			}
			out.fillStatement(statement, snippet.placeholders());
		}
	}

	private static void putArgument(StatementOut out, ParameterProfile p, int index, Object value)
		throws IOException {
		out.putField(index);
		if (p.spread().isPresent()) {
			out.spread(p.spread().get());
		}
		p.codec().encode(out, value);
	}

	private static SqlSource loadSqlSource(Class<?> accessorInterface) throws AssertionError {
		String filename = Snippets.resourceFilenameFor(accessorInterface);
		@Null InputStream resource = accessorInterface.getResourceAsStream(filename);

		if (resource == null) throw new MissingResourceException(
			filename + " must be present in classpath",
			accessorInterface.getCanonicalName(),
			filename);

		// We minimize copying. We only have full source in a single Buffer which
		// we fill in only once and then only operate on its subsequences
		// (shallow copies which share character content)
		// Then we use StringBuffer (one per method) when using regex
		// to generate prepared statement strings which we then store
		// and use directly for JDBC. Also, we collect placeholder strings in lists
		// per method.
		Source.Buffer content = new Source.Buffer();

		// none of the simpler ways available without libraries (?)
		// transferTo works for byte streams, not chars
		try (var source =
			new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
			char[] buffer = new char[BUFFER_SIZE];
			int length;
			while ((length = source.read(buffer)) != -1) {
				content.write(buffer, 0, length);
			}
		} catch (IOException readingClasspathResourceFailed) {
			throw new UncheckedIOException("Cannot read " + filename,
				readingClasspathResourceFailed);
		}

		return new SqlSource(filename, content, Source.Lines.from(content));
	}

	private static Map<String, MethodSnippet> parseSnippets(SqlSource source,
		Set<String> methods) {
		var snippets = Snippets.parse(source.content(), source.lines());

		var problems = new ArrayList<Source.Problem>();
		var unique = new HashMap<String, MethodSnippet>();

		for (Entry<String, List<MethodSnippet>> e : snippets.entrySet()) {
			String name = e.getKey();
			if (name.isEmpty()) {
				for (MethodSnippet nameless : e.getValue()) {
					problems.add(source.problemAt(
						nameless.statementsRange(),
						"SQL statements not under method",
						"Put statements after --.method declarations"));
				}
			} else if (!methods.contains(name)) {
				for (MethodSnippet unmatched : e.getValue()) {
					problems.add(source.problemAt(
						unmatched.identifierRange(),
						"There are no corresponding `" + name + "` method in interface",
						"Declared interface methods: " + String.join(", ", methods)));
				}
			} else if (e.getValue().size() > 1) {
				for (MethodSnippet duplicate : e.getValue().subList(1, e.getValue().size())) {
					problems.add(source.problemAt(
						duplicate.identifierRange(),
						"Duplicate `" + name + "` declaration",
						"No method duplicates or overloads are allowed"));
				}
			} else {
				assert !e.getValue().isEmpty();
				unique.put(name, e.getValue().get(0));
			}
		}

		for (var name : methods) {
			if (!snippets.containsKey(name)) {
				problems.add(source.problemAt(Source.Range.of(source.get(0)),
					"Missing `" + name + "` declaration",
					"Method declared in interface but has no SQL"));
			}
		}

		if (!problems.isEmpty()) throw new WrongDeclaration(
			"\n" + problems.stream().map(Object::toString).collect(joining("\n")));

		return Map.copyOf(unique);
	}

	private static Set<String> uniqueAccessMethods(Class<?> accessorInterface) {
		var methodNamesGrouped = Arrays.stream(accessorInterface.getMethods())
			.filter(Regresql::eligibleAccessMethod)
			.map(Regresql::methodName)
			.collect(Collectors.groupingBy(n -> n));

		var duplicates = methodNamesGrouped.entrySet()
			.stream()
			.filter(e -> e.getValue().size() > 1)
			.map(Entry::getKey)
			.toList();

		if (!duplicates.isEmpty()) {
			throw new WrongDeclaration(
				"Method overloads are not supported for %s: %s. Please use distinct names"
					.formatted(accessorInterface, duplicates));
		}

		return methodNamesGrouped.keySet();
	}

	/**
	 * In Kotlin method may accept value class annotated by @JvmInline as a parameter.
	 * To prevent clash in runtime such method will have a name like 'methodName-{additional
	 * alphanumeric characters}'
	 * This method return the same name as it was on compile time
	 */
	private static String methodName(Method m) {
		String name = m.getName();
		int dashIndex = name.indexOf("-");
		return dashIndex == -1 ? name : name.substring(0, dashIndex);
	}

	private static boolean eligibleAccessMethod(Method m) {
		return Modifier.isAbstract(m.getModifiers()) && !isConnectionHandleMethod(m);
	}

	static @Null Object executeStatement(PreparedStatement statement, MethodProfile profile)
		throws SQLException, IOException {

		Type returnType = profile.returnType();
		boolean useUpdateCount = profile.returnsUpdateCount();
		boolean largeUpdateCount = useUpdateCount
			&& (returnType == long.class || returnType == long[].class);
		boolean sumUpdateCount = useUpdateCount
			&& (returnType == int.class || returnType == long.class);

		// While I usually use early returns, here, for the sake of symmetry and clarity
		// (yep, seems to be clearer in this case) we use return value and if/else branches.
		// do not initialize here, forcing compiler to check all branches to assign it
		@Null Object returnValue;

		if (profile.batchParameter().isPresent()) {
			if (largeUpdateCount) { // large update count
				long[] updates = statement.executeLargeBatch();

				if (sumUpdateCount) {
					returnValue = Arrays.stream(updates).sum();
				} else {
					returnValue = updates;
				}
			} else { // long update count and void
				int[] updates = statement.executeBatch();

				if (sumUpdateCount) {
					returnValue = Arrays.stream(updates).sum();
				} else {
					returnValue = updates;
				}
			}
		} else { // not batching
			boolean hasResultSet = statement.execute();

			if (useUpdateCount) {
				if (largeUpdateCount) { // long update count
					var updates = new ArrayList<Long>();
					for (long count; ; ) {
						count = statement.getLargeUpdateCount();
						if (count >= 0) {
							updates.add(count);
						}
						if (!hasResultSet && count < 0) break;
						hasResultSet = statement.getMoreResults();
					}
					if (sumUpdateCount) {
						returnValue = updates.stream().mapToLong(l -> l).sum();
					} else {
						returnValue = updates.stream().mapToLong(l -> l).toArray();
					}
				} else { // int update count
					var updates = new ArrayList<Integer>();
					for (int count; ; ) {
						count = statement.getUpdateCount();
						if (count >= 0) {
							updates.add(count);
						}
						if (!hasResultSet && count < 0) break;
						hasResultSet = statement.getMoreResults();
					}
					if (sumUpdateCount) {
						returnValue = updates.stream().mapToInt(l -> l).sum();
					} else {
						returnValue = updates.stream().mapToInt(l -> l).toArray();
					}
				}
			} else { // reading result set (not an update count)
				// for the return type, the codec must be present at this point, so AssertionError
				var codec = profile.returnTypeCodec().orElseThrow(AssertionError::new);

				boolean wasResultSet = false;

				returnValue = null; // cannot use clean branching below, have to initialize early

				if (hasResultSet) {
					var in = new ResultIn(statement.getResultSet());
					returnValue = codec.decode(in);
					wasResultSet = true;
				}

				for (int count; ; ) {
					count = statement.getUpdateCount();
					if (!hasResultSet && count < 0) break;
					hasResultSet = statement.getMoreResults();

					if (hasResultSet) {
						if (wasResultSet) throw new WrongDeclaration(
							"Only single ResultSet can be processed, " +
								"Use sql UNION ALL to merge multiple results");

						var in = new ResultIn(statement.getResultSet());
						returnValue = codec.decode(in);
						wasResultSet = true;
					}
				}

				if (!wasResultSet) throw new WrongDeclaration(
					"ResultSet expected but there was none. " +
						"Fix SQL query, or use void return type or int/long @UpdateCount");
			} // end resultsets
		} // end not batch

		return returnValue;
	}

	public static final int BUFFER_SIZE = 8_192;
}
