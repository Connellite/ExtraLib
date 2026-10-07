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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;

/**
 * {@link Handler} that appends one line per {@link LogRecord} using the configured {@link Formatter}
 * (default {@link SimpleFormatter} when {@link FileLogHandlerConfig#formatter()} is {@code null}).
 * Optional rotation renames the current file to {@code name.log.0}, shifts older segments, and starts a new file.
 * <p>
 * {@link #publish} holds the read side of {@link #writerLock} while a record is written. Rotation and {@link #close()}
 * hold the write side: while {@link Files#move} runs, other threads block (async rotation is out of scope).
 * Byte writes themselves are serialized on {@link #streamLock}, because the stream is not safe for concurrent use.
 * </p>
 */
@SuppressWarnings("JavadocLinkAsPlainText")
public class FileLogHandler extends Handler {

    @Getter
    private final Path logFile;
    @Getter
    private final FileLogHandlerConfig config;
    private final ZoneId zone = ZoneId.systemDefault();
    /**
     * Lock for the open writer. Read side covers a publish or flush; write side covers open, rotation, and close.
     * The write lock is reentrant so {@link #closeWriter()} and {@link #openWriter()} can lock again while the caller
     * already holds it.
     */
    private final ReadWriteLock writerLock = new ReentrantReadWriteLock();
    /** Serializes byte writes on {@link #writer}. Several publishes may hold the read lock at once. */
    private final Object streamLock = new Object();

    private volatile BufferedOutputStream writer;
    private volatile long bytesWritten;
    /** {@code yyyy-MM-dd} while daily rotation is on, {@code ""} when it is off, {@code null} when no file is open. */
    private volatile String date;
    private volatile boolean closed;

    public FileLogHandler(Path logFile, FileLogHandlerConfig config) {
        Objects.requireNonNull(logFile, "logFile cannot be null");
        Objects.requireNonNull(config, "config cannot be null");
        this.logFile = logFile.toAbsolutePath().normalize();
        this.config = config;
        configure();
    }

    public FileLogHandler(Path logFile) {
        this(logFile, FileLogHandlerConfig.DEFAULT);
    }

