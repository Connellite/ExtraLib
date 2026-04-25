package io.github.connellite.jdbc;

import io.github.connellite.jdbc.annotation.Column;
import io.github.connellite.util.DateTimeUtil;
import io.github.connellite.util.NumberUtils;
import io.github.connellite.util.ReflectionUtil;
import io.github.connellite.util.UuidUtil;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
 * <p>
 * A row may also be supplied as a {@link Map} of string label to value; see {@link #mapRow(Map)}.
 * Scalar types ({@code Integer}, {@code String}, and so on) are supported only with {@link #mapRow(ResultSet)}, not with {@link #mapRow(Map)}.
 *
 * @param <T> bean type (no-arg constructor for class beans; canonical constructor for records)
 */
public class SimpleResultSetBeanMapper<T> {

    private static final Set<Class<?>> SCALAR_TYPES = Set.of(
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
    private final Map<Class<?>, TypeConverter<?>> converters = new HashMap<>();

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
        if (scalarMode || hasRootConverter()) {
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

    /**
     * Maps one logical row expressed as a map from string key to value. Keys must match the labels this mapper uses
     * for properties (the same labels as for {@link #mapRow(ResultSet)}). The map may come from any source as long as
     * keys and values are compatible with the mapper's coercion rules.
     * <p>
     * A missing key behaves like a {@code null} value for the corresponding mapped property.
     * <p>
     * Scalar target types (for example {@code Integer.class}) are not supported for this overload;
     *
     * @param row one row as label to value; must not be {@code null}
     * @return mapped bean or record instance
     * @throws SQLException if the mapper was built for a scalar type or coercion fails
     */
    public T mapRow(@NonNull Map<String, Object> row) throws SQLException {
        if (scalarMode) {
            throw new SQLException("Scalar mapping is not supported for mapRow(Map<String, Object>);");
        }
        if (beanClass.isRecord()) {
            return mapRecordRowFromMap(row);
        }
        final T instance;
        try {
            instance = ReflectionUtil.getInstance(beanClass);
        } catch (ReflectiveOperationException e) {
            throw new SQLException("Cannot instantiate " + beanClass.getName(), e);
        }

        for (FieldBinding b : bindings) {
            Object raw = row.get(b.columnName());
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

    private T mapRecordRowFromMap(Map<String, Object> row) throws SQLException {
        Object[] args = new Object[recordBindings.size()];
        for (int i = 0; i < recordBindings.size(); i++) {
            RecordBinding b = recordBindings.get(i);
            Object raw = row.get(b.columnName());
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
    private boolean hasRootConverter() {
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
        return coerceDefault(raw, fieldType, columnName);
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object coerceDefault(Object raw, Class<?> fieldType, String columnName) throws SQLException {
        if (raw == null) {
            return null;
        }
        Class<?> boxed = ReflectionUtil.primitiveToWrapper(fieldType);

        if (boxed == String.class) {
            if (raw instanceof String s) return s;
            if (raw instanceof Clob clob) return LobUtils.convertClobToString(clob);
            return Objects.toString(raw, null);
        }

        if (boxed == byte[].class) {
            if (raw instanceof byte[] bytes) return bytes;
            if (raw instanceof Blob blob) return LobUtils.convertBlobToByteArray(blob);
            return null;
        }

        if (boxed.isInstance(raw) && !(raw instanceof Number)) {
            return raw;
        }
        if (boxed.isInstance(raw)) {
            return narrowNumber((Number) raw, boxed);
        }

        if (boxed == Boolean.class) {
            if (raw instanceof Boolean b) return b;
            if (raw instanceof Number n) return NumberUtils.toBoolean(n.longValue());
            if (raw instanceof String s) return NumberUtils.toBoolean(s);
            return null;
        }

        if (Number.class.isAssignableFrom(boxed)) {
            if (raw instanceof Number n) return narrowNumber(n, boxed);
            if (raw instanceof String s) {
                Class<? extends Number> numClass = (Class<? extends Number>) boxed;
                return NumberUtils.parseNumber(s, numClass);
            }
            return null;
        }

        if (boxed == Character.class) {
            if (raw instanceof Character c) return c;
            if (raw instanceof Number n) return (char) n.intValue();
            if (raw instanceof String s) return s.isEmpty() ? null : s.charAt(0);
            return null;
        }

        if (boxed == UUID.class) {
            try {
                return UuidUtil.convert2Uuid(raw);
            } catch (IllegalArgumentException e) {
                throw new SQLException("Cannot map column '" + columnName + "' to UUID", e);
            }
        }

        if (boxed.isEnum()) {
            Class<? extends Enum> enumClass = (Class<? extends Enum>) boxed;
            if (raw instanceof String s) {
                String name = s.trim();
                if (name.isEmpty()) return null;
                try {
                    return Enum.valueOf(enumClass, name);
                } catch (IllegalArgumentException e) {
                    throw new SQLException("Cannot map column '" + columnName + "' to enum " + enumClass.getName(), e);
                }
            }
            if (enumClass.isInstance(raw)) return raw;
            throw new SQLException("Cannot map column '" + columnName + "' to enum " + enumClass.getName());
        }

        if (boxed == java.sql.Date.class) {
            if (raw instanceof java.sql.Date d) return d;
            if (raw instanceof Date d) return new java.sql.Date(d.getTime());
            if (raw instanceof String s) {
                try {
                    return java.sql.Date.valueOf(DateTimeUtil.parseLocalDate(s));
                } catch (IllegalArgumentException e) {
                    throw new SQLException("Cannot map column '" + columnName + "' to java.sql.Date", e);
                }
            }
            return null;
        }

        if (boxed == Clob.class) {
            if (raw instanceof Clob clob) return clob;
            if (raw instanceof String s) return LobUtils.createClob(s);
            return null;
        }

        if (boxed == Blob.class) {
            if (raw instanceof Blob blob) return blob;
            if (raw instanceof byte[] bytes) return LobUtils.createBlob(bytes);
            return null;
        }

        if (boxed == Time.class) {
            if (raw instanceof Time t) return t;
            if (raw instanceof Timestamp ts) return new Time(ts.getTime());
            if (raw instanceof Date d) return new Time(d.getTime());
            if (raw instanceof String s) {
                try {
                    return Time.valueOf(DateTimeUtil.parseLocalTime(s));
                } catch (IllegalArgumentException e) {
                    throw new SQLException("Cannot map column '" + columnName + "' to java.sql.Time", e);
                }
            }
            return null;
        }

        if (boxed == Timestamp.class) {
            if (raw instanceof Timestamp ts) return ts;
            if (raw instanceof Date d) return new Timestamp(d.getTime());
            if (raw instanceof String s) {
                try {
                    return Timestamp.valueOf(DateTimeUtil.parseLocalDateTime(s));
                } catch (IllegalArgumentException e) {
                    throw new SQLException("Cannot map column '" + columnName + "' to java.sql.Timestamp", e);
                }
            }
            return null;
        }

        if (boxed == Date.class) {
            if (raw instanceof Timestamp ts) return new Date(ts.getTime());
            if (raw instanceof java.sql.Date d) return new Date(d.getTime());
            if (raw instanceof Date d) return d;
            if (raw instanceof LocalDateTime ldt) return DateTimeUtil.toDate(ldt);
            if (raw instanceof LocalDate ld) return DateTimeUtil.toDate(ld);
            if (raw instanceof Instant ins) return DateTimeUtil.toDate(ins);
            if (raw instanceof String s) {
                try {
                    return DateTimeUtil.toDate(DateTimeUtil.parseLocalDateTime(s));
                } catch (IllegalArgumentException e) {
                    try {
                        return DateTimeUtil.toDate(DateTimeUtil.parseLocalDate(s));
                    } catch (IllegalArgumentException e2) {
                        throw new SQLException("Cannot map column '" + columnName + "' to java.util.Date", e2);
                    }
                }
            }
            return null;
        }

        if (boxed == LocalDate.class) {
            if (raw instanceof LocalDate ld) return ld;
            if (raw instanceof java.sql.Date d) return DateTimeUtil.toLocalDate(d);
            if (raw instanceof Timestamp ts) return DateTimeUtil.toLocalDate(ts);
            if (raw instanceof Date d) return DateTimeUtil.toLocalDate(d);
            if (raw instanceof Instant ins) return DateTimeUtil.toLocalDate(ins);
            if (raw instanceof ZonedDateTime zdt) return DateTimeUtil.toLocalDate(zdt);
            if (raw instanceof OffsetDateTime odt) return DateTimeUtil.toLocalDate(odt);
            if (raw instanceof String s) {
                try {
                    return DateTimeUtil.parseLocalDate(s);
                } catch (IllegalArgumentException e) {
                    throw new SQLException("Cannot map column '" + columnName + "' to LocalDate", e);
                }
            }
            return null;
        }

        if (boxed == LocalTime.class) {
            if (raw instanceof LocalTime lt) return lt;
            if (raw instanceof LocalDateTime ldt) return DateTimeUtil.toLocalTime(ldt);
            if (raw instanceof Time t) return DateTimeUtil.toLocalTime(t);
            if (raw instanceof Timestamp ts) return DateTimeUtil.toLocalTime(ts);
            if (raw instanceof Date d) return DateTimeUtil.toLocalTime(d);
            if (raw instanceof Instant ins) return DateTimeUtil.toLocalTime(DateTimeUtil.toDate(ins));
            if (raw instanceof ZonedDateTime zdt) return DateTimeUtil.toLocalTime(DateTimeUtil.toDate(zdt));
            if (raw instanceof OffsetDateTime odt) return DateTimeUtil.toLocalTime(DateTimeUtil.toDate(odt));
            if (raw instanceof String s) {
                try {
                    return DateTimeUtil.parseLocalTime(s);
                } catch (IllegalArgumentException e) {
                    throw new SQLException("Cannot map column '" + columnName + "' to LocalTime", e);
                }
            }
            return null;
        }

        if (boxed == LocalDateTime.class) {
            if (raw instanceof LocalDateTime ldt) return ldt;
            if (raw instanceof LocalDate ld) return DateTimeUtil.toLocalDateTime(ld);
            if (raw instanceof Timestamp ts) return DateTimeUtil.toLocalDateTime(ts);
            if (raw instanceof Date d) return DateTimeUtil.toLocalDateTime(d);
            if (raw instanceof Instant ins) return DateTimeUtil.toLocalDateTime(ins);
            if (raw instanceof ZonedDateTime zdt) return DateTimeUtil.toLocalDateTime(zdt);
            if (raw instanceof OffsetDateTime odt) return DateTimeUtil.toLocalDateTime(odt);
            if (raw instanceof String s) {
                try {
                    return DateTimeUtil.parseLocalDateTime(s);
                } catch (IllegalArgumentException e) {
                    throw new SQLException("Cannot map column '" + columnName + "' to LocalDateTime", e);
                }
            }
            return null;
        }

        if (boxed == Instant.class) {
            if (raw instanceof Instant ins) return ins;
            if (raw instanceof ZonedDateTime zdt) return zdt.toInstant();
            if (raw instanceof OffsetDateTime odt) return odt.toInstant();
            if (raw instanceof LocalDateTime ldt) return DateTimeUtil.toZonedDateTime(ldt).toInstant();
            if (raw instanceof LocalDate ld) return DateTimeUtil.toZonedDateTime(ld).toInstant();
            if (raw instanceof Timestamp ts) return ts.toInstant();
            if (raw instanceof Date d) return d.toInstant();
            if (raw instanceof String s) {
                try {
                    return DateTimeUtil.toZonedDateTime(DateTimeUtil.parseLocalDateTime(s)).toInstant();
                } catch (IllegalArgumentException e) {
                    throw new SQLException("Cannot map column '" + columnName + "' to Instant", e);
                }
            }
            return null;
        }

        if (boxed == ZonedDateTime.class) {
            if (raw instanceof ZonedDateTime zdt) return zdt;
            if (raw instanceof OffsetDateTime odt) return odt.toZonedDateTime();
            if (raw instanceof Instant ins) return DateTimeUtil.toZonedDateTime(ins);
            if (raw instanceof LocalDateTime ldt) return DateTimeUtil.toZonedDateTime(ldt);
            if (raw instanceof LocalDate ld) return DateTimeUtil.toZonedDateTime(ld);
            if (raw instanceof Timestamp ts) return DateTimeUtil.toZonedDateTime(ts);
            if (raw instanceof Date d) return DateTimeUtil.toZonedDateTime(d);
            if (raw instanceof String s) {
                try {
                    return DateTimeUtil.toZonedDateTime(DateTimeUtil.parseLocalDateTime(s));
                } catch (IllegalArgumentException e) {
                    throw new SQLException("Cannot map column '" + columnName + "' to ZonedDateTime", e);
                }
            }
            return null;
        }

        if (boxed == OffsetDateTime.class) {
            if (raw instanceof OffsetDateTime odt) return odt;
            if (raw instanceof Instant ins) return DateTimeUtil.toOffsetDateTime(ins);
            if (raw instanceof ZonedDateTime zdt) return DateTimeUtil.toOffsetDateTime(zdt);
            if (raw instanceof LocalDateTime ldt) return DateTimeUtil.toOffsetDateTime(ldt);
            if (raw instanceof LocalDate ld) return DateTimeUtil.toOffsetDateTime(DateTimeUtil.toLocalDateTime(ld));
            if (raw instanceof Timestamp ts) return DateTimeUtil.toOffsetDateTime(ts);
            if (raw instanceof Date d) return DateTimeUtil.toOffsetDateTime(d);
            if (raw instanceof String s) {
                try {
                    return DateTimeUtil.toOffsetDateTime(DateTimeUtil.parseLocalDateTime(s));
                } catch (IllegalArgumentException e) {
                    throw new SQLException("Cannot map column '" + columnName + "' to OffsetDateTime", e);
                }
            }
            return null;
        }
        throw new SQLException("Unsupported field type " + fieldType.getName() + " for column '" + columnName + "'");
    }

    private static Number narrowNumber(Number n, Class<?> boxed) {
        if (boxed == Byte.class) return n.byteValue();
        if (boxed == Short.class) return n.shortValue();
        if (boxed == Integer.class) return n.intValue();
        if (boxed == Long.class) return n.longValue();
        if (boxed == Float.class) return n.floatValue();
        if (boxed == Double.class) return n.doubleValue();
        if (boxed == BigDecimal.class) return new BigDecimal(n.toString());
        if (boxed == BigInteger.class) {
            if (n instanceof BigDecimal bd) return bd.toBigInteger();
            return BigInteger.valueOf(n.longValue());
        }
        return n;
    }

    private record FieldBinding(Field field, String columnName, TypeConverter<?> converter) {
    }

    private record RecordBinding(String name, Class<?> type, String columnName, TypeConverter<?> converter) {
    }
}
