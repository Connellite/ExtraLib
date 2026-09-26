package io.github.connellite.collections.function;

import io.github.connellite.reflection.ReflectionUtil;

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
}
