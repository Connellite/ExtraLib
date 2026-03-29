package io.github.connellite.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadPoolTest {

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
    void happyPathCompletes() throws Exception {
        AtomicBoolean ran = new AtomicBoolean();
        try (ThreadPool pool = new ThreadPool(1)) {
            pool.enqueue(() -> ran.set(true));
        }
        assertTrue(ran.get());
    }
}
