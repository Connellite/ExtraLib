package io.github.connellite.util;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * JMH microbenchmarks: the single-pass scanner behind {@link DateTimeUtil} against the pattern
 * list it replaced and against {@link LocalDate#parse(CharSequence)}.
 *
 * <p>{@code legacy*} methods reproduce the previous implementation, which tried every formatter in
 * turn and used {@link DateTimeParseException} as control flow.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 1, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class DateTimeParseBenchmark {

    private static final String ISO_DATE = "2024-03-21";
    private static final String ISO_DATE_TIME = "2024-03-21T10:15:30";
    private static final String SQL_TIMESTAMP = "2024-03-21 10:15:30.123";
    private static final String DOTTED_DATE = "21.03.2024";
    private static final String RUSSIAN_MONTH = "21 марта 2024";
    private static final String GARBAGE = "not-a-date";

    private static final List<DateTimeFormatter> LEGACY_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.RFC_1123_DATE_TIME,

            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter(),

            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy/MM/dd HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter(),

            new DateTimeFormatterBuilder()
                    .appendPattern("dd.MM.yyyy HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter(),

            new DateTimeFormatterBuilder()
                    .appendPattern("dd/MM/yyyy HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter(),

            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ISO_DATE,
            DateTimeFormatter.BASIC_ISO_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),

            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd MMM yyyy")
                    .toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("yyyy MMM dd")
                    .toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd MMM yyyy")
                    .toFormatter(new Locale("ru")),
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd MMMM yyyy")
                    .toFormatter(new Locale("ru"))
    );

    @Benchmark
    public LocalDate scanner_isoDate() {
        return DateTimeUtil.tryParseLocalDate(ISO_DATE);
    }

    @Benchmark
    public LocalDate legacy_isoDate() {
        return legacyParseDate(ISO_DATE);
    }

    @Benchmark
    public LocalDate jdk_isoDate() {
        return LocalDate.parse(ISO_DATE);
    }

    @Benchmark
    public LocalDateTime scanner_isoDateTime() {
        return DateTimeUtil.tryParseLocalDateTime(ISO_DATE_TIME);
    }

    @Benchmark
    public LocalDateTime legacy_isoDateTime() {
        return legacyParseDateTime(ISO_DATE_TIME);
    }

    @Benchmark
    public LocalDateTime jdk_isoDateTime() {
        return LocalDateTime.parse(ISO_DATE_TIME);
    }

    @Benchmark
    public LocalDateTime scanner_sqlTimestamp() {
        return DateTimeUtil.tryParseLocalDateTime(SQL_TIMESTAMP);
    }

    @Benchmark
    public LocalDateTime legacy_sqlTimestamp() {
        return legacyParseDateTime(SQL_TIMESTAMP);
    }

    @Benchmark
    public LocalDate scanner_dottedDate() {
        return DateTimeUtil.tryParseLocalDate(DOTTED_DATE);
    }

    @Benchmark
    public LocalDate legacy_dottedDate() {
        return legacyParseDate(DOTTED_DATE);
    }

    @Benchmark
    public LocalDate scanner_russianMonth() {
        return DateTimeUtil.tryParseLocalDate(RUSSIAN_MONTH);
    }

    @Benchmark
    public LocalDate legacy_russianMonth() {
        return legacyParseDate(RUSSIAN_MONTH);
    }

    @Benchmark
    public LocalDate scanner_garbage() {
        return DateTimeUtil.tryParseLocalDate(GARBAGE);
    }

    @Benchmark
    public LocalDate legacy_garbage() {
        try {
            return legacyParseDate(GARBAGE);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LocalDate legacyParseDate(String text) {
        text = text.trim();
        try {
            return OffsetDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }
        for (DateTimeFormatter formatter : LEGACY_FORMATTERS) {
            try {
                return LocalDateTime.parse(text, formatter).toLocalDate();
            } catch (DateTimeParseException ignored) {
                try {
                    return LocalDate.parse(text, formatter);
                } catch (DateTimeParseException ignored2) {
                }
            }
        }
        throw new IllegalArgumentException("Unparseable date: '" + text + "'");
    }

    private static LocalDateTime legacyParseDateTime(String text) {
        text = text.trim();
        try {
            return OffsetDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        for (DateTimeFormatter formatter : LEGACY_FORMATTERS) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                try {
                    return LocalDate.parse(text, formatter).atStartOfDay();
                } catch (DateTimeParseException ignored2) {
                }
            }
        }
        throw new IllegalArgumentException("Unparseable date-time: '" + text + "'");
    }
}
