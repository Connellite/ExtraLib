package io.github.connellite.jdbc;

import io.github.connellite.exception.JdbcResultSetException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Forward-only iterator over JDBC query rows.
 * Implements {@link Iterable} so you can use {@code for (Map<String, Object> row : it)} together with
 * try-with-resources. {@link #iterator()} returns {@code this}; the cursor is single-pass, so a second
 * enhanced for-loop on the same instance will not replay rows from the start.
 * <p>
 * Closing releases the result set and statement from construction.
 */
public class ResultSetIterator implements Iterator<Map<String, Object>>, Iterable<Map<String, Object>>, AutoCloseable {

    private final Statement statement;
    private final ResultSet resultSet;
    private final List<String> columnNames;
    private boolean hasNextValue;

    ResultSetIterator(Connection conn, String query) throws Exception {
        this.statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        this.statement.setFetchSize(1000);
        this.resultSet = statement.executeQuery(query);
        ResultSetMetaData metadata = resultSet.getMetaData();
        this.columnNames = getColumnNames(metadata);
        this.hasNextValue = resultSet.next();
    }

    private List<String> getColumnNames(ResultSetMetaData metadata) throws SQLException {
        List<String> names = new ArrayList<>();
        int columnCount = metadata.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String label = metadata.getColumnLabel(i);
            if (label != null && !label.isBlank()) {
                names.add(label);
            } else {
                names.add(metadata.getColumnName(i));
            }
        }
        return names;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code this} as the only iterator; one traversal consumes the {@link ResultSet}.
     */
    @Override
    public Iterator<Map<String, Object>> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return hasNextValue;
    }

    @Override
    public Map<String, Object> next() {
        if (!hasNextValue) {
            throw new NoSuchElementException();
        }

        Map<String, Object> row = new LinkedHashMap<>();
        try {
            for (String columnName : columnNames) {
                Object value = resultSet.getObject(columnName);
                row.put(columnName, value);
            }
            hasNextValue = resultSet.next();
        } catch (Exception e) {
            hasNextValue = false;
            throw new JdbcResultSetException(e);
        }

        return row;
    }

    @Override
    public void close() throws Exception {
        try {
            if (resultSet != null) resultSet.close();
        } catch (Exception ignore) {
        }

        try {
            if (statement != null) statement.close();
        } catch (Exception ignore) {
        }
    }
}
