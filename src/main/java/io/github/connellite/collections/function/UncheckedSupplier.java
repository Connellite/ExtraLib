package io.github.connellite.collections.function;

import io.github.connellite.reflection.ReflectionUtil;

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
}
