package io.github.connellite.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * One-line format similar to classic application logs:
 * {@code yyyy-MM-dd HH:mm:ss,SSS source - message}, where {@code source} is resolved like
 * {@link java.util.logging.SimpleFormatter} (caller class, or class plus method, or logger name).
 * {@link #formatMessage(LogRecord)} and {@link LogRecord#getThrown()} match {@link java.util.logging.SimpleFormatter}.
 */
public final class TimestampClassMethodLogFormatter extends Formatter {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS", Locale.ROOT);

    private final ZoneId zoneId;

    public TimestampClassMethodLogFormatter(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
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
        String ts = ZonedDateTime.ofInstant(record.getInstant(), zoneId).format(TIMESTAMP);
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

    /**
     * Same trailing exception text as {@link java.util.logging.SimpleFormatter}.
     */
    private static String formatThrown(LogRecord record) {
        Throwable thrown = record.getThrown();
        if (thrown == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println();
        thrown.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }
}
