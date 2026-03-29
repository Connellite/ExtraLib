package io.github.connellite.exception;

public class JdbcResultSetException extends RuntimeException {
    public JdbcResultSetException(String message) {
        super(message);
    }

    public JdbcResultSetException(String message, Throwable cause) {
        super(message, cause);
    }

    public JdbcResultSetException(Throwable cause) {
        super(cause);
    }
    
}
