package io.github.connellite.logger;

import io.github.connellite.compress.CompressFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("deprecation")
class BufferedFileLogHandlerTest {

    @Test
    void publishKeepsTheLineUntilFlush(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("now.log");
        BufferedFileLogHandler handler = new BufferedFileLogHandler(log);
        try {
            handler.publish(record("hello"));
            assertFalse(Files.exists(log));
            handler.flush();
            assertFileIsNotHeld(log);
            String content = Files.readString(log, StandardCharsets.UTF_8);
            assertTrue(content.contains("hello"));
            assertTrue(content.contains("INFO"));
        } finally {
            handler.close();
        }
    }

    @Test
    void customFormatterFromConfigIsUsed(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("custom.log");
        Formatter fmt = new Formatter() {
            @Override
            public String format(LogRecord record) {
                return "CUSTOM:" + record.getMessage();
            }
        };
        Logger logger = FileLoggers.forBufferedLogFile("pm-custom", log, FileLogHandlerConfig.DEFAULT.withFormatter(fmt));
        logger.setLevel(Level.INFO);
        try {
            logger.log(Level.INFO, "ping");
            for (Handler handler : logger.getHandlers()) {
                handler.flush();
            }
            assertEquals("CUSTOM:ping\n", Files.readString(log, StandardCharsets.UTF_8));
        } finally {
            closeHandlers(logger);
        }
    }

    @Test
    void formatterFailureDoesNotCreateTheFile(@TempDir Path dir) {
        Path log = dir.resolve("bad.log");
        Formatter fmt = new Formatter() {
            @Override
            public String format(LogRecord record) {
                throw new IllegalStateException("boom");
            }
        };
        BufferedFileLogHandler handler = new BufferedFileLogHandler(log, FileLogHandlerConfig.DEFAULT.withFormatter(fmt));
        try {
            handler.publish(record("nope"));
            assertFalse(Files.exists(log));
        } finally {
            handler.close();
        }
    }

    @Test
    void rotatesWhenSizeExceeded(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("rot.log");
        FileLogHandlerConfig cfg = FileLogHandlerConfig.DEFAULT
                .withFormatter(messageFormatter())
                .withBufferSize(256)
                .withMaxFileBytes(1)
                .withMaxBackupFiles(3);
        BufferedFileLogHandler handler = new BufferedFileLogHandler(log, cfg);
        try {
            handler.publish(record("F".repeat(256)));
            handler.publish(record("S".repeat(256)));
            assertFileIsNotHeld(log);
            assertTrue(Files.readString(dir.resolve("rot.log.0"), StandardCharsets.UTF_8).contains("F".repeat(256)));
            assertTrue(Files.readString(log, StandardCharsets.UTF_8).contains("S".repeat(256)));
            assertFalse(Files.readString(log, StandardCharsets.UTF_8).contains("F".repeat(256)));
        } finally {
            handler.close();
        }
    }

