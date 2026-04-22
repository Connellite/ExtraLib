package io.github.connellite.jdbc;

import lombok.experimental.UtilityClass;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Builds a sequential {@link Stream} over {@link ResultSetBeanIterator} rows.
 * Close the stream (e.g. try-with-resources) to close JDBC resources.
 */
@UtilityClass
public final class ResultSetBeanStream {

    /**
     * @param connection connection used for query execution
     * @param sql        query executed once; forward-only read-only cursor
     * @param beanClass  target bean type
     */
    public static <T> Stream<T> stream(Connection connection, String sql, Class<T> beanClass) throws SQLException {
        ResultSetBeanIterator<T> it;
        try {
            it = new ResultSetBeanIterator<>(connection, sql, beanClass);
        } catch (SQLException se) {
            throw se;
        } catch (Exception e) {
            throw new SQLException(e);
        }
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED | Spliterator.NONNULL),
                        false)
                .onClose(() -> {
                    try {
                        it.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * @param resultSet open result set; stream close closes this result set
     * @param beanClass target bean type
     */
    public static <T> Stream<T> stream(ResultSet resultSet, Class<T> beanClass) throws SQLException {
        ResultSetBeanIterator<T> it;
        try {
            it = new ResultSetBeanIterator<>(resultSet, beanClass);
        } catch (SQLException se) {
            throw se;
        } catch (Exception e) {
            throw new SQLException(e);
        }
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED | Spliterator.NONNULL),
                        false)
                .onClose(() -> {
                    try {
                        it.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
