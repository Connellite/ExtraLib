package io.github.connellite.exception;

/**
 * Thrown when a value cannot be coerced to the requested target type.
 */
public final class TypeCoercionException extends RuntimeException {

    public TypeCoercionException(String message) {
        super(message);
    }

    public TypeCoercionException(String message, Throwable cause) {
        super(message, cause);
    }
}
