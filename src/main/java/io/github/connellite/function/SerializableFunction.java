package io.github.connellite.function;

import java.io.Serializable;
import java.util.function.Function;

/** {@link Function} that is {@link Serializable}. */
@FunctionalInterface
public interface SerializableFunction<T, R> extends Function<T, R>, Serializable {
}
