package io.github.connellite.util;

import io.github.connellite.function.UncheckedBiConsumer;
import io.github.connellite.function.UncheckedBiFunction;
import io.github.connellite.function.UncheckedBiPredicate;
import io.github.connellite.function.UncheckedConsumer;
import io.github.connellite.function.UncheckedFunction;
import io.github.connellite.function.UncheckedPredicate;
import io.github.connellite.function.UncheckedRunnable;
import io.github.connellite.function.UncheckedSupplier;
import lombok.experimental.UtilityClass;

import java.util.concurrent.Callable;

/**
 * Runs functional callbacks while swallowing checked and unchecked {@link Exception}s.
 * When a value would be returned, the outcome is {@code null} (or the given fallback) if the callback
 * throws or the callback argument is {@code null}. {@link Error} subclasses are not caught and propagate.
 */
@UtilityClass
public class SuppressExceptions {

    /** @param action ignored if {@code null} */
    public static void run(UncheckedRunnable action) {
        if (action == null) {
            return;
        }
        try {
            action.run();
        } catch (Exception ignored) {
        }
    }

    /**
     * @return callback result, or {@code null} if {@code callable} is {@code null} or throws
     */
    public static <T> T call(Callable<? extends T> callable) {
        return call(callable, null);
    }

    /**
     * @return callback result, or {@code fallback} if {@code callable} is {@code null} or throws
     */
    public static <T> T call(Callable<? extends T> callable, T fallback) {
        if (callable == null) {
            return fallback;
        }
        try {
            return callable.call();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * @return supplied value, or {@code null} if {@code supplier} is {@code null} or throws
     */
    public static <T> T get(UncheckedSupplier<? extends T> supplier) {
        return get(supplier, null);
    }

    /**
     * @return supplied value, or {@code fallback} if {@code supplier} is {@code null} or throws
     */
    public static <T> T get(UncheckedSupplier<? extends T> supplier, T fallback) {
        if (supplier == null) {
            return fallback;
        }
        try {
            return supplier.get();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * @return function result, or {@code null} if {@code function} is {@code null} or throws
     */
    public static <T, R> R apply(UncheckedFunction<? super T, ? extends R> function, T arg) {
        return apply(function, arg, null);
    }

    /**
     * @return function result, or {@code fallback} if {@code function} is {@code null} or throws
     */
    public static <T, R> R apply(UncheckedFunction<? super T, ? extends R> function, T arg, R fallback) {
        if (function == null) {
            return fallback;
        }
        try {
            return function.apply(arg);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * @return function result, or {@code null} if {@code function} is {@code null} or throws
     */
    public static <T, U, R> R apply(UncheckedBiFunction<? super T, ? super U, ? extends R> function, T t, U u) {
        return apply(function, t, u, null);
    }

    /**
     * @return function result, or {@code fallback} if {@code function} is {@code null} or throws
     */
    public static <T, U, R> R apply(UncheckedBiFunction<? super T, ? super U, ? extends R> function, T t, U u, R fallback) {
        if (function == null) {
            return fallback;
        }
        try {
            return function.apply(t, u);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /** @param consumer ignored if {@code null} */
    public static <T> void accept(UncheckedConsumer<? super T> consumer, T arg) {
        if (consumer == null) {
            return;
        }
        try {
            consumer.accept(arg);
        } catch (Exception ignored) {
        }
    }

    /** @param consumer ignored if {@code null} */
    public static <T, U> void accept(UncheckedBiConsumer<? super T, ? super U> consumer, T t, U u) {
        if (consumer == null) {
            return;
        }
        try {
            consumer.accept(t, u);
        } catch (Exception ignored) {
        }
    }

    /**
     * @return predicate outcome, or {@code null} if {@code predicate} is {@code null} or throws
     */
    public static <T> Boolean test(UncheckedPredicate<? super T> predicate, T arg) {
        return test(predicate, arg, null);
    }

    /**
     * @return predicate outcome, or {@code fallback} if {@code predicate} is {@code null} or throws
     */
    public static <T> Boolean test(UncheckedPredicate<? super T> predicate, T arg, Boolean fallback) {
        if (predicate == null) {
            return fallback;
        }
        try {
            return predicate.test(arg);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * @return predicate outcome, or {@code null} if {@code predicate} is {@code null} or throws
     */
    public static <T, U> Boolean test(UncheckedBiPredicate<? super T, ? super U> predicate, T t, U u) {
        return test(predicate, t, u, null);
    }

    /**
     * @return predicate outcome, or {@code fallback} if {@code predicate} is {@code null} or throws
     */
    public static <T, U> Boolean test(UncheckedBiPredicate<? super T, ? super U> predicate, T t, U u, Boolean fallback) {
        if (predicate == null) {
            return fallback;
        }
        try {
            return predicate.test(t, u);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
