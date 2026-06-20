package io.github.connellite.jdbc.internal;

import java.sql.SQLException;

@FunctionalInterface
public interface ThrowingConsumer<T> {
    void accept(T value) throws SQLException;
}
