package io.github.connellite.reflection;

import io.github.connellite.reflection.annotation.MapField;
import io.github.connellite.reflection.internal.ReflectionTypeCoercionUtil;
import lombok.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Maps one logical row represented as {@code Map<String, Object>} into bean/record instance.
 *
 * @param <T> bean type (no-arg constructor for class beans; canonical constructor for records)
 */
public class SimpleMapBeanMapper<T> {

    private final Class<T> beanClass;
    private final List<FieldBinding> bindings;
    private final Constructor<T> recordConstructor;
    private final List<RecordBinding> recordBindings;

    public SimpleMapBeanMapper(Class<T> beanClass) {
        this.beanClass = Objects.requireNonNull(beanClass, "beanClass");
        if (beanClass.isRecord()) {
            this.bindings = List.of();
            this.recordBindings = collectRecordBindings(beanClass);
            this.recordConstructor = ReflectionUtil.resolveRecordConstructor(beanClass);
            return;
        }
        this.bindings = collectBindings(beanClass);
        this.recordBindings = List.of();
        this.recordConstructor = null;
    }

    /**
     * Maps one logical row expressed as a map from string key to value.
     */
    public T mapRow(@NonNull Map<String, Object> row) {
        if (beanClass.isRecord()) {
            return mapRecordRowFromMap(row);
        }

        final T instance;
        try {
            instance = ReflectionUtil.getInstance(beanClass);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate " + beanClass.getName(), e);
        }

        for (FieldBinding b : bindings) {
            Object raw = row.get(b.key());
            Object value = coerce(raw, b.field().getType(), b.key(), "field '" + b.field().getName() + "'");
            try {
                ReflectionUtil.setValueField(instance, b.field(), value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot set field " + b.field().getDeclaringClass().getName() + "#" + b.field().getName(), e);
            }
        }
        return instance;
    }

    private T mapRecordRowFromMap(Map<String, Object> row) {
        Object[] args = new Object[recordBindings.size()];
        for (int i = 0; i < recordBindings.size(); i++) {
            RecordBinding b = recordBindings.get(i);
            Object raw = b.ignored() ? null : row.get(b.key());
            args[i] = coerce(raw, b.type(), b.key(), "record component '" + b.name() + "'");
        }
        try {
            return recordConstructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate record " + beanClass.getName(), e);
        }
    }

    private static Object coerce(Object raw, Class<?> targetType, String mapKey, String nullPrimitiveOwner) {
        Object value;
        try {
            value = ReflectionTypeCoercionUtil.coerceDefault(raw, targetType, mapKey);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        if (value == null && targetType.isPrimitive()) {
            throw new IllegalArgumentException("Cannot map null to primitive " + nullPrimitiveOwner);
        }
        return value;
    }

    private static List<FieldBinding> collectBindings(Class<?> beanClass) {
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
                MapField mapField = field.getAnnotation(MapField.class);
                if (mapField != null && mapField.ignore()) {
                    continue;
                }
                String key = resolveKey(field.getName(), mapField);
                out.add(new FieldBinding(field, key));
            }
        }
        return out;
    }

    private static List<RecordBinding> collectRecordBindings(Class<?> beanClass) {
        RecordComponent[] components = beanClass.getRecordComponents();
        List<RecordBinding> out = new ArrayList<>(components.length);
        for (RecordComponent component : components) {
            MapField mapField = component.getAnnotation(MapField.class);
            String key = resolveKey(component.getName(), mapField);
            out.add(new RecordBinding(component.getName(), component.getType(), key, mapField != null && mapField.ignore()));
        }
        return out;
    }

    private static String resolveKey(String defaultName, MapField mapField) {
        if (mapField == null || mapField.key().isBlank()) {
            return defaultName;
        }
        return mapField.key();
    }

    private record FieldBinding(Field field, String key) {
    }

    private record RecordBinding(String name, Class<?> type, String key, boolean ignored) {
    }
}