    /**
     * Applies the formatter and UTF-8 encoding from {@link #config}.
     * <p>
     * Tomcat reference:
     * https://github.com/apache/tomcat/blob/05b5d70555068a6647226e9924b2d7f1b9978b03/java/org/apache/juli/FileHandler.java
     */
    private void configure() {
        Formatter formatter = config.formatter();
        setFormatter(formatter != null ? formatter : new SimpleFormatter());
        try {
            setEncoding(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 must be supported by the JVM (required charset)", e);
        }
    }

    /**
     * Formats the record, rotates when the calendar day or the size limit says so, then appends one line.
     * Formatting runs before rotation, so a formatter failure does not rotate the file.
     * <p>
     * Tomcat reference:
     * https://github.com/apache/tomcat/blob/05b5d70555068a6647226e9924b2d7f1b9978b03/java/org/apache/juli/FileHandler.java
     */
    @Override
    public void publish(LogRecord record) {
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
        String tsDate = config.rotateDaily() ? LocalDate.now(zone).toString() : "";
        writerLock.readLock().lock();
        try {
            if (closed) {
                return;
            }
            if (needsSwitch(tsDate)) {
                writerLock.readLock().unlock();
                writerLock.writeLock().lock();
                try {
                    if (!closed && needsSwitch(tsDate)) {
                        applySwitch();
                    }
                } finally {
                    writerLock.readLock().lock();
                    writerLock.writeLock().unlock();
                }
            }
            if (closed) {
                return;
            }
            writePayload(bytes, needsNl);
        } catch (IOException ex) {
            reportError(null, ex, ErrorManager.WRITE_FAILURE);
        } finally {
            writerLock.readLock().unlock();
        }
    }

    private boolean needsSwitch(String tsDate) {
        if (writer == null) {
            return true;
        }
        if (config.rotateDaily() && !tsDate.equals(date)) {
            return true;
        }
        return sizeReached();
    }

    /**
     * First open only opens the file (a stale daily file is shifted inside {@link #openWriter()}).
     * A later day change or a full file closes, shifts, and opens again. An already-large file rotates once more
     * after that first open.
     */
    private void applySwitch() throws IOException {
        if (writer == null) {
            openWriter();
            if (sizeReached()) {
                rotate();
            }
        } else {
            rotate();
        }
    }

    private String currentDateToken() {
        return config.rotateDaily() ? LocalDate.now(zone).toString() : "";
    }

    private boolean sizeReached() {
        if (config.maxFileBytes() <= 0) {
            return false;
        }
        synchronized (streamLock) {
            return bytesWritten >= config.maxFileBytes();
        }
    }

    private void rotate() throws IOException {
        closeWriter();
        shiftLogs(logFile, config.maxBackupFiles(), config.compressRotatedGzip());
        openWriter();
    }

    private void writePayload(byte[] bytes, boolean needsNl) throws IOException {
        synchronized (streamLock) {
            BufferedOutputStream current = writer;
            if (current == null) {
                reportError(null, null, ErrorManager.WRITE_FAILURE);
                return;
            }
            current.write(bytes);
            if (needsNl) {
                current.write('\n');
            }
            bytesWritten += bytes.length + (needsNl ? 1 : 0);
        }
    }

    /**
     * Opens the current log file for append. When daily rotation is on and the existing file's last-modified day is
     * not today, that file is shifted before the new stream is created.
     * <p>
     * Tomcat reference:
     * https://github.com/apache/tomcat/blob/05b5d70555068a6647226e9924b2d7f1b9978b03/java/org/apache/juli/FileHandler.java
     */
    private void openWriter() throws IOException {
        writerLock.writeLock().lock();
        try {
            if (writer != null || closed) {
                return;
            }
            BufferedOutputStream opened = null;
            try {
                Path parent = logFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                if (config.rotateDaily() && Files.isRegularFile(logFile)) {
                    LocalDate fileDay = LocalDate.ofInstant(Files.getLastModifiedTime(logFile).toInstant(), zone);
                    if (!fileDay.equals(LocalDate.now(zone))) {
                        shiftLogs(logFile, config.maxBackupFiles(), config.compressRotatedGzip());
                    }
                }
                boolean existed = Files.isRegularFile(logFile);
                opened = new BufferedOutputStream(new FileOutputStream(logFile.toFile(), true), config.bufferSize());
                byte[] head = utf8(getFormatter().getHead(this));
                long size = existed ? Files.size(logFile) : 0L;
                if (head.length > 0) {
                    opened.write(head);
                    size += head.length;
                }
                bytesWritten = size;
                date = currentDateToken();
                writer = opened;
                opened = null;
            } finally {
                if (opened != null) {
                    try {
                        opened.close();
                    } catch (IOException closeEx) {
                        reportError(null, closeEx, ErrorManager.CLOSE_FAILURE);
                    }
                }
            }
        } finally {
            writerLock.writeLock().unlock();
        }
    }

    /**
     * Flushes and closes the open stream without marking this handler closed.
     * Rotation uses the same path as {@link #close()}.
     * <p>
     * Tomcat reference:
     * https://github.com/apache/tomcat/blob/05b5d70555068a6647226e9924b2d7f1b9978b03/java/org/apache/juli/FileHandler.java
     */
    private void closeWriter() {
        writerLock.writeLock().lock();
        try {
            BufferedOutputStream current;
            synchronized (streamLock) {
                current = writer;
                writer = null;
                date = null;
            }
            if (current == null) {
                return;
            }
            try {
                byte[] tail = utf8(getFormatter().getTail(this));
                if (tail.length > 0) {
                    current.write(tail);
                }
            } catch (Exception ex) {
                reportError(null, ex, ErrorManager.CLOSE_FAILURE);
            }
            try {
                current.flush();
            } catch (IOException ex) {
                reportError(null, ex, ErrorManager.FLUSH_FAILURE);
            }
            try {
                current.close();
            } catch (IOException ex) {
                reportError(null, ex, ErrorManager.CLOSE_FAILURE);
            }
        } finally {
            writerLock.writeLock().unlock();
        }
    }

    private static byte[] utf8(String text) {
        if (text == null || text.isEmpty()) {
            return new byte[0];
        }
        return text.getBytes(StandardCharsets.UTF_8);
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
                CompressFile.compressGzipFile(moved, gz);
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

    /**
     * Flushes the open writer. Does nothing when the file has not been opened yet.
     * <p>
     * Tomcat reference:
     * https://github.com/apache/tomcat/blob/05b5d70555068a6647226e9924b2d7f1b9978b03/java/org/apache/juli/FileHandler.java
     */
    @Override
    public void flush() {
        writerLock.readLock().lock();
        try {
            BufferedOutputStream current = writer;
            if (current == null) {
                return;
            }
            synchronized (streamLock) {
                try {
                    current.flush();
                } catch (IOException ex) {
                    reportError(null, ex, ErrorManager.FLUSH_FAILURE);
                }
            }
        } finally {
            writerLock.readLock().unlock();
        }
    }

    /**
     * Clears {@link #closed} and opens the file again. {@link AsyncFileLogHandler#open()} uses this so a handler closed
     * on purpose can accept records afterwards. {@link #publish} on this class stays closed until then.
     */
    void reopen() {
        writerLock.writeLock().lock();
        try {
            closed = false;
            openWriter();
        } catch (IOException ex) {
            reportError(null, ex, ErrorManager.OPEN_FAILURE);
        } finally {
            writerLock.writeLock().unlock();
        }
    }

    /**
     * Closes the open log file.
     * <p>
     * Tomcat reference:
     * https://github.com/apache/tomcat/blob/05b5d70555068a6647226e9924b2d7f1b9978b03/java/org/apache/juli/FileHandler.java
     */
    @Override
    public void close() {
        writerLock.writeLock().lock();
        try {
            if (closed) {
                return;
            }
            closed = true;
            closeWriter();
        } finally {
            writerLock.writeLock().unlock();
        }
    }
}
