package io.github.connellite.function;

import io.github.connellite.reflection.ReflectionUtil;

/** {@link Runnable} that may throw; checked exceptions are rethrown via {@link ReflectionUtil#propagate(Throwable)}. */
@FunctionalInterface
public interface UncheckedRunnable extends Runnable {
    /** Delegates to {@link #uncheckedRun}; checked exceptions are propagated. */
    @Override
    default void run() {
        try {
            uncheckedRun();
        } catch (Throwable t) {
            ReflectionUtil.propagate(t);
        }
    }

    /** Same as {@link #run}, but may throw. */
    void uncheckedRun() throws Throwable;
}
