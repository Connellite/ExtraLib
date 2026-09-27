package io.github.connellite.concurrent;

import io.github.connellite.logger.Logger;
import io.github.connellite.logger.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager {
    private static final int POOL_TIMEOUT = 1;
    private static final int MAX_TIMEOUTS = 10;
    private static final List<ThreadPoolManager> createdManagers = new CopyOnWriteArrayList<>();
    private final ExecutorService executor;
    private final String name;

    /**
     * Create new thread pool manage.
     */
    public ThreadPoolManager() {
        int availableProcessors = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
        this.executor = Executors.newFixedThreadPool(availableProcessors);
        this.name = Integer.toHexString(hashCode());
        createdManagers.add(this);
    }

    /**
     * Create new thread pool manage.
     *
     * @param nThreads Number of concurrent threads.
     */
    public ThreadPoolManager(int nThreads) {
        this.executor = Executors.newFixedThreadPool(nThreads);
        this.name = Integer.toHexString(hashCode());
        createdManagers.add(this);
    }

    /**
     * Create new thread pool manage.
     *
     * @param nThreads Number of concurrent threads.
     * @param name     Name of the thread pool.
     */
    public ThreadPoolManager(int nThreads, String name) {
        this.executor = Executors.newFixedThreadPool(nThreads);
        this.name = name;
        createdManagers.add(this);
    }

    /**
     * Add a new thread to the manager.
     *
     * @param runnable The Runnable task to add.
     */
    public synchronized void add(Runnable runnable) {
        executor.execute(runnable);
    }

    /**
     * Shutdown pool manager.
     */
    public void shutdown() {
        shutdown(false);
    }

    /**
     * Shutdown pool manager.
     *
     * @param now If the shutdown should be right now or can wait until all thread have finished.
     */
    public synchronized void shutdown(boolean now) {
        if (now) {
            executor.shutdownNow();
        } else {
            executor.shutdown();
        }

        LogHolder.logger.debug("### " + name + ": All threads shutdown requested ###");

        try {
            for (int i = 0; !executor.awaitTermination(POOL_TIMEOUT, TimeUnit.MINUTES); i++) {
                if (i > MAX_TIMEOUTS) {
                    LogHolder.logger.debug("### " + name + ": Killing never ending tasks... (" + i + ") ###");
                    executor.shutdownNow();
                } else {
                    LogHolder.logger.debug("### " + name + ": Awaiting for pool tasks termination... (" + i + ") ###");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LogHolder.logger.error("### " + name + ": Exception awaiting for pool tasks termination: " + e.getMessage() + " ###", e);
        }

        createdManagers.remove(this);
        LogHolder.logger.debug("### " + name + ": All threads have finished ###");
    }

    /**
     * Shutdown all created poll managers.
     */
    public static void shutdownAll() {
        shutdownAll(false);
    }

    /**
     * Shutdown all created poll managers.
     *
     * @param now If the shutdown should be right now or can wait until all thread have finished.
     */
    public static void shutdownAll(boolean now) {
        for (ThreadPoolManager tpm : createdManagers) {
            tpm.shutdown(now);
            LogHolder.logger.debug(tpm.name + ": Shutdown from list");
        }
    }

    private static class LogHolder {
        private static final Logger logger = LoggerFactory.getLogger(ThreadPoolManager.class);
    }
}
