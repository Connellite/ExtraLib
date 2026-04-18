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
 * One-line format: {@code [yyyy-MM-dd HH:mm:ss][LEVEL] message}.
 * Level text, {@link #formatMessage(LogRecord)}, and {@link LogRecord#getThrown()} match {@link java.util.logging.SimpleFormatter}.
 */
public final class BracketTimestampLogFormatter extends Formatter {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final ZoneId zoneId;

    public BracketTimestampLogFormatter(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    }

    /**
     * Singleton with {@link ZoneId#systemDefault()}.
     */
    public static BracketTimestampLogFormatter getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final BracketTimestampLogFormatter INSTANCE = new BracketTimestampLogFormatter(ZoneId.systemDefault());
    }

    @Override
    public String format(LogRecord record) {
        String ts = ZonedDateTime.ofInstant(record.getInstant(), zoneId).format(TIMESTAMP);
        String level =
                record.getLevel() != null ? record.getLevel().getName() : "UNKNOWN";
        return "[" + ts + "][" + level + "] " + formatMessage(record) + formatThrown(record);
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
