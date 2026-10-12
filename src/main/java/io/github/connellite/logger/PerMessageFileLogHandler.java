package io.github.connellite.logger;

import lombok.Getter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * {@link Handler} that opens the log file, appends one formatted line, and closes the file for every {@link LogRecord}.
 * Optional rotation uses the same {@link FileLogHandlerConfig} rules as {@link FileLogHandler}: size, calendar day,
 * backup count, and gzip.
 * <p>
 * {@link #publish} holds {@link #lock} across rotation, the write, and the close of that stream. {@link #flush()} does
 * nothing, because the stream is already closed. {@link #close()} appends the formatter tail when this instance wrote
 * the current segment, then ignores later records.
 * </p>
 *
 * @deprecated Not recommended for production. Each record opens and closes the file and holds a lock across those
 *             calls. Prefer {@link FileLogHandler}.
 */
@Deprecated
public class PerMessageFileLogHandler extends Handler {

    @Getter
    private final Path logFile;
    @Getter
    private final FileLogHandlerConfig config;
    private final ZoneId zone = ZoneId.systemDefault();
    /** Serializes rotation, the per-record write, and {@link #close()}. */
    private final ReentrantLock lock = new ReentrantLock();

    /** Set after this instance has appended a record to the current segment. Cleared when that segment is finished. */
    private boolean wroteCurrentSegment;
    private volatile boolean closed;

    public PerMessageFileLogHandler(Path logFile, FileLogHandlerConfig config) {
        Objects.requireNonNull(logFile, "logFile cannot be null");
        Objects.requireNonNull(config, "config cannot be null");
        this.logFile = logFile.toAbsolutePath().normalize();
        this.config = config;
        configure();
    }

    public PerMessageFileLogHandler(Path logFile) {
        this(logFile, FileLogHandlerConfig.DEFAULT);
    }

    /**
     * Applies the formatter and UTF-8 encoding from {@link #config}.
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
     * Formats the record, rotates when the calendar day or the size limit says so, then appends one line and closes
     * the file. Formatting runs before the lock, so a formatter failure does not rotate the file.
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
        lock.lock();
        try {
            if (closed) {
                return;
            }
            rotateIfNeeded();
            writeRecord(bytes, needsNl);
        } catch (IOException ex) {
            reportError(null, ex, ErrorManager.WRITE_FAILURE);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Shifts the current file when daily rotation sees another calendar day, or the file is already at
     * {@link FileLogHandlerConfig#maxFileBytes()}. A segment this instance wrote receives the formatter tail first.
     */
    private void rotateIfNeeded() throws IOException {
        if (!Files.isRegularFile(logFile)) {
            return;
        }
        if (!dailyStale() && !sizeReached()) {
            return;
        }
        if (wroteCurrentSegment) {
            appendTail();
            wroteCurrentSegment = false;
        }
        FileLogHandler.shiftLogs(logFile, config.maxBackupFiles(), config.compressRotatedGzip());
    }

    private boolean dailyStale() throws IOException {
        if (!config.rotateDaily()) {
            return false;
        }
        LocalDate fileDay = LocalDate.ofInstant(Files.getLastModifiedTime(logFile).toInstant(), zone);
        return !fileDay.equals(LocalDate.now(zone));
    }

    private boolean sizeReached() throws IOException {
        if (config.maxFileBytes() <= 0) {
            return false;
        }
        return Files.size(logFile) >= config.maxFileBytes();
    }

    private void writeRecord(byte[] bytes, boolean needsNl) throws IOException {
        long size = Files.isRegularFile(logFile) ? Files.size(logFile) : 0L;
        try (BufferedOutputStream out = openAppend()) {
            if (size == 0L) {
                byte[] head = FileLogHandler.utf8(getFormatter().getHead(this));
                if (head.length > 0) {
                    out.write(head);
                }
            }
            out.write(bytes);
            if (needsNl) {
                out.write('\n');
            }
        }
        wroteCurrentSegment = true;
    }

    /**
     * Appends {@link Formatter#getTail(Handler)} when it is non-empty. Failures are reported and do not escape, so
     * rotation can still move the segment.
     */
    private void appendTail() {
        try {
            byte[] tail = FileLogHandler.utf8(getFormatter().getTail(this));
            if (tail.length == 0 || !Files.isRegularFile(logFile)) {
                return;
            }
            try (BufferedOutputStream out = openAppend()) {
                out.write(tail);
            }
        } catch (Exception ex) {
            reportError(null, ex, ErrorManager.CLOSE_FAILURE);
        }
    }

    private BufferedOutputStream openAppend() throws IOException {
        Path parent = logFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return new BufferedOutputStream(new FileOutputStream(logFile.toFile(), true), config.bufferSize());
    }

    /**
     * Does nothing. The stream is closed inside {@link #publish}.
     */
    @Override
    public void flush() {
    }

    /**
     * Appends the formatter tail when this instance wrote the current segment, then ignores later records.
     */
    @Override
    public void close() {
        lock.lock();
        try {
            if (closed) {
                return;
            }
            closed = true;
            if (wroteCurrentSegment) {
                appendTail();
                wroteCurrentSegment = false;
            }
        } finally {
            lock.unlock();
        }
    }
}
