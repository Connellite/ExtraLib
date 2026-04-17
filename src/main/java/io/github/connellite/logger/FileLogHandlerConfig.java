package io.github.connellite.logger;

import java.util.logging.Formatter;

/**
 * Settings for {@link FileLogHandler}: buffer size, optional rotation (by size and/or calendar day), and log line format.
 */
public record FileLogHandlerConfig(
        int bufferSize,
        long maxFileBytes,
        boolean rotateDaily,
        int maxBackupFiles,
        /**
         * When non-null, used as the {@link java.util.logging.Handler}'s formatter; when {@code null}, {@link java.util.logging.SimpleFormatter} is used.
         */
        Formatter formatter
) {
    /**
     * @param bufferSize     {@link java.io.BufferedOutputStream} capacity; minimum 256
     * @param maxFileBytes   when {@code > 0}, rotate once the current log file reaches this size
     * @param rotateDaily    when {@code true}, start a new file when the system date changes
     * @param maxBackupFiles number of rotated segments kept ({@code name.log.0} …); {@code 0} means no backups (main file is dropped on rotate)
     * @param formatter      custom {@link Formatter} for each log line, or {@code null} for {@link java.util.logging.SimpleFormatter}
     */
    public FileLogHandlerConfig {
        bufferSize = Math.max(bufferSize, 256);
        maxFileBytes = Math.max(maxFileBytes, 0);
        maxBackupFiles = Math.max(maxBackupFiles, 0);
    }

    /** Buffered writes, no rotation, {@link java.util.logging.SimpleFormatter}. */
    public static final FileLogHandlerConfig DEFAULT = new FileLogHandlerConfig(8192, 0, false, 5, null);

    public FileLogHandlerConfig withBufferSize(int size) {
        return new FileLogHandlerConfig(size, maxFileBytes, rotateDaily, maxBackupFiles, formatter);
    }

    public FileLogHandlerConfig withMaxFileBytes(long bytes) {
        return new FileLogHandlerConfig(bufferSize, bytes, rotateDaily, maxBackupFiles, formatter);
    }

    public FileLogHandlerConfig withRotateDaily(boolean daily) {
        return new FileLogHandlerConfig(bufferSize, maxFileBytes, daily, maxBackupFiles, formatter);
    }

    public FileLogHandlerConfig withMaxBackupFiles(int max) {
        return new FileLogHandlerConfig(bufferSize, maxFileBytes, rotateDaily, max, formatter);
    }

    public FileLogHandlerConfig withFormatter(Formatter customFormatter) {
        return new FileLogHandlerConfig(bufferSize, maxFileBytes, rotateDaily, maxBackupFiles, customFormatter);
    }
}
