package io.github.connellite.logger;

/**
 * Settings for {@link FileLogHandler}: buffer size and optional rotation (by size and/or calendar day).
 */
public record FileLogHandlerConfig(
        int bufferSize,
        long maxFileBytes,
        boolean rotateDaily,
        int maxBackupFiles
) {
    /**
     * @param bufferSize     {@link java.io.BufferedOutputStream} capacity; minimum 256
     * @param maxFileBytes   when {@code > 0}, rotate once the current log file reaches this size
     * @param rotateDaily    when {@code true}, start a new file when the system date changes
     * @param maxBackupFiles number of rotated segments kept ({@code name.log.0} …); {@code 0} means no backups (main file is dropped on rotate)
     */
    public FileLogHandlerConfig {
        if (bufferSize < 256) {
            bufferSize = 256;
        }
        if (maxFileBytes < 0) {
            maxFileBytes = 0;
        }
        if (maxBackupFiles < 0) {
            maxBackupFiles = 0;
        }
    }

    /** Buffered writes, no rotation. */
    public static final FileLogHandlerConfig DEFAULT = new FileLogHandlerConfig(8192, 0, false, 5);

    public FileLogHandlerConfig withBufferSize(int size) {
        return new FileLogHandlerConfig(size, maxFileBytes, rotateDaily, maxBackupFiles);
    }

    public FileLogHandlerConfig withMaxFileBytes(long bytes) {
        return new FileLogHandlerConfig(bufferSize, bytes, rotateDaily, maxBackupFiles);
    }

    public FileLogHandlerConfig withRotateDaily(boolean daily) {
        return new FileLogHandlerConfig(bufferSize, maxFileBytes, daily, maxBackupFiles);
    }

    public FileLogHandlerConfig withMaxBackupFiles(int max) {
        return new FileLogHandlerConfig(bufferSize, maxFileBytes, rotateDaily, max);
    }
}
