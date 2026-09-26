package io.github.connellite.logger;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.LogRecord;

/**
 * One-line format similar to classic application logs:
 * {@code yyyy-MM-dd HH:mm:ss,SSS source - message}, where {@code source} is resolved like
 * {@link java.util.logging.SimpleFormatter} (caller class, or class plus method, or logger name).
 * {@link #formatMessage(LogRecord)} and {@link LogRecord#getThrown()} match {@link java.util.logging.SimpleFormatter}.
 */
public final class TimestampClassMethodLogFormatter extends AbstractLogFormatter {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS", Locale.ROOT);

    public TimestampClassMethodLogFormatter(ZoneId zoneId) {
        super(zoneId);
    }

    /**
     * Singleton with {@link ZoneId#systemDefault()}.
     */
    public static TimestampClassMethodLogFormatter getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final TimestampClassMethodLogFormatter INSTANCE = new TimestampClassMethodLogFormatter(ZoneId.systemDefault());
    }

    @Override
    public String format(LogRecord record) {
        String ts = formatTimestamp(record, TIMESTAMP);
        String source;
        if (record.getSourceClassName() != null) {
            source = record.getSourceClassName();
            if (record.getSourceMethodName() != null) {
                source = source + " " + record.getSourceMethodName();
            }
        } else {
            source = record.getLoggerName();
            if (source == null) {
                source = "";
            }
        }
        return ts + " " + source + " - " + formatMessage(record) + formatThrown(record);
    }
}
