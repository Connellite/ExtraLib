package io.github.connellite.util;

import lombok.experimental.UtilityClass;

import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Runs functional callbacks while swallowing checked and unchecked {@link Exception}s.
 * When a value would be returned, the outcome is {@code null} if the callback throws (or the callback argument is {@code null} where noted).
 * {@link Error} subclasses are not caught and propagate.
 */
@UtilityClass
public class SuppressExceptions {

    /** @param action ignored if {@code null} */
    public static void run(Runnable action) {
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
        if (callable == null) {
            return null;
        }
        try {
            return callable.call();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * @return supplied value, or {@code null} if {@code supplier} is {@code null} or throws
     */
    public static <T> T get(Supplier<? extends T> supplier) {
        if (supplier == null) {
            return null;
        }
        try {
            return supplier.get();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * @return function result, or {@code null} if {@code function} is {@code null} or throws
     */
    public static <T, R> R apply(Function<? super T, ? extends R> function, T arg) {
        if (function == null) {
            return null;
        }
        try {
            return function.apply(arg);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * @return function result, or {@code null} if {@code function} is {@code null} or throws
     */
    public static <T, U, R> R apply(BiFunction<? super T, ? super U, ? extends R> function, T t, U u) {
        if (function == null) {
            return null;
        }
        try {
            return function.apply(t, u);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** @param consumer ignored if {@code null} */
    public static <T> void accept(Consumer<? super T> consumer, T arg) {
        if (consumer == null) {
            return;
        }
        try {
            consumer.accept(arg);
        } catch (Exception ignored) {
        }
    }

    /** @param consumer ignored if {@code null} */
    public static <T, U> void accept(BiConsumer<? super T, ? super U> consumer, T t, U u) {
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
    public static <T> Boolean test(Predicate<? super T> predicate, T arg) {
        if (predicate == null) {
            return null;
        }
        try {
            return predicate.test(arg);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * @return predicate outcome, or {@code null} if {@code predicate} is {@code null} or throws
     */
    public static <T, U> Boolean test(BiPredicate<? super T, ? super U> predicate, T t, U u) {
        if (predicate == null) {
            return null;
        }
        try {
            return predicate.test(t, u);
        } catch (Exception ignored) {
            return null;
        }
    }
}
