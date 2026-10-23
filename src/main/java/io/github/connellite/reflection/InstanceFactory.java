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

        try {
            return ReflectionUtil.getInstance(type);
        } catch (ReflectiveOperationException e) {
            String message = "No accessible no-arg constructor for " + type.getName() + ". Provide a no-arg constructor or add the Objenesis library";
            throw new IllegalStateException(message, e);
        }
    }

    private static final class ObjenesisInstantiator {

        // ObjenesisStd keys its cache by class name, so the same name loaded by another
        // class loader reuses the first class's instantiator.
        private static final org.objenesis.ObjenesisStd OBJENESIS = new org.objenesis.ObjenesisStd(false);

        private static <T> T newInstance(Class<T> type) {
            return OBJENESIS.newInstance(type);
        }
    }
}
