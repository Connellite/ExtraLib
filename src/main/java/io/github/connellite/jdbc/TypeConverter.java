package io.github.connellite.jdbc;

import java.sql.SQLException;

/**
 * Custom conversion hook for target field/component type.
 */
@FunctionalInterface
public interface TypeConverter<T> {
    T convert(Object raw) throws SQLException;


    /**
     * Annotation default marker: means no explicit field/component converter is configured.
     */
    final class DefaultConverter implements TypeConverter<Object> {
        @Override
        public Object convert(Object raw) {
            return raw;
        }
    }
}
