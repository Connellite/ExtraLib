package io.github.connellite.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadPoolTest {

    @Test
    void constructorRejectsNonPositiveThreadCount() {
        assertThrows(IllegalArgumentException.class, () -> new ThreadPool(0));
        assertThrows(IllegalArgumentException.class, () -> new ThreadPool(-1));
    }

    @Test
    void runnableFailureSurfacesOnClose() {
        assertThrows(IllegalArgumentException.class, () -> {
            try (ThreadPool pool = new ThreadPool(1)) {
                pool.enqueue((Runnable) () -> {
                    throw new IllegalArgumentException("boom");
                });
            }
        });
    }

    @Test
    void callableFailureSurfacesOnClose() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            try (ThreadPool pool = new ThreadPool(1)) {
                pool.enqueue((Callable<String>) () -> {
                    throw new Exception("e");
                });
            }
        });
        assertEquals("e", ex.getCause().getMessage());
    }

    @Test
    void happyPathRunnableCompletes() throws Exception {
        AtomicBoolean ran = new AtomicBoolean();
        try (ThreadPool pool = new ThreadPool(1)) {
            pool.enqueue(() -> ran.set(true));
        }
        assertTrue(ran.get());
    }

    @Test
    void happyPathCallableFutureCompletes() throws Exception {
        try (ThreadPool pool = new ThreadPool(1)) {
            Future<String> f = pool.enqueue(() -> "ok");
            assertEquals("ok", f.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void enqueueAfterStopThrows() {
        ThreadPool pool = new ThreadPool(1);
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.enqueue((Runnable) () -> {}));
        assertThrows(IllegalStateException.class, () -> pool.enqueue(() -> "x"));
    }

    @Test
    void secondFailureIsSuppressedOnClose_singleWorker() {
        IllegalArgumentException primary = assertThrows(IllegalArgumentException.class, () -> {
            try (ThreadPool pool = new ThreadPool(1)) {
                pool.enqueue((Runnable) () -> {
                    throw new IllegalArgumentException("first");
                });
                pool.enqueue((Runnable) () -> {
                    throw new IllegalStateException("second");
                });
            }
        });
        assertEquals("first", primary.getMessage());
        Throwable[] suppressed = primary.getSuppressed();
        assertEquals(1, suppressed.length);
        assertInstanceOf(IllegalStateException.class, suppressed[0]);
        assertEquals("second", suppressed[0].getMessage());
    }

    @Test
    void multipleLaterFailuresAllSuppressed_singleWorker() {
        IllegalArgumentException primary = assertThrows(IllegalArgumentException.class, () -> {
            try (ThreadPool pool = new ThreadPool(1)) {
                pool.enqueue((Runnable) () -> {
                    throw new IllegalArgumentException("a");
                });
                pool.enqueue((Runnable) () -> {
                    throw new IllegalStateException("b");
                });
                pool.enqueue((Runnable) () -> {
                    throw new UnsupportedOperationException("c");
                });
            }
        });
        assertEquals(2, primary.getSuppressed().length);
        assertInstanceOf(IllegalStateException.class, primary.getSuppressed()[0]);
        assertInstanceOf(UnsupportedOperationException.class, primary.getSuppressed()[1]);
    }

    @Test
    void concurrentFailures_firstRecordedPrimaryOthersSuppressed() throws Exception {
        int n = 4;
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch go = new CountDownLatch(1);
        IllegalArgumentException primary = assertThrows(IllegalArgumentException.class, () -> {
            try (ThreadPool pool = new ThreadPool(n + 1)) {
                for (int i = 0; i < n; i++) {
                    int id = i;
                    pool.enqueue((Runnable) () -> {
                        ready.countDown();
                        try {
                            go.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError(e);
                        }
                        throw new IllegalStateException("worker-" + id);
                    });
                }
                assertTrue(ready.await(10, TimeUnit.SECONDS));
                pool.enqueue((Runnable) () -> {
                    throw new IllegalArgumentException("primary-task");
                });
                go.countDown();
            }
        });
        assertEquals("primary-task", primary.getMessage());
        assertEquals(n, primary.getSuppressed().length);
        for (Throwable s : primary.getSuppressed()) {
            assertInstanceOf(IllegalStateException.class, s);
        }
    }

    @Test
    void callableCancellationDoesNotRecordPoolError() throws Exception {
        try (ThreadPool pool = new ThreadPool(1)) {
            Future<String> f = pool.enqueue(() -> {
                Thread.sleep(200);
                return "done";
            });
            assertTrue(f.cancel(true));
        }
    }

    @Test
    void errorThrownIsSubclassError_notWrappedAsRuntime() {
        assertThrows(OutOfMemoryError.class, () -> {
            try (ThreadPool pool = new ThreadPool(1)) {
                pool.enqueue((Runnable) () -> {
                    throw new OutOfMemoryError("oom");
                });
            }
        });
    }

    @Test
    void checkedExceptionFromCallableWrappedInExecutionPath() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            try (ThreadPool pool = new ThreadPool(1)) {
                pool.enqueue((Callable<Void>) () -> {
                    throw new java.io.IOException("io");
                });
            }
        });
        assertInstanceOf(java.io.IOException.class, ex.getCause());
        assertEquals("io", ex.getCause().getMessage());
    }

    @Test
    void multiThreadPoolProcessesInParallel() throws Exception {
        int threads = 4;
        CountDownLatch barrier = new CountDownLatch(threads);
        AtomicInteger counter = new AtomicInteger();
        try (ThreadPool pool = new ThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                pool.enqueue(() -> {
                    counter.incrementAndGet();
                    barrier.countDown();
                });
            }
        }
        assertTrue(barrier.await(10, TimeUnit.SECONDS));
        assertEquals(threads, counter.get());
    }
}
