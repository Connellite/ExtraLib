package io.github.connellite.reflection;

/**
 * Custom conversion hook for map field/component mapping.
 */
@FunctionalInterface
public interface MapTypeConverter<T> {
    T convert(Object raw);

    /**
     * Annotation default marker: means no explicit field/component converter is configured.
     */
    final class DefaultConverter implements MapTypeConverter<Object> {
        @Override
        public Object convert(Object raw) {
            return raw;
        }
    }
}
