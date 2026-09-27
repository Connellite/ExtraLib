package io.github.connellite.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadPoolManagerTest {

    @Test
    void add_runsTaskAndGracefulShutdown() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        ThreadPoolManager pool = new ThreadPoolManager(1, "test-graceful");
        try {
            pool.add(done::countDown);
            assertTrue(done.await(5, TimeUnit.SECONDS));
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void defaultAndSizedConstructors_acceptWork() throws Exception {
        CountDownLatch first = new CountDownLatch(1);
        CountDownLatch second = new CountDownLatch(1);
        ThreadPoolManager defaultPool = new ThreadPoolManager();
        ThreadPoolManager sized = new ThreadPoolManager(2);
        try {
            defaultPool.add(first::countDown);
            sized.add(second::countDown);
            assertTrue(first.await(5, TimeUnit.SECONDS));
            assertTrue(second.await(5, TimeUnit.SECONDS));
        } finally {
            defaultPool.shutdown();
            sized.shutdown();
        }
    }

    @Test
    void shutdownNow_interruptsWaitingTask() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        ThreadPoolManager pool = new ThreadPoolManager(1, "test-now");
        pool.add(() -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finished.countDown();
            }
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        pool.shutdown(true);
        assertTrue(finished.await(5, TimeUnit.SECONDS));
    }

    @Test
    void add_afterShutdown_isRejected() {
        ThreadPoolManager pool = new ThreadPoolManager(1, "test-rejected");
        pool.shutdown();
        assertThrows(RejectedExecutionException.class, () -> pool.add(() -> {
        }));
    }

    @Test
    void shutdownAll_terminatesCreatedManagers() throws Exception {
        CountDownLatch a = new CountDownLatch(1);
        CountDownLatch b = new CountDownLatch(1);
        new ThreadPoolManager(1, "shutdown-all-a").add(a::countDown);
        new ThreadPoolManager(1, "shutdown-all-b").add(b::countDown);
        assertTrue(a.await(5, TimeUnit.SECONDS));
        assertTrue(b.await(5, TimeUnit.SECONDS));
        ThreadPoolManager.shutdownAll();
    }
}
