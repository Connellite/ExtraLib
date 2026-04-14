package io.github.connellite.exception;

public class ResultSetException extends RuntimeException {
    public ResultSetException(String message) {
        super(message);
    }

    public ResultSetException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultSetException(Throwable cause) {
        super(cause);
    }
    
}
