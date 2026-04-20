package io.github.connellite.logger;

import io.github.connellite.compress.CompressFile;
import lombok.Getter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.lang.ref.WeakReference;
import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Formatter;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;
import java.time.ZoneId;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * {@link Handler} that appends one line per {@link LogRecord} using the configured {@link Formatter}
 * (default {@link SimpleFormatter} when {@link FileLogHandlerConfig#formatter()} is {@code null}).
 * Optional rotation renames the current file to {@code name.log.0}, shifts older segments, and starts a new file.
 * <p>
 * {@link #publish} and rotation are synchronized on this handler: while {@link java.nio.file.Files#move} runs during
 * rotation, other threads block on this instance (typical for JUL; async rotation is out of scope here).
 * </p>
 */
public class FileLogHandler extends Handler {

    private static final ConcurrentHashMap<Path, WeakReference<FileLogHandler>> ACTIVE_WRITERS = new ConcurrentHashMap<>();

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
    /** Set after {@link #openAppendStreams} completes so a failed first open can {@linkplain #releaseOutputFile() release} the path. */
    private boolean logStreamEverOpened;

    public FileLogHandler(Path logFile, FileLogHandlerConfig config) {
        Objects.requireNonNull(logFile, "logFile cannot be null");
        Objects.requireNonNull(config, "config cannot be null");
        this.logFile = logFile.toAbsolutePath().normalize();
        this.config = config;
        Formatter f = config.formatter();
        setFormatter(f != null ? f : new SimpleFormatter());
        try {
            setEncoding(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 must be supported by the JVM (required charset)", e);
        }
        claimOutputFile();
    }

    public FileLogHandler(Path logFile) {
        this(logFile, FileLogHandlerConfig.DEFAULT);
    }

    private void claimOutputFile() {
        ACTIVE_WRITERS.compute(logFile, (path, oldRef) -> {
            FileLogHandler prev = oldRef == null ? null : oldRef.get();
            if (prev != null && prev != this) {
                throw new IllegalStateException("Another FileLogHandler is already writing to " + path);
            }
            return new WeakReference<>(this);
        });
    }

    private void releaseOutputFile() {
        ACTIVE_WRITERS.compute(logFile, (path, ref) -> {
            if (ref == null) {
                return null;
            }
            FileLogHandler h = ref.get();
            if (h == null || h == this) {
                return null;
            }
            return ref;
        });
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
        } catch (IllegalStateException ex) {
            reportError(null, ex, ErrorManager.GENERIC_FAILURE);
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

    /**
     * Lazily opens the file when needed. When {@code buffered != null}, {@link #dayOfCurrentFile} is already current
     * because calendar rollover is handled above in {@link #publish} via {@link #rotateLocked()} before this returns early.
     * Registration uses {@link #claimOutputFile()} alone (no prior {@code get}) so two threads cannot both see an empty slot then race.
     */
    private void ensureOpen() throws IOException {
        if (buffered != null) {
            return;
        }
        claimOutputFile();
        try {
            ensureParentDirs();
            if (config.rotateDaily() && Files.isRegularFile(logFile)) {
                LocalDate fileDay = LocalDate.ofInstant(Files.getLastModifiedTime(logFile).toInstant(), zone);
                if (!fileDay.equals(LocalDate.now(zone))) {
                    shiftLogs(logFile, config.maxBackupFiles(), config.compressRotatedGzip());
                }
            }
            boolean existed = Files.isRegularFile(logFile);
            openAppendStreams(existed);
        } catch (IOException e) {
            closeStreamsNoMarkClosed();
            if (!logStreamEverOpened) {
                releaseOutputFile();
            }
            throw e;
        }
    }

    private void rotateLocked() throws IOException {
        closeStreamsNoMarkClosed();
        shiftLogs(logFile, config.maxBackupFiles(), config.compressRotatedGzip());
        bytesWritten = 0;
        dayOfCurrentFile = LocalDate.now(zone);
        ensureParentDirs();
        openAppendStreams(Files.isRegularFile(logFile));
    }

    private void openAppendStreams(boolean existed) throws IOException {
        fileOut = new FileOutputStream(logFile.toFile(), true);
        buffered = new BufferedOutputStream(fileOut, config.bufferSize());
        bytesWritten = existed ? Files.size(logFile) : 0;
        dayOfCurrentFile = LocalDate.now(zone);
        logStreamEverOpened = true;
    }

    private void closeStreamsNoMarkClosed() {
        BufferedOutputStream b = buffered;
        buffered = null;
        fileOut = null;
        if (b != null) {
            try {
                b.flush();
            } catch (IOException ex) {
                reportError(null, ex, ErrorManager.FLUSH_FAILURE);
            }
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
        shiftLogs(main, maxLogs, false);
    }

    static void shiftLogs(Path main, int maxLogs, boolean compressRotatedGzip) throws IOException {
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
        deleteOverflowLogsSegments(dir, name, maxLogs, compressRotatedGzip);
        String suffix = compressRotatedGzip ? ".gz" : "";
        Path oldest = dir.resolve(name + "." + (maxLogs - 1) + suffix);
        Files.deleteIfExists(oldest);
        for (int i = maxLogs - 2; i >= 0; i--) {
            Path from = dir.resolve(name + "." + i + suffix);
            Path to = dir.resolve(name + "." + (i + 1) + suffix);
            if (Files.exists(from)) {
                Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        if (Files.exists(main)) {
            Path moved = dir.resolve(name + ".0");
            Files.move(main, moved, StandardCopyOption.REPLACE_EXISTING);
            if (compressRotatedGzip) {
                Path gz = dir.resolve(name + ".0.gz");
                CompressFile.compressGzipFile(moved.toFile(), gz.toFile());
                Files.deleteIfExists(moved);
            }
        }
    }

    /**
     * Drops {@code base.N} when {@code N} is a non-negative integer and {@code N >= maxBackups}, so extra files from an
     * old {@code maxBackups} or a buggy rotation cannot collide with the numeric chain.
     */
    static void deleteOverflowLogsSegments(Path dir, String baseFileName, int maxBackups) throws IOException {
        deleteOverflowLogsSegments(dir, baseFileName, maxBackups, false);
    }

    static void deleteOverflowLogsSegments(Path dir, String baseFileName, int maxBackups, boolean compressRotatedGzip) throws IOException {
        String prefix = baseFileName + ".";
        String expectedSuffix = compressRotatedGzip ? ".gz" : "";
        try (Stream<Path> stream = Files.list(dir)) {
            Path[] entries = stream.toArray(Path[]::new);
            for (Path p : entries) {
                String fname = p.getFileName().toString();
                if (!fname.startsWith(prefix)) {
                    continue;
                }
                String tail = fname.substring(prefix.length());
                if (compressRotatedGzip) {
                    if (!tail.endsWith(expectedSuffix)) {
                        continue;
                    }
                    tail = tail.substring(0, tail.length() - expectedSuffix.length());
                } else if (tail.endsWith(".gz")) {
                    continue;
                }
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
        releaseOutputFile();
    }
}
