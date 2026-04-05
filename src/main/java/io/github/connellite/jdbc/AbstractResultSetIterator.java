package io.github.connellite.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Forward-only iterator over JDBC query rows. Subclasses map the current {@link ResultSet} row.
 * Implements {@link Iterable} so you can use {@code for (Map<String, V> row : it)} with try-with-resources.
 * {@link #iterator()} returns {@code this}; the cursor is single-pass.
 * <p>
 * Closing releases the result set and statement from construction.
 *
 * @param <V> map value type
 * @see ResultSetIterator
 * @see ResultSetStringIterator
 */
public abstract class AbstractResultSetIterator<V> implements Iterator<Map<String, V>>, Iterable<Map<String, V>>, AutoCloseable {

    private Statement statement;
    protected final ResultSet resultSet;
    protected final List<String> columnNames;
    protected boolean hasNextValue;

    public AbstractResultSetIterator(Connection conn, String query) throws SQLException {
        // Prefer forward-only read-only cursors; fall back to default statement if the driver rejects that type/concurrency.
        try {
            this.statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        } catch (Exception e) {
            this.statement = conn.createStatement();
        }

        // Ignore if the driver does not support fetch size hints for this statement type.
        try {
            this.statement.setFetchSize(1000);
        } catch (SQLException ignore) {
        }
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
    public Iterator<Map<String, V>> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return hasNextValue;
    }

    @Override
    public abstract Map<String, V> next();

    @Override
    public void close() throws Exception {
        if (resultSet != null) resultSet.close();
        if (statement != null) statement.close();
    }
}
