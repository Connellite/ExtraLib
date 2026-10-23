package io.github.connellite.function;

import io.github.connellite.reflection.ReflectionUtil;

import java.util.Objects;
import java.util.function.Consumer;

/** {@link Consumer} that may throw; checked exceptions are rethrown via {@link ReflectionUtil#propagate(Throwable)}. */
@FunctionalInterface
public interface UncheckedConsumer<T> extends Consumer<T> {
    /** Delegates to {@link #uncheckedAccept}; checked exceptions are propagated. */
    @Override
    default void accept(T t) {
        try {
            uncheckedAccept(t);
        } catch (Throwable th) {
            ReflectionUtil.propagate(th);
        }
    }

    /** Same as {@link #accept}, but may throw. */
    void uncheckedAccept(T input) throws Throwable;

    /**
     * Gives a throwing method reference this interface as its target type.
     * The same instance is returned; {@link #accept} still propagates checked exceptions.
     */
    static <T> UncheckedConsumer<T> of(UncheckedConsumer<T> consumer) {
        return Objects.requireNonNull(consumer);
    }
}