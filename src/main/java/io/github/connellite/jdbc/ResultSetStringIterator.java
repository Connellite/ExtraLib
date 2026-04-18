package io.github.connellite.jdbc;

import io.github.connellite.exception.ResultSetException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Forward-only iterator over JDBC query rows.
 * Use {@link #asIterable()} for {@code for (Map<String, String> row : ...)} together with try-with-resources;
 * the cursor is single-pass, so a second loop on the same instance will not replay rows from the start.
 * <p>
 * Closing releases the result set and statement from construction.
 */
public class ResultSetStringIterator extends AbstractResultSetIterator<String> {

    public ResultSetStringIterator(Connection conn, String query) throws SQLException {
        super(conn, query);
    }

    @Override
    public Map<String, String> next() {
        if (!hasNextValue) {
            throw new NoSuchElementException();
        }

        Map<String, String> row = new LinkedHashMap<>();
        try {
            for (String columnName : columnNames) {
                Object value = resultSet.getObject(columnName);
                if (value instanceof Number n) {
                    row.put(columnName, new BigDecimal(n.toString()).stripTrailingZeros().toPlainString());
                } else {
                    row.put(columnName, Objects.toString(value, null));
                }
            }
            hasNextValue = resultSet.next();
        } catch (Exception e) {
            hasNextValue = false;
            throw new ResultSetException(e);
        }

        return Collections.unmodifiableMap(row);
    }
}
