package io.github.connellite.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Shared JUL formatter helpers: zone, timestamp, level, thread, and thrown text.
 */
public abstract class AbstractLogFormatter extends Formatter {

    protected final ZoneId zoneId;

    protected AbstractLogFormatter(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    }

    protected String formatTimestamp(LogRecord record, DateTimeFormatter timestamp) {
        return ZonedDateTime.ofInstant(record.getInstant(), zoneId).format(timestamp);
    }

    protected static String formatLevel(LogRecord record) {
        return record.getLevel() != null ? record.getLevel().getName() : "UNKNOWN";
    }

    protected static String threadName() {
        String name = Thread.currentThread().getName();
        return name != null ? name : "";
    }

    /**
     * Same trailing exception text as {@link java.util.logging.SimpleFormatter}.
     */
    protected static String formatThrown(LogRecord record) {
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
