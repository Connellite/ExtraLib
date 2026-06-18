package io.github.connellite.concurrent;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentUtilsTest {

    @Test
    void runWithTimeout_callableReturnsResult() throws Exception {
        String result = ConcurrentUtils.runWithTimeout(() -> "ok", 1, TimeUnit.SECONDS);

        assertEquals("ok", result);
    }

    @Test
    void runWithTimeout_runnableCompletes() throws Exception {
        AtomicBoolean ran = new AtomicBoolean();

        ConcurrentUtils.runWithTimeout(() -> ran.set(true), 1, TimeUnit.SECONDS);

        assertTrue(ran.get());
    }

    @Test
    void runWithTimeout_rethrowsOriginalException() {
        IOException ex = assertThrows(IOException.class,
                () -> ConcurrentUtils.runWithTimeout(
                        () -> {
                            throw new IOException("boom");
                        },
                        1,
                        TimeUnit.SECONDS));

        assertEquals("boom", ex.getMessage());
    }

    @Test
    void runWithTimeout_runnableRethrowsOriginalException() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ConcurrentUtils.runWithTimeout(
                        () -> {
                            throw new IllegalStateException("failed");
                        },
                        1,
                        TimeUnit.SECONDS));

        assertEquals("failed", ex.getMessage());
    }

    @Test
    void runWithTimeout_timeoutWithoutOperationName() {
        SocketTimeoutException ex = assertThrows(SocketTimeoutException.class,
                () -> ConcurrentUtils.runWithTimeout(
                        () -> {
                            Thread.sleep(200);
                            return "late";
                        },
                        50,
                        TimeUnit.MILLISECONDS));

        assertEquals("Operation timed out after 50 milliseconds", ex.getMessage());
    }

    @Test
    void runWithTimeout_timeoutWithOperationName() {
        SocketTimeoutException ex = assertThrows(SocketTimeoutException.class,
                () -> ConcurrentUtils.runWithTimeout(
                        () -> {
                            Thread.sleep(200);
                            return "late";
                        },
                        50,
                        TimeUnit.MILLISECONDS,
                        "db query"));

        assertEquals("db query timed out after 50 milliseconds", ex.getMessage());
    }

    @Test
    void runWithTimeout_supportsNonMillisecondTimeUnit() throws Exception {
        String result = ConcurrentUtils.runWithTimeout(() -> "fast", 2, TimeUnit.SECONDS);

        assertEquals("fast", result);
    }

    @Test
    void runWithTimeout_negativeTimeoutThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ConcurrentUtils.runWithTimeout(() -> "x", -1, TimeUnit.MILLISECONDS));
    }

    @Test
    void runWithTimeout_nullOperationThrows() {
        assertThrows(NullPointerException.class,
                () -> ConcurrentUtils.runWithTimeout((Runnable) null, 1, TimeUnit.SECONDS));
    }
}
