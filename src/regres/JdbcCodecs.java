package io.immutables.regres;

import io.immutables.codec.Codec;
import io.immutables.codec.Medium;
import io.immutables.meta.Null;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class JdbcCodecs
		implements Codec.Factory<ResultIn, StatementOut>, Codec.SupportedTypes {
	private JdbcCodecs() {}

	private static final ZoneId UTC = ZoneId.of("Z").normalized();

	private static final Map<Class<?>, Codec<?, ResultIn, StatementOut>> codecs =
		Map.ofEntries(
				Map.entry(UUID.class, new SpecialCodec<UUID>(
						StatementOut::putSpecial,
						in -> (UUID) in.takeSpecial())),
				// Note! timestampz doesn't store!! timezone, just converts to UTC
				// Do not rely on this datatype for offset, store it as TIME in a separate column!
				Map.entry(OffsetDateTime.class, new SpecialCodec<OffsetDateTime>(
						(out, v) -> out.putSpecial(Types.TIMESTAMP_WITH_TIMEZONE, v),
						in -> OffsetDateTime.ofInstant(((Timestamp) in.takeSpecial()).toInstant(), UTC))),
				// This one stored as timestamp too, so need to be carefully think
				// about that it's a LOCAL data time
				Map.entry(LocalDateTime.class, new SpecialCodec<LocalDateTime>(
						(out, v) -> out.putSpecial(Types.TIMESTAMP, v),
						in -> ((Timestamp) in.takeSpecial()).toLocalDateTime())),
				Map.entry(LocalDate.class, new SpecialCodec<LocalDate>(
						(out, v) -> out.putSpecial(Types.DATE, v),
						in -> ((Date) in.takeSpecial()).toLocalDate())),
				Map.entry(LocalTime.class, new SpecialCodec<LocalTime>(
						(out, v) -> out.putSpecial(Types.DATE, v),
						in -> ((Time) in.takeSpecial()).toLocalTime())),
				Map.entry(Instant.class, new SpecialCodec<Instant>(
						(out, v) -> out.putSpecial(new Timestamp(v.toEpochMilli())),
						in -> ((Timestamp) in.takeSpecial()).toInstant()))
		);

	private static final Set<Class<?>> supports = codecs.keySet();

	@Override
	public @Null Codec<?, ResultIn, StatementOut> tryCreate(
			Type type, Class<?> raw,
			Medium<? extends ResultIn, ? extends StatementOut> medium,
			Codec.Lookup<ResultIn, StatementOut> lookup) {
		return medium == JdbcMedium.Jdbc ? codecs.get(raw) : null;
	}

	@Override
	public Set<Class<?>> supportedRawTypes() {
		return supports;
	}

	public static final JdbcCodecs Instance = new JdbcCodecs();

	private static final class SpecialCodec<T> extends Codec<T, ResultIn, StatementOut> {
		private final BiConsumer<StatementOut, T> encode;
		private final Function<ResultIn, T> decode;

		SpecialCodec(
				BiConsumer<StatementOut, T> encode,
				Function<ResultIn, T> decode) {
			this.encode = encode;
			this.decode = decode;
		}

		@Override
		public void encode(StatementOut out, T instance) throws IOException {
			encode.accept(out, instance);
		}

		@Override
		public T decode(ResultIn in) throws IOException {
			return decode.apply(in);
			//(T) in.takeSpecial()
		}
	}
}
