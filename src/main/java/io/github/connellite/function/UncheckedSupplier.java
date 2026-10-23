package io.github.connellite.function;

import io.github.connellite.reflection.ReflectionUtil;

import java.util.Objects;
import java.util.function.Supplier;

/** {@link Supplier} that may throw; checked exceptions are rethrown via {@link ReflectionUtil#propagate(Throwable)}. */
@FunctionalInterface
public interface UncheckedSupplier<T> extends Supplier<T> {

    /** Delegates to {@link #uncheckedGet}; checked exceptions are propagated. */
    @Override
    default T get() {
        try {
            return uncheckedGet();
        } catch (Throwable t) {
            ReflectionUtil.propagate(t);
            return null;
        }
    }

    /** Same as {@link #get}, but may throw. */
    T uncheckedGet() throws Throwable;

    /**
     * Gives a throwing method reference this interface as its target type.
     * The same instance is returned; {@link #get} still propagates checked exceptions.
     */
    static <T> UncheckedSupplier<T> of(UncheckedSupplier<T> supplier) {
        return Objects.requireNonNull(supplier);
    }
}
