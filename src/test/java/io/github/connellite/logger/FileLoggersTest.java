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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileLoggersTest {

    @Test
    void customFormatterFromConfigIsUsed(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("custom.log");
        Formatter fmt = new Formatter() {
            @Override
            public String format(LogRecord record) {
                return "CUSTOM:" + record.getMessage();
            }
        };
        FileLogHandlerConfig cfg = FileLogHandlerConfig.DEFAULT.withFormatter(fmt);
        Logger logger = FileLoggers.forLogFile("custom.log", log, cfg);
        logger.setLevel(Level.INFO);
        try {
            logger.log(Level.INFO, "ping");
            for (var h : logger.getHandlers()) {
                h.flush();
            }
            String content = Files.readString(log, StandardCharsets.UTF_8).trim();
            assertEquals("CUSTOM:ping", content);
        } finally {
            for (var h : logger.getHandlers()) {
                h.close();
            }
        }
    }

    @Test
    void writesSimpleFormatterAfterFlush(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("app.log");
        Logger logger = FileLoggers.forLogFile("app.log", log);
        logger.setLevel(Level.INFO);
        try {
            logger.log(Level.INFO, "hello");
            for (var h : logger.getHandlers()) {
                h.flush();
            }
            String content = Files.readString(log, StandardCharsets.UTF_8);
            assertTrue(content.contains("INFO"));
            assertTrue(content.contains("hello"));
        } finally {
            for (var h : logger.getHandlers()) {
                h.close();
            }
        }
    }

    @Test
    void repeatedForLogFileWithSameLoggerNameReplacesHandlerInLoop(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("repeat.log");
        String loggerName = "repeat.same";
        Logger logger = null;
        try {
            for (int i = 0; i < 100; i++) {
                logger = FileLoggers.forLogFile(loggerName, log);
                logger.setLevel(Level.INFO);
                assertEquals(1, logger.getHandlers().length, "single FileLogHandler after iteration " + i);
            }
            logger.log(Level.INFO, "after-loop");
            for (var h : logger.getHandlers()) {
                h.flush();
            }
            String content = Files.readString(log, StandardCharsets.UTF_8);
            assertTrue(content.contains("after-loop"));
        } finally {
            if (logger != null) {
                for (var h : logger.getHandlers()) {
                    h.close();
                }
            }
        }
    }

    @Test
    void concurrentForLogFileWithSameLoggerName(@TempDir Path dir) throws Exception {
        Path log = dir.resolve("concurrent.log");
        String loggerName = "concurrent.same";
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                futures.add(executor.submit(() -> {
                    Logger logger = FileLoggers.forLogFile(loggerName, log);
                    logger.setLevel(Level.INFO);
                    logger.info("Concurrent log");
                }));
            }
            executor.shutdown();
            assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));
            for (Future<?> f : futures) {
                f.get();
            }
            Logger logger = Logger.getLogger(loggerName);
            for (var h : logger.getHandlers()) {
                h.flush();
            }
            String content = Files.readString(log, StandardCharsets.UTF_8);
            assertEquals(100, content.lines().filter(line -> line.contains("Concurrent log")).count());
        } finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Logger logger = Logger.getLogger(loggerName);
            for (var h : logger.getHandlers()) {
                h.close();
            }
        }
    }

    @Test
    void pathOverloadUsesFileNameAsLoggerName(@TempDir Path dir) {
        Path log = dir.resolve("named.log");
        Logger a = FileLoggers.forLogFile(log);
        try {
            Logger b = Logger.getLogger("named.log");
            assertEquals(b.getName(), a.getName());
        } finally {
            for (var h : a.getHandlers()) {
                h.close();
            }
        }
    }

    @Test
    void twoLoggersSameNormalizedPathSecondForLogFileThrows(@TempDir Path dir) {
        Path log1 = dir.resolve("a").resolve("shared.log");
        Path log2 = dir.resolve("b").resolve("..").resolve("a").resolve("shared.log");
        Logger first = FileLoggers.forLogFile("one.logger", log1);
        try {
            assertThrows(IllegalStateException.class, () -> FileLoggers.forLogFile("other.logger", log2));
        } finally {
            for (var h : first.getHandlers()) {
                h.close();
            }
        }
    }

    @Test
    void samePathAllowedAfterFirstHandlerClosed(@TempDir Path dir) {
        Path log1 = dir.resolve("a").resolve("reuse.log");
        Path log2 = dir.resolve("b").resolve("..").resolve("a").resolve("reuse.log");
        Logger first = FileLoggers.forLogFile("reuse.a", log1);
        for (var h : first.getHandlers()) {
            h.close();
        }
        Logger second = FileLoggers.forLogFile("reuse.b", log2);
        try {
            assertEquals(1, second.getHandlers().length);
        } finally {
            for (var h : second.getHandlers()) {
                h.close();
            }
        }
    }

    @Test
    void addFileHandlerDoesNotDuplicateSamePath(@TempDir Path dir) {
        Path log = dir.resolve("dup.log");
        Logger logger = Logger.getLogger("test.dup");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        try {
            FileLoggers.addFileHandler(logger, log);
            FileLoggers.addFileHandler(logger, log);
            int fileHandlers = 0;
            for (var h : logger.getHandlers()) {
                if (h instanceof FileLogHandler) {
                    fileHandlers++;
                }
            }
            assertEquals(1, fileHandlers);
        } finally {
            for (var h : logger.getHandlers()) {
                h.close();
            }
        }
    }

    @Test
    void rotatesWhenSizeExceeded(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("rot.log");
        FileLogHandlerConfig cfg = FileLogHandlerConfig.DEFAULT.withMaxFileBytes(80).withMaxBackupFiles(3);
        Logger logger = FileLoggers.forLogFile("rot", log, cfg);
        logger.setLevel(Level.INFO);
        for (int i = 0; i < 20; i++) {
            logger.log(Level.INFO, "line-" + i + " padding padding padding");
        }
        for (var h : logger.getHandlers()) {
            h.close();
        }
        long segments;
        try (Stream<Path> stream = Files.list(dir)) {
            segments = stream.map(Path::getFileName).map(Path::toString).filter(n -> n.startsWith("rot.log")).count();
        }
        assertTrue(segments >= 2, "expected rotated segments (main and at least one .N)");
    }

    @Test
    void createsNestedLogDirectories(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("logs").resolve("deep").resolve("nest.log");
        Logger logger = FileLoggers.forLogFile("nest.log", log);
        logger.setLevel(Level.INFO);
        try {
            logger.info("x");
            for (var h : logger.getHandlers()) {
                h.flush();
            }
            assertTrue(Files.isRegularFile(log));
        } finally {
            for (var h : logger.getHandlers()) {
                h.close();
            }
        }
    }

    @Test
    void rotationDropsNumericSuffixesBeyondLimit(@TempDir Path dir) throws IOException {
        Path main = dir.resolve("keep.log");
        Files.writeString(main, "x", StandardCharsets.UTF_8);
        Files.createFile(dir.resolve("keep.log.0"));
        Files.createFile(dir.resolve("keep.log.9"));
        Files.createFile(dir.resolve("keep.log.10"));
        FileLogHandler.shiftLogs(main, 3);
        assertFalse(Files.exists(dir.resolve("keep.log.10")), "overflow segment should be removed");
        assertFalse(Files.exists(dir.resolve("keep.log.9")), "slot 9 is out of range for maxBackups=3");
    }

    /**
     * When the directory already has more than {@code maxBackupFiles} numeric segments (for example .0 … .15),
     * overflow indices must be removed and the chain .0 … .(max-1) must stay consistent (no lexicographic listing).
     */
    @Test
    void rotationWithOverTenPreexistingSegmentsCapsAtMaxBackups(@TempDir Path dir) throws IOException {
        Path main = dir.resolve("many.log");
        Files.writeString(main, "current", StandardCharsets.UTF_8);
        for (int i = 0; i <= 15; i++) {
            Files.createFile(dir.resolve("many.log." + i));
        }
        int maxBackups = 10;
        FileLogHandler.shiftLogs(main, maxBackups);
        assertFalse(Files.isRegularFile(main), "main file should be moved into the .0 segment");
        for (int i = 10; i <= 15; i++) {
            assertFalse(Files.exists(dir.resolve("many.log." + i)), "segment ." + i + " must not exist when maxBackups=" + maxBackups);
        }
        for (int i = 0; i < maxBackups; i++) {
            assertTrue(Files.isRegularFile(dir.resolve("many.log." + i)), "expected segment many.log." + i);
        }
    }

    @Test
    void loggerRotationRemovesOverflowWhenMoreThanTenSegmentsExist(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("live.log");
        Files.writeString(log, "seed", StandardCharsets.UTF_8);
        for (int i = 0; i <= 15; i++) {
            Files.createFile(dir.resolve("live.log." + i));
        }
        FileLogHandlerConfig cfg = FileLogHandlerConfig.DEFAULT.withMaxFileBytes(1).withMaxBackupFiles(12);
        Logger logger = FileLoggers.forLogFile("live", log, cfg);
        logger.setLevel(Level.INFO);
        try {
            logger.info("trigger rotation after one byte of prior size");
        } finally {
            for (var h : logger.getHandlers()) {
                h.close();
            }
        }
        for (int i = 13; i <= 15; i++) {
            assertFalse(Files.exists(dir.resolve("live.log." + i)), "overflow ." + i + " should be gone after rotation");
        }
    }

    @Test
    void forLogFileRejectsNullLoggerName(@TempDir Path dir) {
        Path log = dir.resolve("n.log");
        assertThrows(NullPointerException.class, () -> FileLoggers.forLogFile(null, log));
    }

    @Test
    void forLogFileRejectsNullPath() {
        assertThrows(NullPointerException.class, () -> FileLoggers.forLogFile("x", null));
    }

    @Test
    void addFileHandlerRejectsNullLogger(@TempDir Path dir) {
        assertThrows(NullPointerException.class, () -> FileLoggers.addFileHandler(null, dir.resolve("x.log")));
    }

    @Test
    void fileLogHandlerConfigClampsBufferSizeMaxFileBytesAndMaxBackups() {
        FileLogHandlerConfig cfg = new FileLogHandlerConfig(1, -10L, false, -3, null);
        assertEquals(256, cfg.bufferSize());
        assertEquals(0L, cfg.maxFileBytes());
        assertEquals(0, cfg.maxBackupFiles());
        assertFalse(cfg.compressRotatedGzip());
    }

    @Test
    void addFileHandlerReplacesHandlerWhenSamePathButDifferentConfig(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("reconf.log");
        Logger logger = Logger.getLogger("reconf");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        Formatter first = new Formatter() {
            @Override
            public String format(LogRecord record) {
                return "FIRST";
            }
        };
        Formatter second = new Formatter() {
            @Override
            public String format(LogRecord record) {
                return "SECOND";
            }
        };
        try {
            FileLoggers.addFileHandler(logger, log, FileLogHandlerConfig.DEFAULT.withFormatter(first));
            FileLoggers.addFileHandler(logger, log, FileLogHandlerConfig.DEFAULT.withFormatter(second));
            int fileHandlers = 0;
            for (var h : logger.getHandlers()) {
                if (h instanceof FileLogHandler) {
                    fileHandlers++;
                }
            }
            assertEquals(1, fileHandlers);
            logger.info("x");
            for (var h : logger.getHandlers()) {
                h.flush();
            }
            String content = Files.readString(log, StandardCharsets.UTF_8).trim();
            assertEquals("SECOND", content);
        } finally {
            for (var h : logger.getHandlers()) {
                h.close();
            }
        }
    }

    @Test
    void formatterThrowingIsReportedViaErrorManagerAndDoesNotPropagate(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("fmt-fail.log");
        Logger logger = FileLoggers.forLogFile("fmt-fail", log, FileLogHandlerConfig.DEFAULT.withFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                throw new IllegalStateException("boom");
            }
        }));
        logger.setLevel(Level.INFO);
        AtomicInteger formatFailures = new AtomicInteger();
        try {
            for (var h : logger.getHandlers()) {
                if (h instanceof FileLogHandler fh) {
                    fh.setErrorManager(new ErrorManager() {
                        @Override
                        public void error(String msg, Exception ex, int code) {
                            if (code == ErrorManager.FORMAT_FAILURE) {
                                formatFailures.incrementAndGet();
                            }
                        }
                    });
                }
            }
            assertDoesNotThrow(() -> logger.info("ping"));
            assertEquals(1, formatFailures.get());
            assertTrue(!Files.exists(log) || Files.size(log) == 0,
                    "no bytes should be written when formatting fails before write");
        } finally {
            for (var h : logger.getHandlers()) {
                h.close();
            }
        }
    }

    @Test
    void publishDoesNotAppendSecondNewlineWhenFormatterEndsWithNewline(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("nl.log");
        Formatter fmt = new Formatter() {
            @Override
            public String format(LogRecord record) {
                return "OK\n";
            }
        };
        Logger logger = FileLoggers.forLogFile("nl", log, FileLogHandlerConfig.DEFAULT.withFormatter(fmt));
        logger.setLevel(Level.INFO);
        try {
            logger.info("ignored");
            for (var h : logger.getHandlers()) {
                h.flush();
            }
            byte[] raw = Files.readAllBytes(log);
            assertFalse(containsDoubleNewline(raw), "must not write an extra newline when formatter already ends with \\n");
        } finally {
            for (var h : logger.getHandlers()) {
                h.close();
            }
        }
    }

    private static boolean containsDoubleNewline(byte[] raw) {
        for (int i = 1; i < raw.length; i++) {
            if (raw[i - 1] == '\n' && raw[i] == '\n') {
                return true;
            }
        }
        return false;
    }

    @Test
    void rotateDailyShiftsStaleFileByLastModifiedDateBeforeAppend(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("daily.log");
        Files.createFile(log);
        Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS);
        Files.setLastModifiedTime(log, FileTime.from(twoDaysAgo));
        FileLogHandlerConfig cfg = FileLogHandlerConfig.DEFAULT.withRotateDaily(true).withMaxBackupFiles(4);
        Logger logger = FileLoggers.forLogFile("daily", log, cfg);
        logger.setLevel(Level.INFO);
        try {
            logger.info("fresh-day");
            for (var h : logger.getHandlers()) {
                h.flush();
            }
            assertTrue(Files.isRegularFile(log), "new current log should exist");
            assertTrue(Files.isRegularFile(dir.resolve("daily.log.0")), "stale main file should move to .0");
            String current = Files.readString(log, StandardCharsets.UTF_8);
            assertTrue(current.contains("fresh-day"));
        } finally {
            for (var h : logger.getHandlers()) {
                h.close();
            }
        }
    }

    @Test
    void shiftLogsWithMaxLogsZeroDeletesMainOnly(@TempDir Path dir) throws IOException {
        Path main = dir.resolve("drop.log");
        Files.writeString(main, "gone", StandardCharsets.UTF_8);
        FileLogHandler.shiftLogs(main, 0);
        assertFalse(Files.exists(main));
    }

    @Test
    void deleteOverflowLogsSegmentsIgnoresNonNumericSuffix(@TempDir Path dir) throws IOException {
        String base = "mix.log";
        Files.createFile(dir.resolve(base + ".backup"));
        Files.createFile(dir.resolve(base + ".12"));
        FileLogHandler.deleteOverflowLogsSegments(dir, base, 10);
        assertTrue(Files.isRegularFile(dir.resolve(base + ".backup")), "suffix .backup is not numeric — must be kept");
        assertFalse(Files.exists(dir.resolve(base + ".12")), "numeric overflow still removed");
    }

    @Test
    void pathOverloadSameFileNameDifferentDirectoriesAttachesTwoHandlersToOneLogger(@TempDir Path dir) throws IOException {
        Path d1 = dir.resolve("p1");
        Path d2 = dir.resolve("p2");
        Files.createDirectories(d1);
        Files.createDirectories(d2);
        Path log1 = d1.resolve("dupname.log");
        Path log2 = d2.resolve("dupname.log");
        Logger first = FileLoggers.forLogFile(log1);
        try {
            Logger second = FileLoggers.forLogFile(log2);
            assertSame(first, second);
            assertEquals(2, first.getHandlers().length, "Path overload uses file name as logger key — two paths => two handlers on one Logger");
            first.info("broadcast");
            for (var h : first.getHandlers()) {
                h.flush();
            }
            assertTrue(Files.readString(log1, StandardCharsets.UTF_8).contains("broadcast"));
            assertTrue(Files.readString(log2, StandardCharsets.UTF_8).contains("broadcast"));
        } finally {
            for (var h : first.getHandlers()) {
                h.close();
            }
        }
    }

    @Test
    void flushBeforeFirstPublishDoesNotThrow(@TempDir Path dir) {
        Path log = dir.resolve("lazy.log");
        FileLogHandler h = new FileLogHandler(log, FileLogHandlerConfig.DEFAULT);
        try {
            assertDoesNotThrow(h::flush);
        } finally {
            h.close();
        }
    }

    @Test
    void sizeRotationWithZeroBackupsDropsContentOnRotate(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("nobak.log");
        FileLogHandlerConfig cfg = FileLogHandlerConfig.DEFAULT.withMaxFileBytes(20).withMaxBackupFiles(0);
        Logger logger = FileLoggers.forLogFile("nobak", log, cfg);
        logger.setLevel(Level.INFO);
        try {
            logger.info("aaaaaaaaaa");
            logger.info("bbbbbbbbbb");
            for (var h : logger.getHandlers()) {
                h.flush();
            }
            assertTrue(Files.isRegularFile(log));
            String content = Files.readString(log, StandardCharsets.UTF_8);
            assertTrue(content.contains("bbbbbbbbbb"));
        } finally {
            for (var h : logger.getHandlers()) {
                h.close();
            }
        }
    }

    @Test
    void rotatesToGzipWhenCompressionEnabled(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("gzip.log");
        FileLogHandlerConfig cfg = FileLogHandlerConfig.DEFAULT
                .withMaxFileBytes(1)
                .withMaxBackupFiles(3)
                .withCompressRotatedGzip(true);
        Logger logger = FileLoggers.forLogFile("gzip", log, cfg);
        logger.setLevel(Level.INFO);
        try {
            logger.info("first-line");
            logger.info("second-line");
            for (var h : logger.getHandlers()) {
                h.flush();
            }
            Path gzBackup = dir.resolve("gzip.log.0.gz");
            assertTrue(Files.isRegularFile(gzBackup), "compressed rotated segment should exist");
            Path decompressed = dir.resolve("gzip.log.0");
            CompressFile.decompressGzipFile(gzBackup.toFile(), decompressed.toFile());
            String backupText = Files.readString(decompressed, StandardCharsets.UTF_8);
            assertTrue(backupText.contains("first-line"), "backup should contain rotated records");
        } finally {
            for (var h : logger.getHandlers()) {
                h.close();
            }
        }
    }
}
