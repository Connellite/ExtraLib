package io.github.connellite.logger;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.LogRecord;

/**
 * One-line format: {@code [yyyy-MM-dd HH:mm:ss][LEVEL][thread]:line message}.
 * Line number is inferred from the current stack when the source class/method is known.
 * Level text, {@link #formatMessage(LogRecord)}, and {@link LogRecord#getThrown()} match {@link java.util.logging.SimpleFormatter}.
 */
public final class BracketTimestampThreadLineLogFormatter extends AbstractLogFormatter {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    public BracketTimestampThreadLineLogFormatter(ZoneId zoneId) {
        super(zoneId);
    }

    /**
     * Singleton with {@link ZoneId#systemDefault()}.
     */
    public static BracketTimestampThreadLineLogFormatter getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final BracketTimestampThreadLineLogFormatter INSTANCE =
                new BracketTimestampThreadLineLogFormatter(ZoneId.systemDefault());
    }

    @Override
    public String format(LogRecord record) {
        String ts = formatTimestamp(record, TIMESTAMP);
        Integer line = resolveLineNumber(record.getSourceClassName(), record.getSourceMethodName());
        String suffix = line != null ? ":" + line : "";
        return "[" + ts + "][" + formatLevel(record) + "][" + threadName() + "]" + suffix
                + " " + formatMessage(record) + formatThrown(record);
    }

    private static Integer resolveLineNumber(String className, String methodName) {
        if (className == null) {
            return null;
        }

        StackTraceElement[] stack = new Throwable().getStackTrace();

        boolean foundFormatter = false;
        for (StackTraceElement element : stack) {
            String currentClass = element.getClassName();

            if (currentClass.equals(BracketTimestampThreadLineLogFormatter.class.getName())) {
                foundFormatter = true;
                continue;
            }

            if (foundFormatter) {
                if (!Objects.equals(className, currentClass) || (methodName != null && !Objects.equals(methodName, element.getMethodName()))) {
                    continue;
                }
                int lineNumber = element.getLineNumber();
                return lineNumber > 0 ? lineNumber : null;
            }
        }
        return null;
    }
}
