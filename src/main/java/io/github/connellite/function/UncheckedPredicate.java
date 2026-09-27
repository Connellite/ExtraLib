package io.github.connellite.function;

import io.github.connellite.reflection.ReflectionUtil;

import java.util.function.Predicate;

/** {@link Predicate} that may throw; checked exceptions are rethrown via {@link ReflectionUtil#propagate(Throwable)}. */
@FunctionalInterface
public interface UncheckedPredicate<T> extends Predicate<T> {
    /** Delegates to {@link #uncheckedTest}; checked exceptions are propagated. */
    @Override
    default boolean test(T t) {
        try {
            return uncheckedTest(t);
        } catch (Throwable th) {
            ReflectionUtil.propagate(th);
            return false;
        }
    }

    /** Same as {@link #test}, but may throw. */
    boolean uncheckedTest(T t) throws Throwable;
}
