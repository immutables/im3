package io.immutables.regres;

import io.immutables.codec.*;
import io.immutables.common.ProxyHandler;
import io.immutables.common.Source;
import io.immutables.meta.Null;
import io.immutables.regres.SqlAccessor.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Minimalistic toolkit for SQL access which is based on reflection proxies of interfaces defining
 * data access methods
 * and underlying SQL code snippets (Dao.java + Dao.sql in classpath). In the SQL file
 * {@code --.<methodName>} comments
 * are used to lead sections of SQL which corresponds to specific methods in Java interface.
 */
// TODO Transactions/Control handle/with lambda
// TODO Exception improvements/consistency
// TODO GetGeneratedKeys (maybe someday)
public final class Regresql {
	private Regresql() {}

	record MethodSnippet(
		String name,
		List<String> placeholders,
		Source.Range identifierRange,
		Source.Range statementsRange,
		String preparedStatements
	) {
		static class Builder {
			@Null String name;
			List<String> placeholders = new ArrayList<>();
			@Null Source.Range identifierRange;
			@Null Source.Range statementsRange;
			@Null String preparedStatements;

			MethodSnippet build() {
				return new MethodSnippet(
					requireNonNull(name),
					List.copyOf(placeholders),
					requireNonNull(identifierRange),
					requireNonNull(statementsRange),
					requireNonNull(preparedStatements)
				);
			}
		}
	}

	record SqlSource(
		String filename,
		CharSequence content,
		Source.Lines lines
	) {
		Source.Position get(int position) {
			return lines().get(position);
		}

		Source.Problem problemAt(Source.Range range, String message, String hint) {
			return new Source.Problem(filename(), content(), lines(), range, message, hint);
		}
	}

	record ParameterProfile(
		String name,
		boolean batch,
		Optional<String> spread,
		Codec<Object, In, Out> codec,
		Type type
	) {}

	record MethodProfile(
		String name,
		OptionalInt batchParameter,
		boolean returnUpdateCount,
		boolean extractColumn,
		List<ParameterProfile> parameters,
		Map<String, ParameterProfile> parametersByName,
		Optional<Codec<Object, In, Out>> returnTypeCodec,
		Type returnType
	) {
		boolean returnResultSet() {
			return !returnUpdateCount();
		}

    NameIndex parameterIndex() {
      return NameIndex.known(parameters().stream()
          .map(ParameterProfile::name)
          .toArray(String[]::new));
    }

		static class Builder {
			@Null String name;
			@SuppressWarnings("all")
			OptionalInt batchParameter = OptionalInt.empty();
			boolean returnUpdateCount;
			boolean extractColumn;
			// this keeps parameter in order
			List<ParameterProfile> parameters = new ArrayList<>();
			Map<String, ParameterProfile> parametersByName = new HashMap<>();
			@Null Type returnType;
			@SuppressWarnings("all")
			Optional<Codec<Object, In, Out>> returnTypeCodec = Optional.empty();

			MethodProfile build() {
				return new MethodProfile(
						requireNonNull(name),
						batchParameter,
						returnUpdateCount,
						extractColumn,
						List.copyOf(parameters),
						Map.copyOf(parametersByName),
						returnTypeCodec,
						requireNonNull(returnType));
			}
		}
	}

	@SuppressWarnings("unchecked") // cast guaranteed by Proxy contract, runtime verified
	public static <T> T create(
			Class<T> accessor,
			Codec.Resolver codecs,
			ConnectionProvider connections) {
		if (!accessor.isInterface()
				|| accessor.getCanonicalName() == null
				|| !SqlAccessor.class.isAssignableFrom(accessor)) {
			throw new IllegalArgumentException(
					"%s is not valid SQL access interface (top level interface extending %s)"
							.formatted(accessor, SqlAccessor.class));
		}

		return (T) Proxy.newProxyInstance(
			accessor.getClassLoader(),
			new Class<?>[]{accessor},
			handlerFor(accessor, codecs, connections));
	}

