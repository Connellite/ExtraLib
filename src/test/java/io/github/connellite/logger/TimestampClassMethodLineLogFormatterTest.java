package io.github.connellite.logger;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TimestampClassMethodLineLogFormatterTest {

    @Test
    void resolvesLineNumberWhenUsedByStandardHandler() {
        Logger logger = Logger.getLogger("line.formatter." + UUID.randomUUID());
        CapturingHandler handler = new CapturingHandler();
        handler.setFormatter(TimestampClassMethodLineLogFormatter.getInstance());
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        logger.addHandler(handler);
        try {
            logWithStandardHandler(logger);

            assertTrue(Pattern.compile(
                    ".* " + Pattern.quote(TimestampClassMethodLineLogFormatterTest.class.getName())
                            + " logWithStandardHandler:\\d+ - hello.*",
                    Pattern.DOTALL
            ).matcher(handler.formatted).matches());
        } finally {
            logger.removeHandler(handler);
        }
    }

    private static void logWithStandardHandler(Logger logger) {
        logger.logp(
                Level.INFO,
                TimestampClassMethodLineLogFormatterTest.class.getName(),
                "logWithStandardHandler",
                "hello"
        );
    }

    private static final class CapturingHandler extends Handler {

        private String formatted;

        @Override
        public void publish(LogRecord record) {
            formatted = getFormatter().format(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
