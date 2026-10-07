package io.github.connellite.logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncFileLogHandlerTest {

    @Test
    void publicConstructorWritesQueuedRecord(@TempDir Path dir) throws Exception {
        Path log = dir.resolve("public.log");
        AsyncFileLogHandler handler = new AsyncFileLogHandler(log);
        Logger logger = Logger.getLogger("async.public." + dir.getFileName());
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        logger.addHandler(handler);
        try {
            logger.info("from-public");
            handler.awaitIdle();
            handler.flush();
            assertTrue(Files.readString(log, StandardCharsets.UTF_8).contains("from-public"));
        } finally {
            logger.removeHandler(handler);
            handler.close();
        }
    }

    @Test
    void recordsStayInPublishOrder(@TempDir Path dir) throws Exception {
        Path log = dir.resolve("order.log");
        AsyncFileLogHandler handler = new AsyncFileLogHandler(log, queueOf(AsyncFileLogHandlerConfig.OVERFLOW_DROP_LAST, 100));
        try {
            handler.publish(record("alpha"));
            handler.publish(record("beta"));
            handler.awaitIdle();
            handler.flush();
            String text = Files.readString(log, StandardCharsets.UTF_8);
            assertTrue(text.contains("alpha"));
            assertTrue(text.indexOf("alpha") < text.indexOf("beta"));
        } finally {
            handler.close();
        }
    }

    @Test
    void publishAfterCloseDoesNotWriteUntilReopened(@TempDir Path dir) throws Exception {
        Path log = dir.resolve("reopen.log");
        AsyncFileLogHandler handler = new AsyncFileLogHandler(log, queueOf(AsyncFileLogHandlerConfig.OVERFLOW_DROP_LAST, 100));
        try {
            handler.publish(record("before-close"));
            handler.awaitIdle();
            handler.flush();
            handler.close();
            handler.publish(record("while-closed"));
            String closedText = Files.readString(log, StandardCharsets.UTF_8);
            assertTrue(closedText.contains("before-close"));
            assertFalse(closedText.contains("while-closed"));

            handler.open();
            handler.publish(record("after-reopen"));
            handler.awaitIdle();
            handler.flush();
            String reopened = Files.readString(log, StandardCharsets.UTF_8);
            assertTrue(reopened.contains("after-reopen"));
            assertFalse(reopened.contains("while-closed"));
        } finally {
            handler.close();
        }
    }

    @Test
    void overflowDropsLastQueuedRecord(@TempDir Path dir) throws Exception {
        Path log = dir.resolve("drop-last.log");
        BlockingHandler handler = new BlockingHandler(log, AsyncFileLogHandlerConfig.OVERFLOW_DROP_LAST);
        try {
            handler.publish(record("block"));
            assertTrue(handler.started.await(5, TimeUnit.SECONDS));
            handler.publish(record("queued"));
            handler.publish(record("newest"));
            handler.release.countDown();
            assertTrue(awaitText(handler, log, "newest"));
            String text = Files.readString(log, StandardCharsets.UTF_8);
            assertTrue(text.contains("newest"));
            assertFalse(text.contains("queued"));
        } finally {
            handler.release.countDown();
            handler.close();
        }
    }

    @Test
    void overflowDropsCurrentRecord(@TempDir Path dir) throws Exception {
        Path log = dir.resolve("drop-current.log");
        BlockingHandler handler = new BlockingHandler(log, AsyncFileLogHandlerConfig.OVERFLOW_DROP_CURRENT);
        try {
            handler.publish(record("block"));
            assertTrue(handler.started.await(5, TimeUnit.SECONDS));
            handler.publish(record("queued"));
            handler.publish(record("newest"));
            handler.release.countDown();
            assertTrue(awaitText(handler, log, "queued"));
            String text = Files.readString(log, StandardCharsets.UTF_8);
            assertTrue(text.contains("queued"));
            assertFalse(text.contains("newest"));
        } finally {
            handler.release.countDown();
            handler.close();
        }
    }

    @Test
    void fileLogHandlerConfigCopiesFileSettings() {
        AsyncFileLogHandlerConfig config = new AsyncFileLogHandlerConfig(512, 1000, true, 3, true, null, 99, 0);
        FileLogHandlerConfig file = config.fileLogHandlerConfig();
        assertEquals(512, file.bufferSize());
        assertEquals(1000L, file.maxFileBytes());
        assertTrue(file.rotateDaily());
        assertEquals(3, file.maxBackupFiles());
        assertTrue(file.compressRotatedGzip());
        assertNull(file.formatter());
        assertEquals(AsyncFileLogHandlerConfig.OVERFLOW_DROP_LAST, config.overflowDropType());
        assertEquals(1, config.maxRecords());
        assertEquals(file, config.fileLogHandlerConfig());
    }

    private static AsyncFileLogHandlerConfig queueOf(int overflowDropType, int maxRecords) {
        return new AsyncFileLogHandlerConfig(FileLogHandlerConfig.DEFAULT, overflowDropType, maxRecords);
    }

    private static boolean awaitText(AsyncFileLogHandler handler, Path log, String expected) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < end) {
            handler.flush();
            if (Files.exists(log) && Files.readString(log, StandardCharsets.UTF_8).contains(expected)) {
                return true;
            }
            Thread.sleep(20);
        }
        handler.flush();
        return Files.exists(log) && Files.readString(log, StandardCharsets.UTF_8).contains(expected);
    }

    private static LogRecord record(String message) {
        return new LogRecord(Level.INFO, message);
    }

    private static final class BlockingHandler extends AsyncFileLogHandler {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        private BlockingHandler(Path logFile, int overflowDropType) {
            super(logFile, queueOf(overflowDropType, 1));
        }

        @Override
        protected void publishInternal(LogRecord record) {
            if ("block".equals(record.getMessage())) {
                started.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
            super.publishInternal(record);
        }
    }
}
