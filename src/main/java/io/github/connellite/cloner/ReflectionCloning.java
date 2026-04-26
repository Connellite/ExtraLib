package io.github.connellite.cloner;

import io.github.connellite.exception.CloningException;
import io.github.connellite.reflection.ReflectionUtil;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Deep / shallow cloning via reflection. Instances are created with the no-arg constructor
 * (see {@link ReflectionUtil#getInstance(Class)}); classes without one cannot be cloned this way.
 * Common immutable JDK types (including {@link String}) are not traversed: the same instance is reused.
 */
@UtilityClass
public class ReflectionCloning {

    private static final Set<Class<?>> IGNORED_CLASSES = new HashSet<>();

    static {
        IGNORED_CLASSES.add(Integer.class);
        IGNORED_CLASSES.add(Long.class);
        IGNORED_CLASSES.add(Boolean.class);
        IGNORED_CLASSES.add(Class.class);
        IGNORED_CLASSES.add(Float.class);
        IGNORED_CLASSES.add(Double.class);
        IGNORED_CLASSES.add(Character.class);
        IGNORED_CLASSES.add(Byte.class);
        IGNORED_CLASSES.add(Short.class);
        IGNORED_CLASSES.add(String.class);
        IGNORED_CLASSES.add(Void.class);
        IGNORED_CLASSES.add(BigDecimal.class);
        IGNORED_CLASSES.add(BigInteger.class);
        IGNORED_CLASSES.add(URI.class);
        IGNORED_CLASSES.add(URL.class);
        IGNORED_CLASSES.add(UUID.class);
        IGNORED_CLASSES.add(Pattern.class);
    }

    /**
     * Deep clone: copies the object graph; shared references become shared in the clone unless broken by cycle handling.
     */
    public static <T> T clone(T original) {
        if (original == null) {
            return null;
        }
        Map<Object, Object> clones = new IdentityHashMap<>();
        try {
            return cloneGraph(original, clones);
        } catch (IllegalAccessException | IllegalStateException e) {
            throw new CloningException("Error during cloning of " + original, e);
        }
    }

    /**
     * Shallow clone: new outer instance; field values are the same references as in the original.
     */
    public static <T> T shallowClone(T original) {
        if (original == null) {
            return null;
        }
        try {
            return cloneGraph(original);
        } catch (IllegalAccessException | IllegalStateException e) {
            throw new CloningException("Error during cloning of " + original, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cloneGraph(T original, Map<Object, Object> clones) throws IllegalAccessException {
        if (original == null) {
            return null;
        }

        Class<T> clz = (Class<T>) original.getClass();

        if (clz.isEnum() || IGNORED_CLASSES.contains(clz)) {
            return original;
        }

        if (clones.containsKey(original)) {
            return (T) clones.get(original);
        }

        if (clz.isArray()) {
            int length = Array.getLength(original);
            T newArray = (T) Array.newInstance(clz.getComponentType(), length);
            clones.put(original, newArray);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(original, i);
                Array.set(newArray, i, cloneGraph(element, clones));
            }
            return newArray;
        }

        T newInstance = newInstance(clz);
        clones.put(original, newInstance);

        for (Field field : allFields(clz)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Object value = ReflectionUtil.getValueField(original, field);
            ReflectionUtil.setValueField(newInstance, field, cloneGraph(value, clones));
        }

        return newInstance;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cloneGraph(T original) throws IllegalAccessException {
        if (original == null) {
            return null;
        }

        Class<T> clz = (Class<T>) original.getClass();

        if (clz.isEnum() || IGNORED_CLASSES.contains(clz)) {
            return original;
        }

        if (clz.isArray()) {
            int length = Array.getLength(original);
            T newArray = (T) Array.newInstance(clz.getComponentType(), length);
            for (int i = 0; i < length; i++) {
                Array.set(newArray, i, Array.get(original, i));
            }
            return newArray;
        }

        T newInstance = newInstance(clz);

        for (Field field : allFields(clz)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Object value = ReflectionUtil.getValueField(original, field);
            ReflectionUtil.setValueField(newInstance, field, value);
        }

        return newInstance;
    }

    private static <T> T newInstance(Class<T> c) {
        try {
            return ReflectionUtil.getInstance(c);
        } catch (NoSuchMethodException e) {
            throw new CloningException("No accessible no-arg constructor for " + c.getName(), e);
        } catch (ReflectiveOperationException e) {
            throw new CloningException("Cannot instantiate " + c.getName(), e);
        }
    }

    private static List<Field> allFields(Class<?> c) {
        List<Field> list = new LinkedList<>();
        Collections.addAll(list, c.getDeclaredFields());
        Class<?> sc = c;
        while ((sc = sc.getSuperclass()) != Object.class && sc != null) {
            Collections.addAll(list, sc.getDeclaredFields());
        }
        return list;
    }
}
