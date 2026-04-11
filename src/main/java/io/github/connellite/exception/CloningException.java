package io.github.connellite.exception;

public class CloningException extends RuntimeException {
    public CloningException(String message) {
        super(message);
    }

    public CloningException(String message, Throwable cause) {
        super(message, cause);
    }
}
