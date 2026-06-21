package io.github.connellite.exception;

import io.github.connellite.container.ServiceRegistry;

/**
 * Thrown when a bean type is resolved from {@link ServiceRegistry}
 * but no implementation has been registered yet.
 */
public final class ServiceNotRegisteredException extends RuntimeException {

    public ServiceNotRegisteredException(Class<?> type) {
        super("Bean not registered: " + type.getName());
    }

    public ServiceNotRegisteredException(String message) {
        super(message);
    }
}
