package io.github.connellite.logger;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * One-line format similar to classic application logs:
 * {@code yyyy-MM-dd HH:mm:ss,SSS fully.qualified.Class methodName - message}.
 */
public final class TimestampClassMethodLogFormatter extends Formatter {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS", Locale.ROOT);

    private final ZoneId zoneId;

    public TimestampClassMethodLogFormatter() {
        this(ZoneId.systemDefault());
    }

    public TimestampClassMethodLogFormatter(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    }

    @Override
    public String format(LogRecord record) {
        String ts = ZonedDateTime.ofInstant(record.getInstant(), zoneId).format(TIMESTAMP);
        String cls = record.getSourceClassName();
        if (cls == null || cls.isEmpty()) {
            cls = record.getLoggerName();
        }
        if (cls == null) {
            cls = "";
        }
        String method = record.getSourceMethodName();
        if (method == null || method.isEmpty()) {
            method = "?";
        }
        return ts + " " + cls + " " + method + " - " + formatMessage(record);
    }
}
