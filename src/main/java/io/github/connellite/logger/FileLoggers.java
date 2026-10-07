package io.github.connellite.logger;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Attaches a {@link FileLogHandler} to {@link java.util.logging.Logger} instances.
 * <p>
 * Example (one file per class), {@code logger} name is a short string (for example the file name):
 * </p>
 * <pre>{@code
 * import java.lang.invoke.MethodHandles;
 *
 * private static final Logger LOGGER = FileLoggers.forLogFile(
 *         MethodHandles.lookup().lookupClass().getSimpleName() + ".log",
 *         Path.of("logs", "modules", MethodHandles.lookup().lookupClass().getSimpleName() + ".log"));
 *
 * static {
 *     LOGGER.setLevel(Level.FINE);
 * }
 * }</pre>
 * <p>
 * Shorter overload: {@link #forLogFile(Path)} uses {@link Path#getFileName()} as the logger name.
 * Async counterparts are {@link #forAsyncLogFile(String, Path, AsyncFileLogHandlerConfig)} and
 * {@link #addAsyncFileHandler(Logger, Path, AsyncFileLogHandlerConfig)}.
 * </p>
 * <p>
 * Each record is one line from the handler's {@link java.util.logging.Formatter}
 * ({@link java.util.logging.SimpleFormatter} by default; override via {@link FileLogHandlerConfig#withFormatter}).
 * </p>
 */
@UtilityClass
public final class FileLoggers {

    /**
     * {@code Logger.getLogger(loggerName)} plus a single {@link FileLogHandler} for {@code logFile} (no parent handlers).
     */
    public static Logger forLogFile(String loggerName, Path logFile, FileLogHandlerConfig config) {
        Objects.requireNonNull(loggerName,  "loggerName must not be null");
        Path normalized = logFile.toAbsolutePath().normalize();
        Logger logger = Logger.getLogger(loggerName);
        synchronized (logger) {
            logger.setUseParentHandlers(false);
            if (findOurHandlerForFile(logger, normalized, config) != null) {
                if (logger.getLevel() == null) {
                    logger.setLevel(Level.FINE);
                }
                return logger;
            }
            removeOurHandlersForFile(logger, normalized);
            logger.addHandler(new FileLogHandler(normalized, config));
            if (logger.getLevel() == null) {
                logger.setLevel(Level.FINE);
            }
        }
        return logger;
    }

    public static Logger forLogFile(String loggerName, Path logFile) {
        return forLogFile(loggerName, logFile, FileLogHandlerConfig.DEFAULT);
    }

    /**
     * Uses {@code logFile.getFileName().toString()} as the logger name (two different paths with the same file name
     * share one {@link Logger} — pass an explicit name if that is a problem).
     */
    public static Logger forLogFile(Path logFile) {
        return forLogFile(logFile.getFileName().toString(), logFile);
    }

    public static Logger forLogFile(Path logFile, FileLogHandlerConfig config) {
        return forLogFile(logFile.getFileName().toString(), logFile, config);
    }

    public static FileLogHandler addFileHandler(Logger logger, Path logFile, FileLogHandlerConfig config) {
        Objects.requireNonNull(logger, "logger");
        Path normalized = logFile.toAbsolutePath().normalize();
        synchronized (logger) {
            FileLogHandler existing = findOurHandlerForFile(logger, normalized, config);
            if (existing != null) {
                return existing;
            }
            removeOurHandlersForFile(logger, normalized);
            FileLogHandler h = new FileLogHandler(normalized, config);
            logger.addHandler(h);
            return h;
        }
    }

    public static FileLogHandler addFileHandler(Logger logger, Path logFile) {
        return addFileHandler(logger, logFile, FileLogHandlerConfig.DEFAULT);
    }

    /**
     * {@code Logger.getLogger(loggerName)} plus a single {@link AsyncFileLogHandler} for {@code logFile}
     * (no parent handlers).
     */
    public static Logger forAsyncLogFile(String loggerName, Path logFile, AsyncFileLogHandlerConfig config) {
        Objects.requireNonNull(loggerName, "loggerName must not be null");
        Objects.requireNonNull(config, "config");
        Path normalized = logFile.toAbsolutePath().normalize();
        Logger logger = Logger.getLogger(loggerName);
        synchronized (logger) {
            logger.setUseParentHandlers(false);
            if (findOurAsyncHandlerForFile(logger, normalized, config) != null) {
                if (logger.getLevel() == null) {
                    logger.setLevel(Level.FINE);
                }
                return logger;
            }
            removeOurHandlersForFile(logger, normalized);
            logger.addHandler(new AsyncFileLogHandler(normalized, config));
            if (logger.getLevel() == null) {
                logger.setLevel(Level.FINE);
            }
        }
        return logger;
    }

    public static Logger forAsyncLogFile(String loggerName, Path logFile) {
        return forAsyncLogFile(loggerName, logFile, AsyncFileLogHandlerConfig.DEFAULT);
    }

    /**
     * Uses {@code logFile.getFileName().toString()} as the logger name (two different paths with the same file name
     * share one {@link Logger} — pass an explicit name if that is a problem).
     */
    public static Logger forAsyncLogFile(Path logFile) {
        return forAsyncLogFile(logFile.getFileName().toString(), logFile);
    }

    public static Logger forAsyncLogFile(Path logFile, AsyncFileLogHandlerConfig config) {
        return forAsyncLogFile(logFile.getFileName().toString(), logFile, config);
    }

    public static AsyncFileLogHandler addAsyncFileHandler(Logger logger, Path logFile, AsyncFileLogHandlerConfig config) {
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(config, "config");
        Path normalized = logFile.toAbsolutePath().normalize();
        synchronized (logger) {
            AsyncFileLogHandler existing = findOurAsyncHandlerForFile(logger, normalized, config);
            if (existing != null) {
                return existing;
            }
            removeOurHandlersForFile(logger, normalized);
            AsyncFileLogHandler handler = new AsyncFileLogHandler(normalized, config);
            logger.addHandler(handler);
            return handler;
        }
    }

    public static AsyncFileLogHandler addAsyncFileHandler(Logger logger, Path logFile) {
        return addAsyncFileHandler(logger, logFile, AsyncFileLogHandlerConfig.DEFAULT);
    }

    private static FileLogHandler findOurHandlerForFile(Logger logger, Path normalized, FileLogHandlerConfig config) {
        for (Handler h : logger.getHandlers()) {
            if (h instanceof FileLogHandler fh
                    && !(h instanceof AsyncFileLogHandler)
                    && fh.getLogFile().equals(normalized)
                    && fh.getConfig().equals(config)) {
                return fh;
            }
        }
        return null;
    }

    private static AsyncFileLogHandler findOurAsyncHandlerForFile(Logger logger, Path normalized, AsyncFileLogHandlerConfig config) {
        for (Handler h : logger.getHandlers()) {
            if (h instanceof AsyncFileLogHandler ah
                    && ah.getLogFile().equals(normalized)
                    && ah.getAsyncConfig().equals(config)) {
                return ah;
            }
        }
        return null;
    }

    private static void removeOurHandlersForFile(Logger logger, Path normalized) {
        for (Handler h : logger.getHandlers()) {
            if (h instanceof FileLogHandler fh && fh.getLogFile().equals(normalized)) {
                logger.removeHandler(fh);
                fh.close();
            }
        }
    }
}
