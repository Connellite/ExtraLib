package io.github.connellite.logger;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.LogRecord;

/**
 * One-line format: {@code [yyyy-MM-dd HH:mm:ss][LEVEL][thread] message}.
 * Level text, {@link #formatMessage(LogRecord)}, and {@link LogRecord#getThrown()} match {@link java.util.logging.SimpleFormatter}.
 */
public final class BracketTimestampThreadLogFormatter extends AbstractLogFormatter {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    public BracketTimestampThreadLogFormatter(ZoneId zoneId) {
        super(zoneId);
    }

    /**
     * Singleton with {@link ZoneId#systemDefault()}.
     */
    public static BracketTimestampThreadLogFormatter getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final BracketTimestampThreadLogFormatter INSTANCE = new BracketTimestampThreadLogFormatter(ZoneId.systemDefault());
    }

    @Override
    public String format(LogRecord record) {
        String ts = formatTimestamp(record, TIMESTAMP);
        return "[" + ts + "][" + formatLevel(record) + "][" + threadName() + "] "
                + formatMessage(record) + formatThrown(record);
    }
}
