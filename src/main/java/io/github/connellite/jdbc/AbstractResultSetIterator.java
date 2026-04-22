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
 * This type implements only {@link Iterator} (not {@link Iterable}) so APIs with overloads for both
 * (for example {@code org.jooq.lambda.Seq.seq}) remain unambiguous when passed this instance.
 * For enhanced for-loops use {@link #asIterable()}; it is single-pass and delegates to this iterator.
 * <p>
 * Closing releases the result set and statement from construction.
 *
 * @param <V> map value type
 * @see ResultSetIterator
 * @see ResultSetStringIterator
 */
public abstract class AbstractResultSetIterator<V> implements Iterator<Map<String, V>>, AutoCloseable {

    protected final ResultSet resultSet;
    protected final List<String> columnNames;
    protected boolean hasNextValue;

    /**
     * Executes {@code query} on {@code conn} and prepares forward-only iteration over rows.
     */
    public AbstractResultSetIterator(Connection conn, String query) throws SQLException {
        Statement statement;
        // Prefer forward-only read-only cursors; fall back to default statement if the driver rejects that type/concurrency.
        try {
            statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        } catch (Exception e) {
            statement = conn.createStatement();
        }

        // Ignore if the driver does not support fetch size hints for this statement type.
        try {
            statement.setFetchSize(1000);
        } catch (Exception ignore) {
        }
        this.resultSet = new ResultSetWrapper(statement, statement.executeQuery(query));
        ResultSetMetaData metadata = resultSet.getMetaData();
        this.columnNames = getColumnNames(metadata);
        this.hasNextValue = resultSet.next();
    }

    /**
     * Wraps an already opened {@link ResultSet} and prepares iteration over current/remaining rows.
     */
    public AbstractResultSetIterator(ResultSet resultSet) throws SQLException {
        this.resultSet = resultSet;
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
     * Single-pass {@link Iterable} view for enhanced for-loops. {@link Iterable#iterator()} returns
     * {@code this}; one traversal consumes the {@link ResultSet}.
     */
    public Iterable<Map<String, V>> asIterable() {
        return () -> AbstractResultSetIterator.this;
    }

    @Override
    public boolean hasNext() {
        return hasNextValue;
    }

    @Override
    public abstract Map<String, V> next();

    /**
     * Closes underlying {@link ResultSet}.
     */
    @Override
    public void close() throws Exception {
        if (resultSet != null) resultSet.close();
    }
}
