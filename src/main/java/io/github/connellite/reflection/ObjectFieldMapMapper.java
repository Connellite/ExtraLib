package io.github.connellite.reflection;

import io.github.connellite.reflection.annotation.MapField;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps object fields into {@link LinkedHashMap}.
 */
@UtilityClass
public class ObjectFieldMapMapper {

    /**
     * Maps all non-static fields from class hierarchy to a linked hash map.
     * Parent class fields are emitted first.
     *
     * @param source source object
     * @return field map where key is field name or {@link MapField#key()}
     */
    public static Map<String, Object> map(@NonNull Object source) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        List<Class<?>> hierarchy = getHierarchy(source.getClass());
        for (Class<?> type : hierarchy) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                MapField mapField = field.getAnnotation(MapField.class);
                if (mapField != null && mapField.ignore()) {
                    continue;
                }

                String key = resolveKey(field, mapField);
                try {
                    Object value = ReflectionUtil.getValueField(source, field);
                    result.put(key, value);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Cannot read field " + field.getDeclaringClass().getName() + "#" + field.getName(), e);
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static String resolveKey(Field field, MapField mapField) {
        if (mapField == null || mapField.key().isBlank()) {
            return field.getName();
        }
        return mapField.key();
    }

    private static List<Class<?>> getHierarchy(Class<?> sourceType) {
        List<Class<?>> hierarchy = new ArrayList<>();
        for (Class<?> c = sourceType; c != null && c != Object.class; c = c.getSuperclass()) {
            hierarchy.add(c);
        }
        Collections.reverse(hierarchy);
        return hierarchy;
    }
}
