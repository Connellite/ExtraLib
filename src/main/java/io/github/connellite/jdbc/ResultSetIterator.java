package io.github.connellite.jdbc;

import io.github.connellite.exception.ResultSetException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Forward-only iterator over JDBC query rows.
 * Use {@link #asIterable()} for {@code for (Map<String, Object> row : ...)} together with try-with-resources;
 * the cursor is single-pass, so a second loop on the same instance will not replay rows from the start.
 * <p>
 * Closing releases the result set and statement from construction.
 */
public class ResultSetIterator extends AbstractResultSetIterator<Object> {

    public ResultSetIterator(Connection conn, String query) throws SQLException {
        super(conn, query);
    }

    public ResultSetIterator(ResultSet resultSet) throws SQLException {
        super(resultSet);
    }

    public static List<Map<String, Object>> findAll(Connection conn, String query) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        try (ResultSetIterator it = new ResultSetIterator(conn, query)) {
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

    public static Optional<Map<String, Object>> findFirst(Connection conn, String query) throws SQLException {
        try (ResultSetIterator it = new ResultSetIterator(conn, query)) {
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

    public static List<Map<String, Object>> findAll(ResultSet resultSet) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        try (ResultSetIterator it = new ResultSetIterator(resultSet)) {
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

    public static Optional<Map<String, Object>> findFirst(ResultSet resultSet) throws SQLException {
        try (ResultSetIterator it = new ResultSetIterator(resultSet)) {
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
     * Returns the current row as a column-to-value map and advances the cursor.
     *
     * @return ordered map of column name to value for the current row
     * @throws NoSuchElementException if no more rows are available
     * @throws ResultSetException if row extraction fails
     */
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
            throw new ResultSetException(e);
        }

        return Collections.unmodifiableMap(row);
    }
}
