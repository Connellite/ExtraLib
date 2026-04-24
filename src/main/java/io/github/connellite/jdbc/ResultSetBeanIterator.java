package io.github.connellite.jdbc;

import io.github.connellite.exception.ResultSetException;
import lombok.Getter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Forward-only iterator over JDBC query rows mapped to a simple POJO via {@link SimpleResultSetBeanMapper}.
 * Column labels are validated against the result set metadata using {@link ResultSetMetaDataUtils}.
 * <p>
 * Use {@link #asIterable()} for enhanced for-loops together with try-with-resources; the cursor is
 * single-pass.
 * <p>
 * Closing releases the result set and statement from construction.
 *
 * @param <T> bean type
 * @see SimpleResultSetBeanMapper
 */
public class ResultSetBeanIterator<T> implements Iterator<T>, AutoCloseable {

    private final ResultSet resultSet;
    @Getter
    private final SimpleResultSetBeanMapper<T> mapper;
    private boolean hasNextValue;

    /**
     * Executes {@code query} and iterates rows mapped to {@code beanClass}.
     */
    public ResultSetBeanIterator(Class<T> beanClass, Connection conn, String query, Object... params) throws SQLException {
        Object[] safeParams = params == null ? new Object[0] : params;
        PreparedStatement statement;
        try {
            statement = conn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        } catch (Exception e) {
            // Prefer forward-only read-only cursors; fall back to default statement if the driver rejects that type/concurrency.
            statement = conn.prepareStatement(query);
        }

        try {
            statement.setFetchSize(1000);
        } catch (Exception ignore) {// Ignore if the driver does not support fetch size hints for this statement type.
        }
        for (int i = 0; i < safeParams.length; i++) {
            statement.setObject(i + 1, safeParams[i]);
        }
        this.resultSet = new ResultSetWrapper(statement, statement.executeQuery());
        this.mapper = new SimpleResultSetBeanMapper<>(beanClass, ResultSetMetaDataUtils.getColumnLabels(resultSet));
        this.hasNextValue = resultSet.next();
    }

    /**
     * Executes {@code query} and iterates rows using provided mapper.
     */
    public ResultSetBeanIterator(SimpleResultSetBeanMapper<T> mapper, Connection conn, String query, Object... params) throws SQLException {
        Object[] safeParams = params == null ? new Object[0] : params;
        PreparedStatement statement;
        try {
            statement = conn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        } catch (Exception e) {
            // Prefer forward-only read-only cursors; fall back to default statement if the driver rejects that type/concurrency.
            statement = conn.prepareStatement(query);
        }

        try {
            statement.setFetchSize(1000);
        } catch (Exception ignore) {// Ignore if the driver does not support fetch size hints for this statement type.
        }
        for (int i = 0; i < safeParams.length; i++) {
            statement.setObject(i + 1, safeParams[i]);
        }
        this.resultSet = new ResultSetWrapper(statement, statement.executeQuery());
        this.mapper = mapper;
        this.hasNextValue = resultSet.next();
    }

    /**
     * Wraps an already opened {@link ResultSet} and maps rows to {@code beanClass}.
     */
    public ResultSetBeanIterator(ResultSet resultSet, Class<T> beanClass) throws SQLException {
        this.resultSet = resultSet;
        this.mapper = new SimpleResultSetBeanMapper<>(beanClass, ResultSetMetaDataUtils.getColumnLabels(resultSet));
        this.hasNextValue = resultSet.next();
    }

    /**
     * Wraps an already opened {@link ResultSet} and maps rows using provided mapper.
     */
    public ResultSetBeanIterator(ResultSet resultSet, SimpleResultSetBeanMapper<T> mapper) throws SQLException {
        this.resultSet = resultSet;
        this.mapper = mapper;
        this.hasNextValue = resultSet.next();
    }

    /**
     * Registers custom type converter in the underlying mapper and returns this iterator.
     */
    public <C> ResultSetBeanIterator<T> withConverter(Class<C> type, TypeConverter<C> converter) {
        mapper.withConverter(type, converter);
        return this;
    }

    /**
     * Single-pass {@link Iterable} view for enhanced for-loops. {@link Iterable#iterator()} returns
     * {@code this}; one traversal consumes the {@link ResultSet}.
     */
    public Iterable<T> asIterable() {
        return () -> ResultSetBeanIterator.this;
    }

    @Override
    public boolean hasNext() {
        return hasNextValue;
    }

    /**
     * Returns the current mapped bean and advances cursor.
     */
    @Override
    public T next() {
        if (!hasNextValue) {
            throw new NoSuchElementException();
        }
        try {
            T row = mapper.mapRow(resultSet);
            hasNextValue = resultSet.next();
            return row;
        } catch (Exception e) {
            hasNextValue = false;
            throw new ResultSetException(e);
        }
    }

    /**
     * Closes underlying {@link ResultSet}.
     */
    @Override
    public void close() throws Exception {
        if (resultSet != null) {
            resultSet.close();
        }
    }

    /**
     * Reads all mapped rows into an immutable list.
     */
    public static <T> List<T> getAll(Class<T> beanClass, Connection conn, String query, Object... params) throws SQLException {
        List<T> out = new ArrayList<>();
        try (ResultSetBeanIterator<T> it = new ResultSetBeanIterator<>(beanClass, conn, query, params)) {
            while (it.hasNext()) {
                out.add(it.next());
            }
        } catch (SQLException se) {
            throw se;
        } catch (Exception e) {
            throw new SQLException(e);
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Reads all mapped rows into an immutable list using provided mapper.
     */
    public static <T> List<T> getAll(SimpleResultSetBeanMapper<T> mapper, Connection conn, String query, Object... params) throws SQLException {
        List<T> out = new ArrayList<>();
        try (ResultSetBeanIterator<T> it = new ResultSetBeanIterator<>(mapper, conn, query, params)) {
            while (it.hasNext()) {
                out.add(it.next());
            }
        } catch (SQLException se) {
            throw se;
        } catch (Exception e) {
            throw new SQLException(e);
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Reads the first mapped row, if present.
     */
    public static <T> Optional<T> getFirst(Class<T> beanClass, Connection conn, String query, Object... params) throws SQLException {
        try (ResultSetBeanIterator<T> it = new ResultSetBeanIterator<>(beanClass, conn, query, params)) {
            if (it.hasNext()) {
                return Optional.ofNullable(it.next());
            }
            return Optional.empty();
        } catch (SQLException se) {
            throw se;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    /**
     * Reads the first mapped row, if present, using provided mapper.
     */
    public static <T> Optional<T> getFirst(SimpleResultSetBeanMapper<T> mapper, Connection conn, String query, Object... params) throws SQLException {
        try (ResultSetBeanIterator<T> it = new ResultSetBeanIterator<>(mapper, conn, query, params)) {
            if (it.hasNext()) {
                return Optional.ofNullable(it.next());
            }
            return Optional.empty();
        } catch (SQLException se) {
            throw se;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    /**
     * Reads all mapped rows from an existing {@link ResultSet} into an immutable list.
     */
    public static <T> List<T> getAll(ResultSet resultSet, Class<T> beanClass) throws SQLException {
        List<T> out = new ArrayList<>();
        try (ResultSetBeanIterator<T> it = new ResultSetBeanIterator<>(resultSet, beanClass)) {
            while (it.hasNext()) {
                out.add(it.next());
            }
        } catch (SQLException se) {
            throw se;
        } catch (Exception e) {
            throw new SQLException(e);
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Reads all mapped rows from an existing {@link ResultSet} into an immutable list using provided mapper.
     */
    public static <T> List<T> getAll(ResultSet resultSet, SimpleResultSetBeanMapper<T> mapper) throws SQLException {
        List<T> out = new ArrayList<>();
        try (ResultSetBeanIterator<T> it = new ResultSetBeanIterator<>(resultSet, mapper)) {
            while (it.hasNext()) {
                out.add(it.next());
            }
        } catch (SQLException se) {
            throw se;
        } catch (Exception e) {
            throw new SQLException(e);
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Reads the first mapped row from an existing {@link ResultSet}, if present.
     */
    public static <T> Optional<T> getFirst(ResultSet resultSet, Class<T> beanClass) throws SQLException {
        try (ResultSetBeanIterator<T> it = new ResultSetBeanIterator<>(resultSet, beanClass)) {
            if (it.hasNext()) {
                return Optional.ofNullable(it.next());
            }
            return Optional.empty();
        } catch (SQLException se) {
            throw se;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    /**
     * Reads the first mapped row from an existing {@link ResultSet}, if present, using provided mapper.
     */
    public static <T> Optional<T> getFirst(ResultSet resultSet, SimpleResultSetBeanMapper<T> mapper) throws SQLException {
        try (ResultSetBeanIterator<T> it = new ResultSetBeanIterator<>(resultSet, mapper)) {
            if (it.hasNext()) {
                return Optional.ofNullable(it.next());
            }
            return Optional.empty();
        } catch (SQLException se) {
            throw se;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }
}
