package io.github.connellite.jdbc;

import io.github.connellite.exception.ResultSetException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Forward-only iterator over JDBC query rows.
 * Use {@link #asIterable()} for {@code for (Map<String, String> row : ...)} together with try-with-resources;
 * the cursor is single-pass, so a second loop on the same instance will not replay rows from the start.
 * <p>
 * Closing releases the result set and statement from construction.
 */
public class ResultSetStringIterator extends AbstractResultSetIterator<String> {

    /**
     * Executes {@code query} and iterates resulting rows as {@code Map<String, String>}.
     */
    public ResultSetStringIterator(Connection conn, String query) throws SQLException {
        super(conn, query);
    }

    /**
     * Wraps an already opened {@link ResultSet}.
     */
    public ResultSetStringIterator(ResultSet resultSet) throws SQLException {
        super(resultSet);
    }

    /**
     * Reads all rows into an immutable list.
     */
    public static List<Map<String, String>> findAll(Connection conn, String query) throws SQLException {
        List<Map<String, String>> out = new ArrayList<>();
        try (ResultSetStringIterator it = new ResultSetStringIterator(conn, query)) {
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
     * Reads the first row, if present.
     */
    public static Optional<Map<String, String>> findFirst(Connection conn, String query) throws SQLException {
        try (ResultSetStringIterator it = new ResultSetStringIterator(conn, query)) {
            if (it.hasNext()) {
                return Optional.of(it.next());
            }
            return Optional.empty();
        } catch (SQLException se) {
            throw se;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    /**
     * Reads all rows from an existing {@link ResultSet} into an immutable list.
     */
    public static List<Map<String, String>> findAll(ResultSet resultSet) throws SQLException {
        List<Map<String, String>> out = new ArrayList<>();
        try (ResultSetStringIterator it = new ResultSetStringIterator(resultSet)) {
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
     * Reads the first row from an existing {@link ResultSet}, if present.
     */
    public static Optional<Map<String, String>> findFirst(ResultSet resultSet) throws SQLException {
        try (ResultSetStringIterator it = new ResultSetStringIterator(resultSet)) {
            if (it.hasNext()) {
                return Optional.of(it.next());
            }
            return Optional.empty();
        } catch (SQLException se) {
            throw se;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    /**
     * Returns the current row as a column-to-value map (stringified values) and advances cursor.
     */
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
