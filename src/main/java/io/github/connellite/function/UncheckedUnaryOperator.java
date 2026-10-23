package io.github.connellite.function;

import io.github.connellite.reflection.ReflectionUtil;

import java.util.Objects;
import java.util.function.UnaryOperator;

/** {@link UnaryOperator} that may throw; checked exceptions are rethrown via {@link ReflectionUtil#propagate(Throwable)}. */
@FunctionalInterface
public interface UncheckedUnaryOperator<T> extends UnaryOperator<T> {
    /** Delegates to {@link #uncheckedApply}; checked exceptions are propagated. */
    @Override
    default T apply(T t) {
        try {
            return uncheckedApply(t);
        } catch (Throwable th) {
            ReflectionUtil.propagate(th);
            return null;
        }
    }

    /** Same as {@link #apply}, but may throw. */
    T uncheckedApply(T t) throws Throwable;

    /**
     * Gives a throwing method reference this interface as its target type.
     * The same instance is returned; {@link #apply} still propagates checked exceptions.
     */
    static <T> UncheckedUnaryOperator<T> of(UncheckedUnaryOperator<T> operator) {
        return Objects.requireNonNull(operator);
    }
}
