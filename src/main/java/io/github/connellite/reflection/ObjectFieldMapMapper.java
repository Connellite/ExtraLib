package io.github.connellite.reflection;

import io.github.connellite.reflection.annotation.MapField;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
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
        for (Field field : ReflectionUtil.getAllDeclaredFields(source.getClass())) {
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
                MapTypeConverter<?> converter = resolveAnnotationConverter(mapField);
                if (converter != null) {
                    value = convertValue(converter, value);
                }
                result.put(key, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot read field " + field.getDeclaringClass().getName() + "#" + field.getName(), e);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    @SuppressWarnings("unchecked")
    private static Object convertValue(MapTypeConverter<?> converter, Object value) {
        return ((MapTypeConverter<Object>) converter).convert(value);
    }

    private static MapTypeConverter<?> resolveAnnotationConverter(MapField mapField) {
        if (mapField == null || mapField.converter() == MapTypeConverter.DefaultConverter.class) {
            return null;
        }
        return InstanceFactory.newInstance(mapField.converter());
    }

    private static String resolveKey(Field field, MapField mapField) {
        if (mapField == null || mapField.key().isBlank()) {
            return field.getName();
        }
        return mapField.key();
    }
}
