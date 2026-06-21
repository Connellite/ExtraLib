package io.github.connellite.container;

import io.github.connellite.container.internal.LazyServiceProxy;
import io.github.connellite.exception.ServiceNotRegisteredException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Dynamic registry of application services. The global singleton is available via
 * {@link #getInstance()}; isolated registries can be created with {@link #create()}.
 * <p>
 * For an interface type, {@link #get(Class)} returns a lazy proxy that resolves the current
 * registration on every method call. The proxy can be obtained before {@link #register(Class, Object)};
 * resolution fails at first use with {@link ServiceNotRegisteredException} if nothing is registered yet.
 * A later {@code register} makes subsequent invocations succeed; replacing a registration is visible
 * on the next call (OSGi-style dynamic lookup).
 * <p>
 * For a concrete class, {@code get} looks up the registered instance immediately and throws
 * {@link ServiceNotRegisteredException} when the type is missing.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServiceRegistry {

    private static final class Holder {
        private static final ServiceRegistry INSTANCE = new ServiceRegistry();
    }

    private final ConcurrentMap<Class<?>, Object> beans = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, Object> lazyProxies = new ConcurrentHashMap<>();

    public static ServiceRegistry getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Creates a new isolated registry that does not share state with {@link #getInstance()}.
     */
    public static ServiceRegistry create() {
        return new ServiceRegistry();
    }

    /**
     * Registers {@code instance} under {@code type}. Replaces any previous registration for the same type.
     *
     * @throws IllegalArgumentException if {@code instance} is not an instance of {@code type}
     */
    public <T> void register(Class<T> type, T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        requireInstanceOf(type, instance);
        beans.put(type, instance);
    }

    /**
     * Registers {@code instance} only when {@code type} is not registered yet.
     *
     * @return {@code true} if registered, {@code false} if {@code type} was already present
     * @throws IllegalArgumentException if {@code instance} is not an instance of {@code type}
     */
    public <T> boolean registerIfAbsent(Class<T> type, T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        requireInstanceOf(type, instance);
        return beans.putIfAbsent(type, instance) == null;
    }

    /**
     * Removes the registration for {@code type}, if any.
     *
     * @return the previous instance, or {@code null} if none was registered
     */
    @SuppressWarnings("unchecked")
    public <T> T unregister(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return (T) beans.remove(type);
    }

    /**
     * Returns a lazy proxy for an interface type, or the registered instance for a concrete type.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (type.isInterface()) {
            return (T) lazyProxies.computeIfAbsent(type, key -> LazyServiceProxy.create(type, this::resolve));
        }
        return resolve(type);
    }

    public boolean contains(Class<?> type) {
        Objects.requireNonNull(type, "type");
        return beans.containsKey(type);
    }

    /**
     * Removes all registrations and cached lazy proxies in this registry.
     */
    public void clear() {
        beans.clear();
        lazyProxies.clear();
    }

    private <T> T resolve(Class<T> type) {
        Object bean = beans.get(type);
        if (bean == null) {
            throw new ServiceNotRegisteredException(type);
        }
        return type.cast(bean);
    }

    private static <T> void requireInstanceOf(Class<T> type, T instance) {
        if (!type.isInstance(instance)) {
            throw new IllegalArgumentException("Instance of type " + instance.getClass().getName() + " is not assignable to " + type.getName());
        }
    }
}