    @Test
    void rotateDailyShiftsStaleFileByLastModifiedDate(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("daily.log");
        Files.writeString(log, "old-day", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(log, FileTime.from(Instant.now().minus(2, ChronoUnit.DAYS)));
        FileLogHandlerConfig cfg = FileLogHandlerConfig.DEFAULT.withRotateDaily(true).withMaxBackupFiles(4);
        BufferedFileLogHandler handler = new BufferedFileLogHandler(log, cfg);
        try {
            handler.publish(record("fresh-day"));
            handler.flush();
            assertFileIsNotHeld(log);
            assertTrue(Files.readString(dir.resolve("daily.log.0"), StandardCharsets.UTF_8).contains("old-day"));
            String current = Files.readString(log, StandardCharsets.UTF_8);
            assertTrue(current.contains("fresh-day"));
            assertFalse(current.contains("old-day"));
        } finally {
            handler.close();
        }
    }

    @Test
    void rotatesToGzipWhenCompressionEnabled(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("gzip.log");
        FileLogHandlerConfig cfg = FileLogHandlerConfig.DEFAULT
                .withFormatter(messageFormatter())
                .withBufferSize(256)
                .withMaxFileBytes(1)
                .withMaxBackupFiles(3)
                .withCompressRotatedGzip(true);
        BufferedFileLogHandler handler = new BufferedFileLogHandler(log, cfg);
        try {
            handler.publish(record("first-line" + "F".repeat(246)));
            handler.publish(record("second-line" + "S".repeat(245)));
            assertFileIsNotHeld(log);
            Path gzBackup = dir.resolve("gzip.log.0.gz");
            assertTrue(Files.isRegularFile(gzBackup));
            Path decompressed = dir.resolve("gzip.log.0");
            CompressFile.decompressGzipFile(gzBackup.toFile(), decompressed.toFile());
            assertTrue(Files.readString(decompressed, StandardCharsets.UTF_8).contains("first-line"));
            assertTrue(Files.readString(log, StandardCharsets.UTF_8).contains("second-line"));
        } finally {
            handler.close();
        }
    }

    @Test
    void headOncePerSegmentAndTailOnRotateAndClose(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("wrap.log");
        Formatter fmt = new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getMessage();
            }

            @Override
            public String getHead(Handler h) {
                return "HEAD\n";
            }

            @Override
            public String getTail(Handler h) {
                return "TAIL\n";
            }
        };
        FileLogHandlerConfig cfg = FileLogHandlerConfig.DEFAULT.withFormatter(fmt).withMaxFileBytes(1).withMaxBackupFiles(2);
        BufferedFileLogHandler handler = new BufferedFileLogHandler(log, cfg);
        try {
            handler.publish(record("one"));
            handler.flush();
            assertEquals("HEAD\none\n", Files.readString(log, StandardCharsets.UTF_8));
            handler.publish(record("two"));
            handler.flush();
            assertEquals("HEAD\none\nTAIL\n", Files.readString(dir.resolve("wrap.log.0"), StandardCharsets.UTF_8));
            assertEquals("HEAD\ntwo\n", Files.readString(log, StandardCharsets.UTF_8));
            handler.close();
            assertEquals("HEAD\ntwo\nTAIL\n", Files.readString(log, StandardCharsets.UTF_8));
            handler.publish(record("three"));
            assertEquals("HEAD\ntwo\nTAIL\n", Files.readString(log, StandardCharsets.UTF_8));
        } finally {
            handler.close();
        }
    }

    @Test
    void flushWritesTheBufferAndCloseStopsFurtherWrites(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("life.log");
        BufferedFileLogHandler handler = new BufferedFileLogHandler(log, FileLogHandlerConfig.DEFAULT.withFormatter(messageFormatter()));
        try {
            handler.flush();
            assertFalse(Files.exists(log));
            handler.publish(record("first"));
            assertFalse(Files.exists(log));
            handler.flush();
            assertFileIsNotHeld(log);
            assertEquals("first\n", Files.readString(log, StandardCharsets.UTF_8));
            handler.publish(record("second"));
            assertEquals("first\n", Files.readString(log, StandardCharsets.UTF_8));
            handler.flush();
            assertEquals("first\nsecond\n", Files.readString(log, StandardCharsets.UTF_8));
            handler.publish(record("left"));
            handler.close();
            String afterClose = Files.readString(log, StandardCharsets.UTF_8);
            assertEquals("first\nsecond\nleft\n", afterClose);
            handler.publish(record("third"));
            handler.flush();
            assertEquals(afterClose, Files.readString(log, StandardCharsets.UTF_8));
        } finally {
            handler.close();
        }
    }

    @Test
    void repeatedFactoryCallsKeepASingleHandler(@TempDir Path dir) {
        Path log = dir.resolve("dedup.log");
        FileLogHandlerConfig cfg = FileLogHandlerConfig.DEFAULT;
        Logger named = FileLoggers.forBufferedLogFile("pm-dedup", log, cfg);
        try {
            Logger again = FileLoggers.forBufferedLogFile("pm-dedup", log, cfg);
            assertSame(named, again);
            assertEquals(1, named.getHandlers().length);
            assertTrue(named.getHandlers()[0] instanceof BufferedFileLogHandler);

            Logger added = Logger.getLogger("pm-add");
            added.setUseParentHandlers(false);
            BufferedFileLogHandler first = FileLoggers.addBufferedFileHandler(added, log, cfg);
            BufferedFileLogHandler second = FileLoggers.addBufferedFileHandler(added, log, cfg);
            assertSame(first, second);
            assertEquals(1, added.getHandlers().length);
        } finally {
            closeHandlers(named);
            closeHandlers(Logger.getLogger("pm-add"));
        }
    }

    @Test
    void addingEitherHandlerReplacesTheOtherForTheSameFile(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("swap.log");
        Logger logger = Logger.getLogger("pm-swap");
        logger.setUseParentHandlers(false);
        try {
            FileLoggers.addFileHandler(logger, log);
            FileLoggers.addBufferedFileHandler(logger, log);
            assertEquals(1, logger.getHandlers().length);
            assertTrue(logger.getHandlers()[0] instanceof BufferedFileLogHandler);

            FileLoggers.addFileHandler(logger, log);
            assertEquals(1, logger.getHandlers().length);
            assertTrue(logger.getHandlers()[0] instanceof FileLogHandler);
            assertFalse(logger.getHandlers()[0] instanceof BufferedFileLogHandler);
        } finally {
            closeHandlers(logger);
        }
    }

    /**
     * A still-open {@link java.io.FileOutputStream} on Windows rejects a rename. On Linux the rename succeeds, so open
     * descriptors are checked through {@code /proc/self/fd}.
     */
    private static void assertFileIsNotHeld(Path log) throws IOException {
        Path moved = log.resolveSibling(log.getFileName().toString() + ".released");
        try {
            Files.move(log, moved);
        } catch (IOException ex) {
            fail("log file is still open", ex);
        }
        Files.move(moved, log);
        Path fdDir = Path.of("/proc/self/fd");
        if (!Files.isDirectory(fdDir)) {
            return;
        }
        try (Stream<Path> fds = Files.list(fdDir)) {
            for (Path fd : fds.toList()) {
                Path target;
                try {
                    target = Files.readSymbolicLink(fd);
                } catch (IOException ex) {
                    continue;
                }
                if (Files.exists(target) && Files.isSameFile(target, log)) {
                    fail("log file is still open: " + fd);
                }
            }
        }
    }

    private static LogRecord record(String message) {
        return new LogRecord(Level.INFO, message);
    }

    private static Formatter messageFormatter() {
        return new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getMessage();
            }
        };
    }

    private static void closeHandlers(Logger logger) {
        for (Handler handler : logger.getHandlers()) {
            handler.close();
            logger.removeHandler(handler);
        }
    }
}
