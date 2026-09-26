package io.github.connellite.logger;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.LogRecord;

/**
 * One-line format similar to {@link TimestampClassMethodLineLogFormatter}, with the current thread:
 * {@code yyyy-MM-dd HH:mm:ss,SSS [thread] class method:line - message}.
 * <p>
 * Not recommended for production use because stack inspection can be relatively expensive.
 * <p>
 * Line number is inferred from the current stack and may be unavailable in some environments.
 */
public final class TimestampThreadClassMethodLineLogFormatter extends AbstractLogFormatter {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS", Locale.ROOT);

    public TimestampThreadClassMethodLineLogFormatter(ZoneId zoneId) {
        super(zoneId);
    }

    /**
     * Singleton with {@link ZoneId#systemDefault()}.
     */
    public static TimestampThreadClassMethodLineLogFormatter getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final TimestampThreadClassMethodLineLogFormatter INSTANCE =
                new TimestampThreadClassMethodLineLogFormatter(ZoneId.systemDefault());
    }

    @Override
    public String format(LogRecord record) {
        String ts = formatTimestamp(record, TIMESTAMP);
        return ts + " [" + threadName() + "] " + resolveSource(record)
                + " - " + formatMessage(record) + formatThrown(record);
    }

    private static String resolveSource(LogRecord record) {
        String className = record.getSourceClassName();
        String methodName = record.getSourceMethodName();
        Integer line = resolveLineNumber(className, methodName);

        if (className == null) {
            className = record.getLoggerName();
            if (className == null) {
                className = "";
            }
        }

        String source = className;
        if (methodName != null && !methodName.isEmpty()) {
            source = source + " " + methodName;
        }
        if (line != null && line > 0) {
            source = source + ":" + line;
        }
        return source;
    }

    private static Integer resolveLineNumber(String className, String methodName) {
        if (className == null) {
            return null;
        }

        StackTraceElement[] stack = new Throwable().getStackTrace();

        boolean foundFormatter = false;
        for (StackTraceElement element : stack) {
            String currentClass = element.getClassName();

            if (currentClass.equals(TimestampThreadClassMethodLineLogFormatter.class.getName())) {
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
