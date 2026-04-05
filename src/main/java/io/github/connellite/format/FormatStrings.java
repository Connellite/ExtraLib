package io.github.connellite.format;

import lombok.experimental.UtilityClass;

import java.util.Arrays;

/**
 * Default text form for values when no {@code String#format} conversion applies.
 */
@UtilityClass
final class FormatStrings {
    /**
     * Like {@link String#valueOf(Object)} but arrays render with {@link Arrays#toString} /
     * {@link Arrays::deepToString}, not {@code [I@…}.
     */
    static String defaultArgString(Object value) {
        if (value == null) {
            return "null";
        }
        Class<?> cl = value.getClass();
        if (!cl.isArray()) {
            return String.valueOf(value);
        }
        Class<?> comp = cl.getComponentType();
        if (!comp.isPrimitive()) {
            return Arrays.deepToString((Object[]) value);
        }
        if (comp == byte.class) {
            return Arrays.toString((byte[]) value);
        }
        if (comp == short.class) {
            return Arrays.toString((short[]) value);
        }
        if (comp == int.class) {
            return Arrays.toString((int[]) value);
        }
        if (comp == long.class) {
            return Arrays.toString((long[]) value);
        }
        if (comp == char.class) {
            return Arrays.toString((char[]) value);
        }
        if (comp == float.class) {
            return Arrays.toString((float[]) value);
        }
        if (comp == double.class) {
            return Arrays.toString((double[]) value);
        }
        if (comp == boolean.class) {
            return Arrays.toString((boolean[]) value);
        }
        return String.valueOf(value);
    }
}
