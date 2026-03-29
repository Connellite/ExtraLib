package io.github.connellite.concurrent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

public class ThreadPool implements AutoCloseable {
    // need to keep track of threads, so we can join them
    private final List<Thread> workers;

    // the task queue
    private final Queue<Runnable> tasks;

    // synchronization
    private final Object queueMutex;
    private volatile boolean stop;

    /** First failure from a worker thread; checked on {@link #close()} and {@link #checkError()}. */
    private final AtomicReference<Throwable> errorRef = new AtomicReference<>();

    /**
     * The constructor just launches some amount of workers
     * @param threads number of threads
     * @Throws IllegalStateException if number of threads is less than or equal to 0
     */
    public ThreadPool(int threads) {
        if(threads <= 0) {
            throw new IllegalArgumentException("number of threads must be greater than 0");
        }

        workers = new ArrayList<>();
        tasks = new LinkedList<>();
        queueMutex = new Object();
        stop = false;

        for (int i = 0; i < threads; i++) {
            workers.add(new Thread(() -> {
                while (true) {
                    Runnable task;
                    synchronized (queueMutex) {
                        while (!stop && tasks.isEmpty()) {
                            try {
                                queueMutex.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                        if (stop && tasks.isEmpty())
                            return;
                        task = tasks.poll();
                    }
                    runTask(task);
                }
            }));
        }
        workers.forEach(Thread::start);
    }

    /**
     * Add new work item to the pool
     * @param task work item
     * @Throws IllegalStateException if stopped ThreadPool
     */
    public void enqueue(Runnable task) {
        synchronized (queueMutex) {
            // don't allow enqueueing after stopping the pool
            if (stop) {
                throw new IllegalStateException("enqueue on stopped ThreadPool");
            }
            tasks.offer(task);
            queueMutex.notify();
        }
    }

    /**
     * Add new work item to the pool
     * @param task work item
     * @return {@link Future<T>}
     * @param <T> The result type returned by this Future's get method
     * @Throws IllegalStateException if stopped ThreadPool
     */
    public <T> Future<T> enqueue(Callable<T> task) {
        FutureTask<T> future = new FutureTask<>(task);
        synchronized (queueMutex) {
            // don't allow enqueueing after stopping the pool
            if (stop) {
                throw new IllegalStateException("enqueue on stopped ThreadPool");
            }
            tasks.offer(future);
            queueMutex.notify();
        }
        return future;
    }

    private void runTask(Runnable task) {
        try {
            task.run();
            if (task instanceof FutureTask<?> ft) {
                try {
                    ft.get();
                } catch (CancellationException ignored) {
                    // intentional cancellation — not a pool fault
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errorRef.compareAndSet(null, e);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    errorRef.compareAndSet(null, cause != null ? cause : e);
                }
            }
        } catch (Throwable t) {
            errorRef.compareAndSet(null, t);
        }
    }

    /**
     * Throws the first error recorded by a worker, if any.
     *
     * @throws Error            if the cause was an {@link Error}
     * @throws RuntimeException if the cause was unchecked or wrapped
     */
    private void checkError() {
        Throwable err = errorRef.get();
        if (err == null) {
            return;
        }
        if (err instanceof Error e) {
            throw e;
        }
        if (err instanceof RuntimeException re) {
            throw re;
        }
        throw new RuntimeException(err);
    }

    /**
     * Join all threads, stop ThreadPool
     */
    @Override
    public void close() {
        synchronized (queueMutex) {
            stop = true;
            queueMutex.notifyAll();
        }
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        checkError();
    }
}