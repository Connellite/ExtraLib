package io.github.connellite.jdbc;

import io.github.connellite.jdbc.annotation.Column;
import io.github.connellite.reflection.internal.ReflectionTypeCoercionUtil;
import io.github.connellite.reflection.ReflectionUtil;
import lombok.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps the current row of a {@link ResultSet} into a simple POJO (no collections, arrays,
 * or nested beans). Field -> column: {@link Column#value()} when non-blank, otherwise the field name.
 * <p>
 * Supported property types: primitives and wrappers, {@link String}, {@link BigDecimal},
 * {@link BigInteger}, {@link UUID}, {@link Enum} (by constant name), {@link java.sql.Date},
 * {@link Time}, {@link Timestamp}, {@link Date}, {@link LocalDate}, {@link LocalTime},
 * {@link LocalDateTime}, {@link Instant}, {@link ZonedDateTime}, {@link OffsetDateTime}.
 * <p>
 * The two-argument constructor accepts labels (for example from
 * {@link ResultSetMetaDataUtils#getColumnLabels(ResultSet)}): every mapped column must
 * appear in that collection or construction throws {@link SQLException}. A {@code null}
 * {@code columnLabels} argument skips that check.
 * @param <T> bean type (no-arg constructor for class beans; canonical constructor for records)
 */
public class SimpleResultSetBeanMapper<T> {

    private static final Set<Class<?>> SCALAR_TYPES = Set.of(
            Object.class,
            String.class,
            UUID.class,
            Boolean.class,
            Character.class,
            byte[].class,
            Blob.class,
            Clob.class,
            java.sql.Date.class,
            Time.class,
            Timestamp.class,
            Date.class,
            LocalDate.class,
            LocalTime.class,
            LocalDateTime.class,
            Instant.class,
            ZonedDateTime.class,
            OffsetDateTime.class
    );

    private final Class<T> beanClass;
    private final List<FieldBinding> bindings;
    private final Constructor<T> recordConstructor;
    private final List<RecordBinding> recordBindings;
    private final boolean scalarMode;
    private final Map<Class<?>, TypeConverter<?>> converters = new ConcurrentHashMap<>();

    /**
     * Builds mapper without metadata validation.
     */
    public SimpleResultSetBeanMapper(Class<T> beanClass) throws SQLException {
        this.beanClass = Objects.requireNonNull(beanClass, "beanClass");
        if (beanClass.isRecord()) {
            this.bindings = List.of();
            this.recordBindings = collectRecordBindings(beanClass);
            this.recordConstructor = ReflectionUtil.resolveRecordConstructor(beanClass);
            this.scalarMode = false;
        } else if (isScalarType(beanClass)) {
            this.bindings = List.of();
            this.recordBindings = List.of();
            this.recordConstructor = null;
            this.scalarMode = true;
        } else {
            this.bindings = collectBindings(beanClass);
            this.recordBindings = List.of();
            this.recordConstructor = null;
            this.scalarMode = false;
        }
    }

    /**
     * Builds mapper and validates that mapped columns exist in {@code columnLabels}.
     */
    public SimpleResultSetBeanMapper(Class<T> beanClass, Collection<String> columnLabels) throws SQLException {
        this(beanClass);
        if (columnLabels == null) {
            return;
        }
        Set<String> available = new HashSet<>();
        for (String label : columnLabels) {
            if (label != null && !label.isBlank()) {
                available.add(label);
            }
        }
        for (FieldBinding b : bindings) {
            if (!available.contains(b.columnName())) {
                throw new SQLException("Result set has no column label: " + b.columnName());
            }
        }
        for (RecordBinding b : recordBindings) {
            if (!available.contains(b.columnName())) {
                throw new SQLException("Result set has no column label: " + b.columnName());
            }
        }
    }

    /**
     * Registers custom converter for target type. Primitive types are normalized to wrappers.
     */
    public <C> SimpleResultSetBeanMapper<T> withConverter(@NonNull Class<C> type, @NonNull TypeConverter<C> converter) {
        converters.put(ReflectionUtil.primitiveToWrapper(type), converter);
        return this;
    }

    /**
     * Maps current row of {@code rs} to target bean/record instance.
     */
    public T mapRow(@NonNull ResultSet rs) throws SQLException {
        if (scalarMode || isOverriddenByRootConverter()) {
            return mapScalarRow(rs);
        }
        if (beanClass.isRecord()) {
            return mapRecordRow(rs);
        }
        final T instance;
        try {
            instance = ReflectionUtil.getInstance(beanClass);
        } catch (ReflectiveOperationException e) {
            throw new SQLException("Cannot instantiate " + beanClass.getName(), e);
        }

        for (FieldBinding b : bindings) {
            Object raw = rs.getObject(b.columnName());
            Object value = coerce(raw, b.field().getType(), b.columnName(), b.converter());
            if (value == null && b.field().getType().isPrimitive()) {
                throw new SQLException("Cannot map null to primitive field '" + b.field().getName() + "'");
            }
            try {
                ReflectionUtil.setValueField(instance, b.field(), value);
            } catch (IllegalAccessException e) {
                throw new SQLException("Cannot set field " + b.field().getDeclaringClass().getName() + "#" + b.field().getName(), e);
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private T mapScalarRow(ResultSet rs) throws SQLException {
        Object raw = rs.getObject(1);
        // In scalar mode we always read the first column; "1" is used only as a column identifier in error messages.
        Object value = coerce(raw, beanClass, "1", null);
        if (value == null && beanClass.isPrimitive()) {
            throw new SQLException("Cannot map null to primitive scalar type '" + beanClass.getName() + "'");
        }
        return (T) value;
    }

    private T mapRecordRow(ResultSet rs) throws SQLException {
        Object[] args = new Object[recordBindings.size()];
        for (int i = 0; i < recordBindings.size(); i++) {
            RecordBinding b = recordBindings.get(i);
            Object raw = rs.getObject(b.columnName());
            Object value = coerce(raw, b.type(), b.columnName(), b.converter());
            if (value == null && b.type().isPrimitive()) {
                throw new SQLException("Cannot map null to primitive record component '" + b.name() + "'");
            }
            args[i] = value;
        }
        try {
            return recordConstructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new SQLException("Cannot instantiate record " + beanClass.getName(), e);
        }
    }

    private static List<FieldBinding> collectBindings(Class<?> beanClass) throws SQLException {
        List<Class<?>> hierarchy = new ArrayList<>();
        for (Class<?> c = beanClass; c != null && c != Object.class; c = c.getSuperclass()) {
            hierarchy.add(c);
        }
        Collections.reverse(hierarchy);
        List<FieldBinding> out = new ArrayList<>();
        for (Class<?> type : hierarchy) {
            for (Field field : type.getDeclaredFields()) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || field.isSynthetic()) {
                    continue;
                }
                Column col = field.getAnnotation(Column.class);
                String columnName = col != null && !col.value().isBlank() ? col.value() : field.getName();
                TypeConverter<?> converter = resolveAnnotationConverter(col);
                field.trySetAccessible();
                out.add(new FieldBinding(field, columnName, converter));
            }
        }
        return out;
    }

    private static List<RecordBinding> collectRecordBindings(Class<?> beanClass) throws SQLException {
        RecordComponent[] components = beanClass.getRecordComponents();
        List<RecordBinding> out = new ArrayList<>(components.length);
        for (RecordComponent component : components) {
            Column col = component.getAnnotation(Column.class);
            String columnName = col != null && !col.value().isBlank() ? col.value() : component.getName();
            TypeConverter<?> converter = resolveAnnotationConverter(col);
            out.add(new RecordBinding(component.getName(), component.getType(), columnName, converter));
        }
        return out;
    }

    private static boolean isScalarType(Class<?> beanClass) {
        Class<?> boxed = ReflectionUtil.primitiveToWrapper(beanClass);
        return SCALAR_TYPES.contains(boxed)
                || Number.class.isAssignableFrom(boxed)
                || boxed.isEnum();
    }

    // Checks whether a custom converter is registered for the root bean type itself.
    private boolean isOverriddenByRootConverter() {
        Class<?> boxedBeanClass = ReflectionUtil.primitiveToWrapper(beanClass);
        return converters.containsKey(boxedBeanClass);
    }

    @SuppressWarnings("unchecked")
    private Object coerce(Object raw, Class<?> fieldType, String columnName, TypeConverter<?> explicitConverter) throws SQLException {
        if (explicitConverter != null) {
            return ((TypeConverter<Object>) explicitConverter).convert(raw);
        }
        Class<?> boxed = ReflectionUtil.primitiveToWrapper(fieldType);
        TypeConverter<?> converter = converters.get(boxed);
        if (converter != null) {
            return ((TypeConverter<Object>) converter).convert(raw);
        }
        try {
            return ReflectionTypeCoercionUtil.coerceDefault(raw, fieldType, columnName);
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    private static TypeConverter<?> resolveAnnotationConverter(Column col) throws SQLException {
        if (col == null || col.converter() == TypeConverter.DefaultConverter.class) {
            return null;
        }
        try {
            return ReflectionUtil.getInstance(col.converter());
        } catch (ReflectiveOperationException e) {
            throw new SQLException("Cannot instantiate converter " + col.converter().getName(), e);
        }
    }

    private record FieldBinding(Field field, String columnName, TypeConverter<?> converter) {
    }

    private record RecordBinding(String name, Class<?> type, String columnName, TypeConverter<?> converter) {
    }
}
