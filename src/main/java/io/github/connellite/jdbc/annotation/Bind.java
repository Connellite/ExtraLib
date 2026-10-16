package io.github.connellite.jdbc.annotation;

import io.github.connellite.jdbc.TypeConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field or record component as a named JDBC parameter.
 * When {@link #value()} is blank, the member name is used.
 * {@link #converter()} runs on the raw value before {@code setObject} or {@code setNull}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface Bind {

    /**
     * Named parameter. When blank, the field or record-component name is used.
     */
    String value() default "";

    /**
     * Optional converter applied to the raw member value, including {@code null}.
     * Use {@link TypeConverter.DefaultConverter} to indicate "no explicit converter".
     * A {@code null} result is bound with {@code setNull}; any other result uses {@code setObject}.
     */
    Class<? extends TypeConverter<?>> converter()
            default TypeConverter.DefaultConverter.class;
}
