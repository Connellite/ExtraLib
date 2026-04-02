package io.github.connellite.jdbc;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Builds a sequential {@link Stream} over {@link ResultSetStringIterator} rows. Close the stream (e.g. try-with-resources) to close JDBC resources.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResultSetStringStream {
    /**
     * @param connection connection used for the query; iterator closes statement/result set when the stream is closed
     * @param sql        query executed once; forward-only read-only cursor
     */
    public static Stream<Map<String, String>> stream(Connection connection, String sql) throws SQLException {
        ResultSetStringIterator it;
        try {
            it = new ResultSetStringIterator(connection, sql);
        } catch (Exception e) {
            if (e instanceof SQLException se) {
                throw se;
            }
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
