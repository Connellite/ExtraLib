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
        boolean compressRotatedGzip,
        Formatter formatter
) {
    /**
     * @param bufferSize     {@link java.io.BufferedOutputStream} capacity; minimum 256
     * @param maxFileBytes   when {@code > 0}, rotate once the current log file reaches this size
     * @param rotateDaily    when {@code true}, start a new file when the system date changes
     * @param maxBackupFiles number of rotated segments kept ({@code name.log.0} …); {@code 0} means no backups (main file is dropped on rotate)
     * @param compressRotatedGzip when {@code true}, rotated segments are compressed to {@code .gz}
     * @param formatter      custom {@link Formatter} for each log line, or {@code null} for {@link java.util.logging.SimpleFormatter}
     */
    public FileLogHandlerConfig {
        bufferSize = Math.max(bufferSize, 256);
        maxFileBytes = Math.max(maxFileBytes, 0);
        maxBackupFiles = Math.max(maxBackupFiles, 0);
    }

    public FileLogHandlerConfig(int bufferSize, long maxFileBytes, boolean rotateDaily, int maxBackupFiles, Formatter formatter) {
        this(bufferSize, maxFileBytes, rotateDaily, maxBackupFiles, false, formatter);
    }

    /** Buffered writes, no rotation, {@link java.util.logging.SimpleFormatter}. */
    public static final FileLogHandlerConfig DEFAULT =
            new FileLogHandlerConfig(8192, 0, false, 10, false, null);

    public FileLogHandlerConfig withBufferSize(int size) {
        return new FileLogHandlerConfig(size, maxFileBytes, rotateDaily, maxBackupFiles, compressRotatedGzip, formatter);
    }

    public FileLogHandlerConfig withMaxFileBytes(long bytes) {
        return new FileLogHandlerConfig(bufferSize, bytes, rotateDaily, maxBackupFiles, compressRotatedGzip, formatter);
    }

    public FileLogHandlerConfig withRotateDaily(boolean daily) {
        return new FileLogHandlerConfig(bufferSize, maxFileBytes, daily, maxBackupFiles, compressRotatedGzip, formatter);
    }

    public FileLogHandlerConfig withMaxBackupFiles(int max) {
        return new FileLogHandlerConfig(bufferSize, maxFileBytes, rotateDaily, max, compressRotatedGzip, formatter);
    }

    public FileLogHandlerConfig withCompressRotatedGzip(boolean compress) {
        return new FileLogHandlerConfig(bufferSize, maxFileBytes, rotateDaily, maxBackupFiles, compress, formatter);
    }

    public FileLogHandlerConfig withFormatter(Formatter customFormatter) {
        return new FileLogHandlerConfig(bufferSize, maxFileBytes, rotateDaily, maxBackupFiles, compressRotatedGzip, customFormatter);
    }
}
