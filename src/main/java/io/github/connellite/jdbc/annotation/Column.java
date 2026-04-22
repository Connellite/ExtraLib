package io.github.connellite.jdbc.annotation;

import io.github.connellite.jdbc.SimpleResultSetBeanMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a bean field to a {@link java.sql.ResultSet} column by label (see
 * {@link java.sql.ResultSetMetaData#getColumnLabel(int)} / {@link java.sql.ResultSet#getObject(String)}).
 * When {@link #value()} is blank, the field name is used as the column label.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface Column {

    /**
     * Column label in the result set (same string passed to {@link java.sql.ResultSet#getObject(String)}).
     * When blank, field/record-component name is used.
     */
    String value() default "";

    /**
     * Optional converter class for this field/component.
     * Use {@link SimpleResultSetBeanMapper.DefaultConverter} to indicate "no explicit converter".
     */
    Class<? extends SimpleResultSetBeanMapper.TypeConverter<?>> converter()
            default SimpleResultSetBeanMapper.DefaultConverter.class;
}
