package io.github.connellite.reflection;

import io.github.connellite.collections.ConcurrentReferenceHashMap;
import lombok.experimental.UtilityClass;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.util.Locale;
import java.util.concurrent.ConcurrentMap;

/**
 * Resolves serializable getter/setter method references to JavaBean property names.
 */
@UtilityClass
public class LambdaPropertyUtil {

    private static final ConcurrentMap<Class<?>, SerializedLambda> CACHE =
            new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);

    /**
     * Extracts {@link SerializedLambda} via {@code writeReplace}. Cached weakly by lambda class.
     */
    public static SerializedLambda serializedLambda(Serializable lambda) {
        return CACHE.computeIfAbsent(lambda.getClass(), ignored -> extract(lambda));
    }

    /**
     * Strips {@code get}/{@code set}/{@code is} and decapitalizes, e.g. {@code getName} → {@code name}.
     *
     * @throws IllegalArgumentException if {@code accessorName} has no JavaBean prefix
     */
    public static String methodToProperty(String accessorName) {
        String name = accessorName;
        if (name.startsWith("is")) {
            name = name.substring(2);
        } else if (name.startsWith("get") || name.startsWith("set")) {
            name = name.substring(3);
        } else {
            throw new IllegalArgumentException(
                    "Error parsing property name '" + accessorName + "'. Didn't start with 'is', 'get' or 'set'.");
        }
        if (name.length() == 1 || name.length() > 1 && !Character.isUpperCase(name.charAt(1))) {
            name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
        }
        return name;
    }

    /**
     * Property name for a serializable getter or setter method reference.
     */
    public static String propertyName(Serializable getter) {
        return methodToProperty(serializedLambda(getter).getImplMethodName());
    }

    private static SerializedLambda extract(Serializable lambda) {
        try {
            return (SerializedLambda) ReflectionUtil.invoke(lambda, "writeReplace");
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalArgumentException("Cannot inspect field method reference", e);
        }
    }
}
