package io.github.connellite.logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
