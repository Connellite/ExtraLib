package io.github.connellite.collections.function;

import io.github.connellite.reflection.ReflectionUtil;

import java.util.function.Function;

/** {@link Function} that may throw; checked exceptions are rethrown via {@link ReflectionUtil#propagate(Throwable)}. */
@FunctionalInterface
public interface UncheckedFunction<T, R> extends Function<T, R> {
    /** Delegates to {@link #uncheckedApply}; checked exceptions are propagated. */
    @Override
    default R apply(T t) {
        try {
            return uncheckedApply(t);
        } catch (Throwable th) {
            ReflectionUtil.propagate(th);
            return null;
        }
    }

    /** Same as {@link #apply}, but may throw. */
    R uncheckedApply(T input) throws Throwable;
}
