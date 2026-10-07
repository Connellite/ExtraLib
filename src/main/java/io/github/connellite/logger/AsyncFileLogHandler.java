package io.github.connellite.logger;

import lombok.Getter;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogRecord;

/**
 * {@link FileLogHandler} that queues records and writes them on a background thread.
 * <p>
 * Each handler has its own queue. Size and overflow policy come from {@link AsyncFileLogHandlerConfig}.
 * <p>
 * Tomcat reference:
 * https://github.com/apache/tomcat/blob/05b5d70555068a6647226e9924b2d7f1b9978b03/java/org/apache/juli/AsyncFileHandler.java
 */
@SuppressWarnings("JavadocLinkAsPlainText")
public class AsyncFileLogHandler extends FileLogHandler {

    static final String THREAD_PREFIX = "AsyncFileLogHandlerWriter-";

    private final Object closeLock = new Object();
    /**
     * Indicates whether this handler has been closed.
     */
    protected volatile boolean closed = false;
    @Getter
    private final AsyncFileLogHandlerConfig asyncConfig;
    private volatile LoggerExecutorService loggerService;

    public AsyncFileLogHandler(Path logFile, AsyncFileLogHandlerConfig config) {
        super(logFile, requireFileConfig(config));
        this.asyncConfig = config;
        this.loggerService = new LoggerExecutorService(config.overflowDropType(), config.maxRecords());
    }

    public AsyncFileLogHandler(Path logFile) {
        this(logFile, AsyncFileLogHandlerConfig.DEFAULT);
    }

    private static FileLogHandlerConfig requireFileConfig(AsyncFileLogHandlerConfig config) {
        Objects.requireNonNull(config, "config");
        return config.fileLogHandlerConfig();
    }

    /**
     * Closes this handler. While the JVM is already shutting down, queued records are written first.
     * <p>
     * Tomcat reference:
     * https://github.com/apache/tomcat/blob/05b5d70555068a6647226e9924b2d7f1b9978b03/java/org/apache/juli/AsyncFileHandler.java
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void close() {
        if (closed) {
            return;
        }
        LoggerExecutorService service;
        synchronized (closeLock) {
            if (closed) {
                return;
            }
            closed = true;
            service = loggerService;
        }
        if (isJvmShuttingDown()) {
            service.shutdown();
            try {
                service.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            service.shutdownNow();
        }
        super.close();
        if (!service.isShutdown()) {
            service.shutdownNow();
        }
    }

    /**
     * Reopens a closed handler and starts its writer thread again.
     * <p>
     * Tomcat reference:
     * https://github.com/apache/tomcat/blob/05b5d70555068a6647226e9924b2d7f1b9978b03/java/org/apache/juli/AsyncFileHandler.java
     */
    public void open() {
        if (!closed) {
            return;
        }
        synchronized (closeLock) {
            if (!closed) {
                return;
            }
            closed = false;
            if (loggerService.isShutdown()) {
                loggerService = new LoggerExecutorService(asyncConfig.overflowDropType(), asyncConfig.maxRecords());
            }
        }
        reopen();
    }

    /**
     * Queues the record. Caller data is resolved on this thread before the handoff.
     * <p>
     * Tomcat reference:
     * https://github.com/apache/tomcat/blob/05b5d70555068a6647226e9924b2d7f1b9978b03/java/org/apache/juli/AsyncFileHandler.java
     */
    @Override
    public void publish(LogRecord record) {
        if (closed || !isLoggable(record)) {
            return;
        }
        captureCaller(record);
        LoggerExecutorService service = loggerService;
        if (closed || service.isShutdown()) {
            return;
        }
        try {
            service.execute(() -> {
                /*
                 * Handlers are closed before the executor queue is flushed on JVM shutdown, so the closed flag is ignored
                 * while the executor is terminating.
                 */
                if (!closed || service.isTerminating()) {
                    publishInternal(record);
                }
            });
        } catch (RejectedExecutionException ex) {
            // The handler was closed and its writer stopped between the check above and this handoff.
        }
    }

    /**
     * Publishes a log record to the underlying handler.
     * <p>
     * Tomcat reference:
     * https://github.com/apache/tomcat/blob/05b5d70555068a6647226e9924b2d7f1b9978b03/java/org/apache/juli/AsyncFileHandler.java
     */
    protected void publishInternal(LogRecord record) {
        super.publish(record);
    }

    /**
     * {@link LogRecord} looks up the caller only on the first {@code getSource*} call, by walking the current stack.
     * That walk has to happen on the logging thread. On the writer thread it would record the writer itself.
     */
    private static void captureCaller(LogRecord record) {
        record.getSourceMethodName();
    }

    /**
     * {@code true} when {@link Runtime#addShutdownHook} is already refused, which is how a close during JVM exit is told
     * apart from a normal close.
     */
    @SuppressWarnings({"InstantiatingAThreadWithDefaultRunMethod"})
    private static boolean isJvmShuttingDown() {
        try {
            Thread dummyHook = new Thread();
            Runtime.getRuntime().addShutdownHook(dummyHook);
            Runtime.getRuntime().removeShutdownHook(dummyHook);
            return false;
        } catch (IllegalStateException shuttingDown) {
            return true;
        }
    }

    /**
     * Blocks until tasks queued before this call have finished.
     */
    void awaitIdle() throws InterruptedException, ExecutionException, TimeoutException {
        loggerService.submit(() -> {
        }).get(5, TimeUnit.SECONDS);
    }

    private static class LoggerExecutorService extends ThreadPoolExecutor {

        private static final ThreadFactory THREAD_FACTORY = new ThreadFactory(THREAD_PREFIX);

        LoggerExecutorService(final int overflowDropType, final int maxRecords) {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(maxRecords), THREAD_FACTORY);
            switch (overflowDropType) {
                case AsyncFileLogHandlerConfig.OVERFLOW_DROP_FIRST ->
                        setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
                case AsyncFileLogHandlerConfig.OVERFLOW_DROP_FLUSH ->
                        setRejectedExecutionHandler(new DropFlushPolicy());
                case AsyncFileLogHandlerConfig.OVERFLOW_DROP_CURRENT ->
                        setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
                default -> setRejectedExecutionHandler(new DropLastPolicy());
            }
        }

        @Override
        public LinkedBlockingDeque<Runnable> getQueue() {
            return (LinkedBlockingDeque<Runnable>) super.getQueue();
        }

        private static class DropFlushPolicy implements RejectedExecutionHandler {

            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                while (true) {
                    if (executor.isShutdown()) {
                        break;
                    }
                    try {
                        if (executor.getQueue().offer(r, 1000, TimeUnit.MILLISECONDS)) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RejectedExecutionException("Interrupted", e);
                    }
                }
            }
        }

        private static class DropLastPolicy implements RejectedExecutionHandler {

            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                if (!executor.isShutdown()) {
                    ((LoggerExecutorService) executor).getQueue().pollLast();
                    executor.execute(r);
                }
            }
        }
    }

    /**
     * Daemon threads whose context class loader is this class's loader, not a caller's loader.
     */
    private static final class ThreadFactory implements java.util.concurrent.ThreadFactory {
        private final String namePrefix;
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private ThreadFactory(final String namePrefix) {
            this.namePrefix = namePrefix;
            this.group = Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(group, r, namePrefix + threadNumber.getAndIncrement());
            thread.setContextClassLoader(AsyncFileLogHandler.class.getClassLoader());
            thread.setDaemon(true);
            return thread;
        }
    }
}
