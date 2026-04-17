package io.github.connellite.logger;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * One-line format: {@code [yyyy-MM-dd HH:mm:ss][LEVEL] message}.
 */
public final class BracketTimestampLogFormatter extends Formatter {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final ZoneId zoneId;

    public BracketTimestampLogFormatter() {
        this(ZoneId.systemDefault());
    }

    public BracketTimestampLogFormatter(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    }

    @Override
    public String format(LogRecord record) {
        String ts = ZonedDateTime.ofInstant(record.getInstant(), zoneId).format(TIMESTAMP);
        String level = record.getLevel() != null ? record.getLevel().getName() : "UNKNOWN";
        return "[" + ts + "][" + level + "] " + formatMessage(record);
    }
}
