package io.github.connellite.jdbc;

import lombok.experimental.UtilityClass;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Executes parameterized SQL queries via {@link PreparedStatement}.
 */
@UtilityClass
public class QueryUtils {

    /**
     * Executes a SELECT query and returns detached rows.
     */
    public static ResultSet selectQuery(Connection connection, String query, Object... params) throws SQLException {
        Object[] safeParams = params == null ? new Object[0] : params;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (int i = 0; i < safeParams.length; i++) {
                statement.setObject(i + 1, safeParams[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                CachedRowSet cachedRowSet = RowSetProvider.newFactory().createCachedRowSet();
                cachedRowSet.populate(rs);
                return cachedRowSet;
            }
        }
    }

    /**
     * Executes a non-SELECT query.
     */
    public static boolean executeQuery(Connection connection, String query, Object... params) throws SQLException {
        Object[] safeParams = params == null ? new Object[0] : params;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (int i = 0; i < safeParams.length; i++) {
                statement.setObject(i + 1, safeParams[i]);
            }
            return statement.execute();
        }
    }

    /**
     * Runs the same prepared statement for many parameter sets (batch). Returns update counts per batch entry; order matches {@code batchParams}.
     */
    public static int[] executeBatch(Connection connection, String sql, Object[][] batchParams) throws SQLException {
        Object[][] rows = batchParams == null ? new Object[0][] : batchParams;
        if (rows.length == 0) {
            return new int[0];
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Object[] params : rows) {
                Object[] safeParams = params == null ? new Object[0] : params;
                for (int i = 0; i < safeParams.length; i++) {
                    statement.setObject(i + 1, safeParams[i]);
                }
                statement.addBatch();
            }
            return statement.executeBatch();
        }
    }

    /**
     * Executes a stored procedure or function call with IN parameters only; returns the same meaning as {@link CallableStatement#execute()}.
     */
    public static boolean executeCall(Connection connection, String call, Object... inParams) throws SQLException {
        Object[] safeParams = inParams == null ? new Object[0] : inParams;
        try (CallableStatement statement = connection.prepareCall(call)) {
            for (int i = 0; i < safeParams.length; i++) {
                statement.setObject(i + 1, safeParams[i]);
            }
            return statement.execute();
        }
    }

    /**
     * Executes a call that returns a result set (first non-null {@link ResultSet} from {@link CallableStatement#execute()} / {@link CallableStatement#getMoreResults()}),
     * detached as a {@link CachedRowSet}. IN parameters only.
     */
    public static ResultSet selectFromCall(Connection connection, String call, Object... inParams) throws SQLException {
        Object[] safeParams = inParams == null ? new Object[0] : inParams;
        try (CallableStatement statement = connection.prepareCall(call)) {
            for (int i = 0; i < safeParams.length; i++) {
                statement.setObject(i + 1, safeParams[i]);
            }
            boolean hasResult = statement.execute();
            for (;;) {
                if (hasResult) {
                    try (ResultSet rs = statement.getResultSet()) {
                        if (rs != null) {
                            CachedRowSet cachedRowSet = RowSetProvider.newFactory().createCachedRowSet();
                            cachedRowSet.populate(rs);
                            return cachedRowSet;
                        }
                    }
                } else if (statement.getUpdateCount() == -1) {
                    break;
                }
                hasResult = statement.getMoreResults();
            }
            throw new SQLException("CallableStatement returned no ResultSet");
        }
    }
}
