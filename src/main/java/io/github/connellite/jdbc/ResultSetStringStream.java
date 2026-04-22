package io.github.connellite.jdbc;

import lombok.experimental.UtilityClass;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Builds a sequential {@link Stream} over {@link ResultSetStringIterator} rows. Close the stream (e.g. try-with-resources) to close JDBC resources.
 */
@UtilityClass
public final class ResultSetStringStream {
    /**
     * @param connection connection used for the query; iterator closes statement/result set when the stream is closed
     * @param sql        query executed once; forward-only read-only cursor
     */
    public static Stream<Map<String, String>> stream(Connection connection, String sql) throws SQLException {
        ResultSetStringIterator it;
        try {
            it = new ResultSetStringIterator(connection, sql);
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
     */
    public static Stream<Map<String, String>> stream(ResultSet resultSet) throws SQLException {
        ResultSetStringIterator it;
        try {
            it = new ResultSetStringIterator(resultSet);
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
