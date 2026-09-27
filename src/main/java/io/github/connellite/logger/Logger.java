package io.github.connellite.logger;

import java.util.function.Supplier;

/** Logging facade: SLF4J when {@code slf4j-api} is present, otherwise {@link java.util.logging}. */
public interface Logger {

    void debug(Supplier<String> message);

    default void debug(String message) {
        debug(() -> message);
    }

    void trace(Supplier<String> message);

    default void trace(String message) {
        trace(() -> message);
    }

    void info(Supplier<String> message);

    default void info(String message) {
        info(() -> message);
    }

    void warn(Supplier<String> message);

    default void warn(String message) {
        warn(() -> message);
    }

    void error(Supplier<String> message, Throwable throwable);

    default void error(String message, Throwable throwable) {
        error(() -> message, throwable);
    }
}
