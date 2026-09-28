package io.github.connellite.container.internal;

import io.github.connellite.reflection.ReflectionUtil;
import lombok.experimental.UtilityClass;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

/**
 * JDK dynamic proxy that resolves the delegate from the registry on every method invocation.
 */
@UtilityClass
public final class LazyServiceProxy {

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> serviceType, Function<Class<T>, T> resolver) {
        if (!serviceType.isInterface()) {
            throw new IllegalArgumentException("Lazy proxy requires an interface: " + serviceType.getName());
        }
        ClassLoader classLoader = serviceType.getClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return (T) Proxy.newProxyInstance(
                classLoader,
                new Class<?>[]{serviceType},
                new LazyInvocationHandler<>(serviceType, resolver)
        );
    }

    private record LazyInvocationHandler<T>(Class<T> serviceType,
                                            Function<Class<T>, T> resolver) implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, method, args);
            }

            T resolved = resolver.apply(serviceType);
            if (!method.canAccess(resolved)) {
                method.setAccessible(true);
            }
            try {
                return method.invoke(resolved, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
            if (ReflectionUtil.isEqualsMethod(method)) {
                return proxy == args[0];
            }
            if (ReflectionUtil.isHashCodeMethod(method)) {
                return System.identityHashCode(proxy);
            }
            if (ReflectionUtil.isToStringMethod(method)) {
                return "LazyServiceProxy[" + serviceType.getName() + "]";
            }
            throw new UnsupportedOperationException("Unsupported Object method: " + method.getName());
        }
    }
}