	private static Map<String, MethodProfile> compileProfiles(
		Class<?> accessor,
		Set<String> methods,
		Codec.Resolver codecs) {

		Map<String, MethodProfile> map = new HashMap<>();

		for (Method m : accessor.getMethods()) {
			String name = methodName(m);
			if (methods.contains(name)) {
				map.put(name, profileMethod(m, codecs));
			}
			// ignore here everything else, either assume these kind of mismatches handled elsewhere
			// or just wish it will be ok.
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
			throw new IllegalStateException(
				"@UpdateCount and (@Column extraction or @Single result) cannot be used together on "
						+ method);

		if (updateCount != null
			&& returnType != int.class
			&& returnType != int[].class
			&& returnType != long.class
			&& returnType != long[].class) {
			throw new IllegalStateException(
				"@UpdateCount requires return type int, int[], long, or long[] on " + method);
		}

		profileParameters(method, codecs, builder);

		boolean returnUpdateCount = updateCount != null || returnType == void.class;
		builder.returnUpdateCount = returnUpdateCount;

		boolean useBatch = determineBatchParameter(builder);

		if (useBatch && !returnUpdateCount) throw new IllegalStateException(
				"@Batch requires returning @UpdateCount or void return type" + method);

		if (!returnUpdateCount) {
			var maybeCodec = codecs.resolve(returnType, JdbcMedium.Internal);
			if (maybeCodec.isEmpty()) throw new IllegalStateException(
					"No JDBC codec registered for return type %s in method %s"
							.formatted(returnType, method));

			var codec = maybeCodec.get();

			if (column != null) {
				if (single != null) {
					codec = new ColumnExtractor(codec, column);
				} else if (codec instanceof RemapContainerCodec remapping) {
					codec = remapping.remap(element -> new ColumnExtractor(element, column));
				} else throw new IllegalStateException(
					"@Column can only be used with @Single for " + method + ". " +
							"The codec for " + returnType + " is not known to support such extraction " +
							"(a ContainerCodec can)");
			}

			if (single != null) {
				codec = new SingleRowDecoder(codec, single);
			}

			builder.returnTypeCodec = Optional.of(codec);
		}

		return builder.build();
	}

	private static boolean determineBatchParameter(MethodProfile.Builder builder) {
		boolean useBatch = false;
		var parameters = builder.parameters;
		for (int i = 0; i < parameters.size(); i++) {
			if (parameters.get(i).batch()) {
				builder.batchParameter = OptionalInt.of(i);
				useBatch = true;
				break;
			}
		}
		return useBatch;
	}

	private static void profileParameters(Method m, Codec.Resolver codecs, MethodProfile.Builder builder) {
		Type[] types = m.getGenericParameterTypes();

		int batchCount = 0;
		Parameter[] parameters = m.getParameters();

		for (int i = 0; i < parameters.length; i++) {
			Parameter p = parameters[i];

			@Null Named named = p.getAnnotation(Named.class);
			@Null Batch batch = p.getAnnotation(Batch.class);
			@Null Spread spread = p.getAnnotation(Spread.class);

			boolean hasName = p.isNamePresent();

			if (spread == null && named == null && !hasName) throw new IllegalArgumentException(
				"Parameter #" + i + " of "
					+ m.getDeclaringClass() + "." + methodName(m)
					+ " must have @Named annotation. (unless @Spread)");

			Type type = types[i];

			if (batch != null) {
				batchCount++;

				Class<?> raw = Types.toRawType(type);
				if (raw.isArray()) {
					type = raw.getComponentType();
				} else if (Iterable.class.isAssignableFrom(raw)) {
					type = Types.getFirstArgument(type);
				} else throw new IllegalStateException(
					"@Batch parameter must an Iterable or an array, but was " + type);
			}

			var codec = codecs.resolve(type, JdbcMedium.Internal);

			if (codec.isEmpty()) {
				throw new IllegalStateException("No JDBC codec can be obtained for " + type);
			}

			{
				var name = named != null ? named.value() : p.getName();
				var spreadWith = Optional.ofNullable(spread).map(Spread::prefix);
				boolean useBatch = batch != null;

				var profile = new ParameterProfile(name, useBatch, spreadWith, codec.get(), type);

				@Null var existing = builder.parametersByName.put(name, profile);
				if (existing != null) throw new IllegalStateException(
						"Method parameters have duplicate names, check @Named annotations: " + name);

				builder.parameters.add(profile);
			}
		}

		if (batchCount > 1) throw new IllegalStateException(
			"@Batch should not be present on more than one parameter on " + m);
	}

	static InvocationHandler handlerFor(Class<?> accessor, Codec.Resolver codecs,
		ConnectionProvider provider) {
		Set<String> methods = uniqueAccessMethods(accessor);

		// We allow "empty" interfaces as a way to access SQL (via getConnectionProvider() etc
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
			protected @Null Object handleInterfaceMethod(Object proxy, Method method, Object[] arguments)
					throws Throwable {
				if (isConnectionHandleMethod(method)) return provider.handle();

				String name = methodName(method);
				MethodSnippet snippet = snippets.get(name);
				MethodProfile profile = profiles.get(name);

				if (snippet == null) throw new AssertionError("SQL method not defined: " + name);

				try (
						var handle = provider.handle();
						var statement = handle.connection().prepareStatement(snippet.preparedStatements())) {
					prepareStatement(statement, profile, snippet, arguments);
					return executeStatement(statement, profile);
				} catch (SQLException sqlException) {
					throw Errors.refineException(source, method, snippet, sqlException);
				}
			}
		};
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
		PreparedStatementOut out = new PreparedStatementOut(profile.parameterIndex());

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
					fillStatement(statement, snippet.placeholders(), out);
					statement.addBatch();
				}
			} else {
				assert batch.getClass().isArray();
				int length = Array.getLength(batch);
				for (int i = 0; i < length; i++) {
					Object o = Array.get(batch, i);
					putArgument(out, batcher, batchIndex, o);
					fillStatement(statement, snippet.placeholders(), out);
					statement.addBatch();
				}
			}
		} else {
			for (int i = 0; i < parameters.size(); i++) {
				putArgument(out, parameters.get(i), i, args[i]);
			}
			fillStatement(statement, snippet.placeholders(), out);
		}
	}

	private static void putArgument(PreparedStatementOut out, ParameterProfile p, int index,
		Object value)
		throws IOException {
		out.putField(index);
		if (p.spread().isPresent()) {
			out.spread(p.spread().get());
		}
		p.codec().encode(out, value);
	}

	private static void fillStatement(
		PreparedStatement statement,
		List<String> placeholders,
		PreparedStatementOut out) throws SQLException, IOException {
		int i = 1;
		for (String p : placeholders) {
			@Null Object value = out.get(p);
			if (value instanceof Instant) {
				// Experimental, only for instant for now
				// TODO move to a codec for JdbcMedia?
				statement.setTimestamp(i, new Timestamp(((Instant) value).toEpochMilli()));
			} else {
				// this also covers null case for us,
				// do we need setNull with specific JDBC type?
				statement.setObject(i, value);
			}
			i++;
		}
	}

	private static SqlSource loadSqlSource(Class<?> accessorInterface) throws AssertionError {
		String filename = resourceFilenameFor(accessorInterface);
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
		try(var source = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
			char[] buffer = new char[8192];
			int length;
			while ((length = source.read(buffer)) != -1) {
				content.write(buffer, 0, length);
			}
		} catch (IOException readingClasspathResourceFailed) {
			throw new UncheckedIOException("Cannot read " + filename, readingClasspathResourceFailed);
		}

		return new SqlSource(filename, content, Source.Lines.from(content));
	}

	private static Map<String, MethodSnippet> parseSnippets(SqlSource source, Set<String> methods) {
		var snippets = parse(source.content(), source.lines());

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

		if (!problems.isEmpty()) throw new RuntimeException(
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
			throw new IllegalArgumentException(
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

	private static String resourceFilenameFor(Class<?> accessorInterface) {
		String canonicalName = accessorInterface.getCanonicalName();
		assert canonicalName != null : "precondition checked before";
		// not sure if null package can be for unnamed package, handling just in case
		Package packageObject = accessorInterface.getPackage();
		String packageName = packageObject != null ? packageObject.getName() : "";
		String packagePath = packageName.replace('.', '/');
		String resourceFilename;
		if (canonicalName.startsWith(packageName + ".")) {
			resourceFilename = packagePath + "/" + canonicalName.substring(packageName.length() + 1);
		} else { // may include case for unnamed packages etc
			resourceFilename = canonicalName;
		}
		return "/" + resourceFilename + ".sql";
	}

	private static Map<String, List<MethodSnippet>> parse(CharSequence content, Source.Lines lines) {
		var allMethods = new HashMap<String, List<MethodSnippet>>();

		class Parser {
			@Null MethodSnippet.Builder openBuilder;
			@Null Source.Range openRange;

			void parse() {
				for (int i = 1; i <= lines.count(); i++) {
					Source.Range range = lines.getLineRange(i);
					CharSequence line = range.get(content);
					String name = methodName(line);

					if (!name.isEmpty()) {
						// method identifier line
						// flush any open method and start
						// new method builder
						flushMethod(content, range);
						openMethod(name, range);
					} else {
						// regular statement line
						if (openBuilder != null) {
							// begin or expand range for open method
							openRange = openRange == null ? range : openRange.span(range);
						} else {
							// can collect unnamed leading lines for error reporting
							openMethod("", range);
						}
					}
				}

				var initialEmptyRange = Source.Range.of(Source.Position.of(0, 1, 1));
				flushMethod(content, initialEmptyRange);
			}

			String methodName(CharSequence line) {
				if (line.length() > 3
					&& line.charAt(0) == '-'
					&& line.charAt(1) == '-'
					&& line.charAt(2) == '.') {
					// can return empty string which is no method declared on this line
					// threat it as just an SQL comment. Or can be illegal name
					// anyway we expect these to be matched by the data access interface
					// method names and any discrepancy returned as errors
					return line.subSequence(3, line.length()).toString().trim();
				}
				return ""; // none
			}

			void openMethod(String name, Source.Range range) {
				openRange = null;
				openBuilder = new MethodSnippet.Builder();
				openBuilder.identifierRange = range;
				openBuilder.name = name;
			}

			void flushMethod(CharSequence content, Source.Range currentRange) {
				if (openBuilder != null) {
					if (openRange != null) prepareRange(content);
					else prepareEmpty(currentRange);

					allMethods.computeIfAbsent(openBuilder.name, k -> new ArrayList<>())
							.add(openBuilder.build());
				}
			}

			/**
			 * incomplete / empty method, defer error to runtime (like empty SQL statement)
			 * otherwise it would too painful during development
			 */
			void prepareEmpty(Source.Range currentRange) {
				assert openBuilder != null;
				openBuilder.statementsRange = Source.Range.of(currentRange.begin);
				openBuilder.preparedStatements = "--";
			}

			/**
			 * Parses source to extract placeholder list and also
			 * builds prepared statement where placeholders are substituted
			 * with '?' character to match the JDBC prepared statement syntax.
			 */
			void prepareRange(CharSequence content) {
				assert openBuilder != null;
				assert openRange != null;
				openBuilder.statementsRange = openRange;

				CharSequence source = content.subSequence(
					openRange.begin.position,
					openRange.end.position);

				StringBuilder buffer = new StringBuilder();
				Matcher matcher = PLACEHOLDER.matcher(source);
				while (matcher.find()) {
					if (source.charAt(matcher.start(0) + 1) == ':') {
						// first char is always ':' in this match, so
						// we look for the second char and see if we
						// ignore and append the same SQL verbatim, consider this
						// just type coercion expression starting with '::'.
						// Could be potentially "fixed" by just not matching
						// such sequences in the first place if we have better pattern, idk
						matcher.appendReplacement(buffer, "$0");
					} else {
						String placeholder = matcher.group(1);
						openBuilder.placeholders.add(placeholder);
						matcher.appendReplacement(buffer, "?");
						// append number of spaces to match the length of original
						// placeholder so SQL syntax error reporting would operate
						// on the same source positions/offsets as template definitions.
						for (int i = 0; i < placeholder.length(); i++) {
							buffer.append(' ');
						}
					}
				}
				matcher.appendTail(buffer);
				openBuilder.preparedStatements = buffer.toString();
			}
		}

		new Parser().parse();

		return Map.copyOf(allMethods);
	}

	static @Null Object executeStatement(PreparedStatement statement, MethodProfile profile)
		throws SQLException, IOException {

		Type returnType = profile.returnType();
		boolean useUpdateCount = profile.returnUpdateCount();
		boolean voidUpdateCount = useUpdateCount
			&& returnType == void.class;
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

				if (voidUpdateCount) {
					returnValue = null;
				} else if (sumUpdateCount) {
					returnValue = Arrays.stream(updates).sum();
				} else {
					returnValue = updates;
				}
			}
		} else { // not batching
			boolean hasResultSet = statement.execute();

			if (useUpdateCount) {
				if (voidUpdateCount) {
					returnValue = null;
				} else if (largeUpdateCount) { // long update count
					List<Long> updates = new ArrayList<>();
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
					List<Integer> updates = new ArrayList<>();
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
			} else { // reading result set (not update count)
				// for the return type, the codec must be present at this point, so AssertionError
				var codec = profile.returnTypeCodec().orElseThrow(AssertionError::new);

				boolean wasResultSet = false;

				returnValue = null; // cannot use clean branching below, have to initialize early

				if (hasResultSet) {
					var in = new ResultSetIn(statement.getResultSet());
					returnValue = codec.decode(in);
					wasResultSet = true;
				}

				for (int count; ; ) {
					count = statement.getUpdateCount();
					if (!hasResultSet && count < 0) break;
					hasResultSet = statement.getMoreResults();

					if (hasResultSet) {
						if (wasResultSet) throw new IllegalStateException(
							"Only single result set can be processes, Use sql UNION ALL to merge multiple " +
								"results");

						var in = new ResultSetIn(statement.getResultSet());
						returnValue = codec.decode(in);
						wasResultSet = true;
					}
				}

				if (!wasResultSet) throw new IllegalStateException(
					"ResultSet exected but there was none. Fix SQL query, or use void return type or " +
						"int/long @UpdateCount");
			} // end resultsets
		} // end not batch

		return returnValue;
	}

	private static final Pattern PLACEHOLDER = Pattern.compile(":{1,2}([a-zA-Z0-9.]+)");
}
