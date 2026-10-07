package io.github.connellite.logger;

import java.util.logging.Formatter;

/**
 * Settings for {@link AsyncFileLogHandler}: everything {@link FileLogHandlerConfig} carries, plus the queue limit and
 * what to do when that queue is full.
 */
public record AsyncFileLogHandlerConfig(
        int bufferSize,
        long maxFileBytes,
        boolean rotateDaily,
        int maxBackupFiles,
        boolean compressRotatedGzip,
        Formatter formatter,
        int overflowDropType,
        int maxRecords
) {
    /** Overflow policy: drop the last record in the queue. */
    public static final int OVERFLOW_DROP_LAST = 1;
    /** Overflow policy: drop the first (oldest) record in the queue. */
    public static final int OVERFLOW_DROP_FIRST = 2;
    /** Overflow policy: wait until the queue accepts the record. */
    public static final int OVERFLOW_DROP_FLUSH = 3;
    /** Overflow policy: drop the current record. */
    public static final int OVERFLOW_DROP_CURRENT = 4;

    /**
     * @param bufferSize          {@link java.io.BufferedOutputStream} capacity; minimum 256
     * @param maxFileBytes        when {@code > 0}, rotate once the current log file reaches this size
     * @param rotateDaily         when {@code true}, start a new file when the system date changes
     * @param maxBackupFiles      number of rotated segments kept; {@code 0} drops the main file on rotate
     * @param compressRotatedGzip when {@code true}, rotated segments are compressed to {@code .gz}
     * @param formatter           custom {@link Formatter}, or {@code null} for {@link java.util.logging.SimpleFormatter}
     * @param overflowDropType    {@link #OVERFLOW_DROP_LAST}, {@link #OVERFLOW_DROP_FIRST}, {@link #OVERFLOW_DROP_FLUSH},
     *                            or {@link #OVERFLOW_DROP_CURRENT}; anything else becomes {@link #OVERFLOW_DROP_LAST}
     * @param maxRecords          queue capacity; values below 1 become 1
     */
    public AsyncFileLogHandlerConfig {
        FileLogHandlerConfig file = new FileLogHandlerConfig(
                bufferSize, maxFileBytes, rotateDaily, maxBackupFiles, compressRotatedGzip, formatter);
        bufferSize = file.bufferSize();
        maxFileBytes = file.maxFileBytes();
        rotateDaily = file.rotateDaily();
        maxBackupFiles = file.maxBackupFiles();
        compressRotatedGzip = file.compressRotatedGzip();
        formatter = file.formatter();
        if (overflowDropType < OVERFLOW_DROP_LAST || overflowDropType > OVERFLOW_DROP_CURRENT) {
            overflowDropType = OVERFLOW_DROP_LAST;
        }
        maxRecords = Math.max(maxRecords, 1);
    }

    public AsyncFileLogHandlerConfig(FileLogHandlerConfig fileConfig, int overflowDropType, int maxRecords) {
        this(fileConfig.bufferSize(), fileConfig.maxFileBytes(), fileConfig.rotateDaily(), fileConfig.maxBackupFiles(),
                fileConfig.compressRotatedGzip(), fileConfig.formatter(), overflowDropType, maxRecords);
    }

    /** Same file settings as {@link FileLogHandlerConfig#DEFAULT}, drop-last overflow, queue of 10000. */
    public static final AsyncFileLogHandlerConfig DEFAULT =
            new AsyncFileLogHandlerConfig(FileLogHandlerConfig.DEFAULT, OVERFLOW_DROP_LAST, 10_000);

    /** File settings carried by this config, without the queue. */
    public FileLogHandlerConfig fileLogHandlerConfig() {
        return new FileLogHandlerConfig(
                bufferSize, maxFileBytes, rotateDaily, maxBackupFiles, compressRotatedGzip, formatter);
    }

    public AsyncFileLogHandlerConfig withBufferSize(int size) {
        return new AsyncFileLogHandlerConfig(
                size, maxFileBytes, rotateDaily, maxBackupFiles, compressRotatedGzip, formatter, overflowDropType, maxRecords);
    }

    public AsyncFileLogHandlerConfig withMaxFileBytes(long bytes) {
        return new AsyncFileLogHandlerConfig(
                bufferSize, bytes, rotateDaily, maxBackupFiles, compressRotatedGzip, formatter, overflowDropType, maxRecords);
    }

    public AsyncFileLogHandlerConfig withRotateDaily(boolean daily) {
        return new AsyncFileLogHandlerConfig(
                bufferSize, maxFileBytes, daily, maxBackupFiles, compressRotatedGzip, formatter, overflowDropType, maxRecords);
    }

    public AsyncFileLogHandlerConfig withMaxBackupFiles(int max) {
        return new AsyncFileLogHandlerConfig(
                bufferSize, maxFileBytes, rotateDaily, max, compressRotatedGzip, formatter, overflowDropType, maxRecords);
    }

    public AsyncFileLogHandlerConfig withCompressRotatedGzip(boolean compress) {
        return new AsyncFileLogHandlerConfig(
                bufferSize, maxFileBytes, rotateDaily, maxBackupFiles, compress, formatter, overflowDropType, maxRecords);
    }

    public AsyncFileLogHandlerConfig withFormatter(Formatter customFormatter) {
        return new AsyncFileLogHandlerConfig(
                bufferSize, maxFileBytes, rotateDaily, maxBackupFiles, compressRotatedGzip, customFormatter, overflowDropType, maxRecords);
    }

    public AsyncFileLogHandlerConfig withOverflowDropType(int dropType) {
        return new AsyncFileLogHandlerConfig(
                bufferSize, maxFileBytes, rotateDaily, maxBackupFiles, compressRotatedGzip, formatter, dropType, maxRecords);
    }

    public AsyncFileLogHandlerConfig withMaxRecords(int records) {
        return new AsyncFileLogHandlerConfig(
                bufferSize, maxFileBytes, rotateDaily, maxBackupFiles, compressRotatedGzip, formatter, overflowDropType, records);
    }
}
