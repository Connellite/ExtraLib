package io.github.connellite.collections.function;

import io.github.connellite.reflection.ReflectionUtil;

import java.util.function.BiFunction;

/** {@link BiFunction} that may throw; checked exceptions are rethrown via {@link ReflectionUtil#propagate(Throwable)}. */
@FunctionalInterface
public interface UncheckedBiFunction<T, U, R> extends BiFunction<T, U, R> {
    /** Delegates to {@link #uncheckedApply}; checked exceptions are propagated. */
    @Override
    default R apply(T t, U u) {
        try {
            return uncheckedApply(t, u);
        } catch (Throwable th) {
            ReflectionUtil.propagate(th);
            return null;
        }
    }

    /** Same as {@link #apply}, but may throw. */
    R uncheckedApply(T t, U u) throws Throwable;
}
