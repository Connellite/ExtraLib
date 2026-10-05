package io.github.connellite.reflection;

import lombok.experimental.UtilityClass;

/**
 * Creates instances without calling a constructor when Objenesis is present,
 * otherwise via {@link ReflectionUtil#getInstance(Class)}.
 */
@UtilityClass
public class InstanceFactory {

    static final boolean USE_OBJENESIS;

    static {
        boolean useObjenesis;
        try {
            Class.forName("org.objenesis.ObjenesisStd");
            useObjenesis = true;
        } catch (Exception e) {
            useObjenesis = false;
        }
        USE_OBJENESIS = useObjenesis;
    }

    /**
     * Creates an instance of {@code type}. With Objenesis the constructor is not called.
     * Without it, an accessible no-arg constructor is required.
     */
    public static <T> T newInstance(Class<T> type) {
        if (USE_OBJENESIS) {
            return ObjenesisInstantiator.newInstance(type);
        }
        return ReflectionInstantiator.newInstance(type);
    }

    private static final class ReflectionInstantiator {

        private static <T> T newInstance(Class<T> type) {
            try {
                return ReflectionUtil.getInstance(type);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "No accessible no-arg constructor for " + type.getName() + ". Provide a no-arg constructor or add the Objenesis library",
                        e);
            }
        }
    }

    private static final class ObjenesisInstantiator {

        private static final org.objenesis.ObjenesisStd OBJENESIS = new org.objenesis.ObjenesisStd();

        private static <T> T newInstance(Class<T> type) {
            return OBJENESIS.newInstance(type);
        }
    }
}
