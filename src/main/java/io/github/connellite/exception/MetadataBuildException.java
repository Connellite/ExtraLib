package io.github.connellite.exception;

import java.sql.SQLException;

public final class MetadataBuildException extends RuntimeException {
    public MetadataBuildException(SQLException cause) {
        super(cause);
    }

    public SQLException sqlException() {
        return (SQLException) getCause();
    }
}