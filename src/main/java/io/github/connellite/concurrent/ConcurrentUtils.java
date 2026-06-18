package io.github.connellite.concurrent;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Small helpers for running work with a time limit on a dedicated worker thread.
 */
@UtilityClass
public class ConcurrentUtils {

    /**
     * Runs {@code operation} on a worker thread and waits up to {@code timeout} in {@code unit}.
     *
     * @param operation callable to execute; must not be {@code null}
     * @param timeout   maximum wait time; must be non-negative
     * @param unit      time unit for {@code timeout}; must not be {@code null}
     * @return result of {@code operation}
     * @throws Exception if {@code operation} fails or the wait is interrupted
     */
    public static <T> T runWithTimeout(@NonNull Callable<T> operation, long timeout, @NonNull TimeUnit unit) throws Exception {
        return runWithTimeout(operation, timeout, unit, null);
    }

    /**
     * Runs {@code operation} on a worker thread and waits up to {@code timeout} in {@code unit}.
     *
     * @param operation     callable to execute; must not be {@code null}
     * @param timeout       maximum wait time; must be non-negative
     * @param unit          time unit for {@code timeout}; must not be {@code null}
     * @param operationName optional label used in timeout messages; may be {@code null}
     * @return result of {@code operation}
     * @throws Exception if {@code operation} fails or the wait is interrupted
     */
    public static <T> T runWithTimeout(@NonNull Callable<T> operation, long timeout, @NonNull TimeUnit unit, String operationName) throws Exception {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(unit, "unit");
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be non-negative");
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(operation);
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException timeoutException) {
            future.cancel(true);
            throw createTimeoutException(operationName, timeout, unit);
        } catch (ExecutionException executionException) {
            throw unwrapExecutionException(executionException);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw interruptedException;
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Runs {@code operation} on a worker thread and waits up to {@code timeout} in {@code unit}.
     *
     * @param operation runnable to execute; must not be {@code null}
     * @param timeout   maximum wait time; must be non-negative
     * @param unit      time unit for {@code timeout}; must not be {@code null}
     * @throws Exception if {@code operation} fails or the wait is interrupted
     */
    public static void runWithTimeout(@NonNull Runnable operation, long timeout, @NonNull TimeUnit unit) throws Exception {
        runWithTimeout(operation, timeout, unit, null);
    }

    /**
     * Runs {@code operation} on a worker thread and waits up to {@code timeout} in {@code unit}.
     *
     * @param operation     runnable to execute; must not be {@code null}
     * @param timeout       maximum wait time; must be non-negative
     * @param unit          time unit for {@code timeout}; must not be {@code null}
     * @param operationName optional label used in timeout messages; may be {@code null}
     * @throws Exception if {@code operation} fails or the wait is interrupted
     */
    public static void runWithTimeout(@NonNull Runnable operation, long timeout, @NonNull TimeUnit unit, String operationName) throws Exception {
        runWithTimeout(() -> {
            operation.run();
            return null;
        }, timeout, unit, operationName);
    }

    private static SocketTimeoutException createTimeoutException(String operationName, long timeout, TimeUnit unit) {
        String timeoutLabel = timeout + " " + unit.name().toLowerCase(Locale.ROOT);
        String message = operationName == null || operationName.isEmpty()
                ? "Operation timed out after " + timeoutLabel
                : operationName + " timed out after " + timeoutLabel;
        return new SocketTimeoutException(message);
    }

    private static Exception unwrapExecutionException(ExecutionException executionException) throws Exception {
        Throwable cause = executionException.getCause();
        if (cause instanceof Exception ex) {
            throw ex;
        }
        if (cause instanceof Error err) {
            throw err;
        }
        throw new RuntimeException(cause);
    }
}
