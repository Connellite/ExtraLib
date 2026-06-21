package io.github.connellite.exception;

public final class CloningException extends RuntimeException {
    public CloningException(String message) {
        super(message);
    }

    public CloningException(String message, Throwable cause) {
        super(message, cause);
    }
}
