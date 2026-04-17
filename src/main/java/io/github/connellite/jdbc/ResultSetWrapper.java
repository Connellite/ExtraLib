package io.github.connellite.jdbc;

import lombok.experimental.Delegate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class ResultSetWrapper implements ResultSet {

    @Delegate(types = ResultSet.class)
    private final ResultSet delegate;
    private final Statement statement;

    ResultSetWrapper(Statement statement, ResultSet delegate) {
        this.statement = statement;
        this.delegate = delegate;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (iface != null && iface.isInstance(this)) {
            return true;
        }
        return delegate.isWrapperFor(iface);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface == null) {
            throw new SQLException("iface must not be null");
        }
        if (iface.isInstance(this)) {
            try {
                return iface.cast(this);
            } catch (ClassCastException e) {
                throw new SQLException("Unable to unwrap to " + iface.getName(), e);
            }
        }
        return delegate.unwrap(iface);
    }

    @Override
    public void close() throws SQLException {
        SQLException closeException = null;
        try {
            if (delegate != null) delegate.close();
        } catch (SQLException e) {
            closeException = e;
        }
        try {
            if(statement != null) statement.close();
        } catch (SQLException e) {
            if (closeException == null) {
                closeException = e;
            } else {
                closeException.addSuppressed(e);
            }
        }
        if (closeException != null) {
            throw closeException;
        }
    }
}