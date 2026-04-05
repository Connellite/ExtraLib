package io.github.connellite.logger;

import lombok.Getter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Objects;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;
import java.time.ZoneId;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * {@link Handler} that appends lines formatted like {@link java.util.logging.SimpleFormatter} (same locale and fields),
 * Optional rotation renames the current file to {@code name.log.0}, shifts older segments, and starts a new file.
 */
public class FileLogHandler extends Handler {

    @Getter
    private final Path logFile;
    @Getter
    private final FileLogHandlerConfig config;
    private final ZoneId zone = ZoneId.systemDefault();

    private BufferedOutputStream buffered;
    private FileOutputStream fileOut;
    private long bytesWritten;
    private LocalDate dayOfCurrentFile;
    private volatile boolean closed;

    public FileLogHandler(Path logFile, FileLogHandlerConfig config) {
        Objects.requireNonNull(logFile, "logFile cannot be null");
        Objects.requireNonNull(config, "config cannot be null");
        this.logFile = logFile.toAbsolutePath().normalize();
        this.config = config;
        setFormatter(new SimpleFormatter());
        try {
            setEncoding(StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
        }
    }

    public FileLogHandler(Path logFile) {
        this(logFile, FileLogHandlerConfig.DEFAULT);
    }

    @Override
    public synchronized void publish(LogRecord record) {
        if (closed || !isLoggable(record)) {
            return;
        }
        String line;
        try {
            line = getFormatter().format(record);
        } catch (Exception ex) {
            reportError(null, ex, ErrorManager.FORMAT_FAILURE);
            return;
        }
        byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
        boolean needsNl = bytes.length == 0 || bytes[bytes.length - 1] != '\n';
        int payloadLen = bytes.length + (needsNl ? 1 : 0);
        try {
            if (buffered != null && config.rotateDaily() && !LocalDate.now(zone).equals(dayOfCurrentFile)) {
                rotateLocked();
            }
            ensureOpen();
            if (config.maxFileBytes() > 0 && bytesWritten >= config.maxFileBytes()) {
                rotateLocked();
            }
            buffered.write(bytes);
            if (needsNl) {
                buffered.write('\n');
            }
            bytesWritten += payloadLen;
        } catch (IOException ex) {
            reportError(null, ex, ErrorManager.WRITE_FAILURE);
        }
    }

    private void ensureParentDirs() throws IOException {
        Path parent = logFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private void ensureOpen() throws IOException {
        if (buffered != null) {
            return;
        }
        ensureParentDirs();
        if (config.rotateDaily() && Files.isRegularFile(logFile)) {
            LocalDate fileDay = LocalDate.ofInstant(Files.getLastModifiedTime(logFile).toInstant(), zone);
            if (!fileDay.equals(LocalDate.now(zone))) {
                shiftLogs(logFile, config.maxBackupFiles());
            }
        }
        boolean existed = Files.isRegularFile(logFile);
        fileOut = new FileOutputStream(logFile.toFile(), true);
        buffered = new BufferedOutputStream(fileOut, config.bufferSize());
        bytesWritten = existed ? Files.size(logFile) : 0;
        dayOfCurrentFile = LocalDate.now(zone);
    }

    private void rotateLocked() throws IOException {
        closeStreamsNoMarkClosed();
        shiftLogs(logFile, config.maxBackupFiles());
        bytesWritten = 0;
        dayOfCurrentFile = LocalDate.now(zone);
        ensureParentDirs();
        fileOut = new FileOutputStream(logFile.toFile(), true);
        buffered = new BufferedOutputStream(fileOut, config.bufferSize());
    }

    private void closeStreamsNoMarkClosed() {
        BufferedOutputStream b = buffered;
        buffered = null;
        fileOut = null;
        if (b != null) {
            try {
                b.close();
            } catch (IOException ex) {
                reportError(null, ex, ErrorManager.CLOSE_FAILURE);
            }
        }
    }

    /**
     * Shifts {@code main} to {@code name.0}, {@code name.i} to {@code name.(i+1)}. Indices are processed from high to low
     * so order does not depend on directory listing. Segments {@code name.N} with {@code N >= maxLogs} (for example
     * leftovers from a previously larger limit) are removed first.
     */
    static void shiftLogs(Path main, int maxLogs) throws IOException {
        String name = main.getFileName().toString();
        Path dir = main.getParent();
        if (dir == null) {
            dir = Path.of(".");
        }
        Files.createDirectories(dir);
        if (maxLogs <= 0) {
            Files.deleteIfExists(main);
            return;
        }
        deleteOverflowLogsSegments(dir, name, maxLogs);
        Path oldest = dir.resolve(name + "." + (maxLogs - 1));
        Files.deleteIfExists(oldest);
        for (int i = maxLogs - 2; i >= 0; i--) {
            Path from = dir.resolve(name + "." + i);
            Path to = dir.resolve(name + "." + (i + 1));
            if (Files.exists(from)) {
                Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        if (Files.exists(main)) {
            Files.move(main, dir.resolve(name + ".0"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Drops {@code base.N} when {@code N} is a non-negative integer and {@code N >= maxBackups}, so extra files from an
     * old {@code maxBackups} or a buggy rotation cannot collide with the numeric chain.
     */
    static void deleteOverflowLogsSegments(Path dir, String baseFileName, int maxBackups) throws IOException {
        String prefix = baseFileName + ".";
        try (Stream<Path> stream = Files.list(dir)) {
            Path[] entries = stream.toArray(Path[]::new);
            for (Path p : entries) {
                String fname = p.getFileName().toString();
                if (!fname.startsWith(prefix)) {
                    continue;
                }
                String tail = fname.substring(prefix.length());
                if (!tail.chars().allMatch(Character::isDigit) || tail.isEmpty()) {
                    continue;
                }
                int idx = Integer.parseInt(tail);
                if (idx >= maxBackups) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    @Override
    public synchronized void flush() {
        if (buffered != null) {
            try {
                buffered.flush();
            } catch (IOException ex) {
                reportError(null, ex, ErrorManager.FLUSH_FAILURE);
            }
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        closeStreamsNoMarkClosed();
    }
}
