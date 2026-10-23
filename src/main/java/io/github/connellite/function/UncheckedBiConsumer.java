package io.github.connellite.function;

import io.github.connellite.reflection.ReflectionUtil;

import java.util.Objects;
import java.util.function.BiConsumer;

/** {@link BiConsumer} that may throw; checked exceptions are rethrown via {@link ReflectionUtil#propagate(Throwable)}. */
@FunctionalInterface
public interface UncheckedBiConsumer<T, U> extends BiConsumer<T, U> {
    /** Delegates to {@link #uncheckedAccept}; checked exceptions are propagated. */
    @Override
    default void accept(T t, U u) {
        try {
            uncheckedAccept(t, u);
        } catch (Throwable th) {
            ReflectionUtil.propagate(th);
        }
    }

    /** Same as {@link #accept}, but may throw. */
    void uncheckedAccept(T t, U u) throws Throwable;

    /**
     * Gives a throwing method reference this interface as its target type.
     * The same instance is returned; {@link #accept} still propagates checked exceptions.
     */
    static <T, U> UncheckedBiConsumer<T, U> of(UncheckedBiConsumer<T, U> consumer) {
        return Objects.requireNonNull(consumer);
    }
}
