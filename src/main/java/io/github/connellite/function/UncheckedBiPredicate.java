package io.github.connellite.function;

import io.github.connellite.reflection.ReflectionUtil;

import java.util.Objects;
import java.util.function.BiPredicate;

/** {@link BiPredicate} that may throw; checked exceptions are rethrown via {@link ReflectionUtil#propagate(Throwable)}. */
@FunctionalInterface
public interface UncheckedBiPredicate<T, U> extends BiPredicate<T, U> {
    /** Delegates to {@link #uncheckedTest}; checked exceptions are propagated. */
    @Override
    default boolean test(T t, U u) {
        try {
            return uncheckedTest(t, u);
        } catch (Throwable th) {
            ReflectionUtil.propagate(th);
            return false;
        }
    }

    /** Same as {@link #test}, but may throw. */
    boolean uncheckedTest(T t, U u) throws Throwable;

    /**
     * Gives a throwing method reference this interface as its target type.
     * The same instance is returned; {@link #test} still propagates checked exceptions.
     */
    static <T, U> UncheckedBiPredicate<T, U> of(UncheckedBiPredicate<T, U> predicate) {
        return Objects.requireNonNull(predicate);
    }
}
