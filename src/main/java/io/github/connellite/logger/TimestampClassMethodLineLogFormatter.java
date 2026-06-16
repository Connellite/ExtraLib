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
 * {@code yyyy-MM-dd HH:mm:ss,SSS class method:line - message}.
 * <p>
 * Not recommended for production use because stack inspection can be relatively expensive.
 * <p>
 * Line number is inferred from the current stack and may be unavailable in some environments.
 */
public final class TimestampClassMethodLineLogFormatter extends Formatter {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS", Locale.ROOT);

    private final ZoneId zoneId;

    public TimestampClassMethodLineLogFormatter(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    }

    /**
     * Singleton with {@link ZoneId#systemDefault()}.
     */
    public static TimestampClassMethodLineLogFormatter getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final TimestampClassMethodLineLogFormatter INSTANCE =
                new TimestampClassMethodLineLogFormatter(ZoneId.systemDefault());
    }

    @Override
    public String format(LogRecord record) {
        String ts = ZonedDateTime.ofInstant(record.getInstant(), zoneId).format(TIMESTAMP);
        String source = resolveSource(record);
        return ts + " " + source + " - " + formatMessage(record) + formatThrown(record);
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

            if (currentClass.equals(TimestampClassMethodLineLogFormatter.class.getName())) {
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
