package io.github.connellite.function;

import io.github.connellite.reflection.ReflectionUtil;

import java.util.Objects;
import java.util.function.BinaryOperator;

/** {@link BinaryOperator} that may throw; checked exceptions are rethrown via {@link ReflectionUtil#propagate(Throwable)}. */
@FunctionalInterface
public interface UncheckedBinaryOperator<T> extends BinaryOperator<T> {
    /** Delegates to {@link #uncheckedApply}; checked exceptions are propagated. */
    @Override
    default T apply(T t, T u) {
        try {
            return uncheckedApply(t, u);
        } catch (Throwable th) {
            ReflectionUtil.propagate(th);
            return null;
        }
    }

    /** Same as {@link #apply}, but may throw. */
    T uncheckedApply(T t, T u) throws Throwable;

    /**
     * Gives a throwing method reference this interface as its target type.
     * The same instance is returned; {@link #apply} still propagates checked exceptions.
     */
    static <T> UncheckedBinaryOperator<T> of(UncheckedBinaryOperator<T> operator) {
        return Objects.requireNonNull(operator);
    }
}
