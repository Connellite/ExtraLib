package io.github.connellite.jdbc;

import io.github.connellite.jdbc.internal.ResultSetWrapper;
import lombok.experimental.UtilityClass;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

/**
 * Executes parameterized SQL queries via {@link PreparedStatement}.
 */
@UtilityClass
public class QueryUtils {

    /**
     * Executes a SELECT query and returns a result set wrapper that closes both {@link java.sql.Statement} and {@link ResultSet}.
     */
    public static ResultSet selectQuery(Connection connection, String query, Object... params) throws SQLException {
        Object[] safeParams = params == null ? new Object[0] : params;
        PreparedStatement statement = connection.prepareStatement(query);
        try {
            for (int i = 0; i < safeParams.length; i++) {
                statement.setObject(i + 1, safeParams[i]);
            }
            ResultSet rs = statement.executeQuery();
            return new ResultSetWrapper(statement, rs);
        } catch (SQLException e) {
            statement.close();
            throw e;
        }
    }

    /**
     * Executes a SELECT query and returns detached rows as a {@link CachedRowSet}.
     * Unlike {@link #selectQuery(Connection, String, Object...)}, the returned result is independent from the
     * underlying JDBC {@link ResultSet} / {@link PreparedStatement}: both are closed before this method returns.
     * Not recommended for large result sets because all rows are materialized in memory.
     */
    public static ResultSet selectQueryCached(Connection connection, String query, Object... params) throws SQLException {
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
     * Executes a named SELECT ({@code :name} parameters) and returns a result set wrapper that closes both
     * {@link java.sql.Statement} and {@link ResultSet}.
     */
    public static ResultSet selectNamedQuery(Connection connection, String namedQuery, Map<String, Object> params) throws SQLException {
        return selectNamedQuery(connection, NamedQuery.of(namedQuery), params);
    }

    /**
     * Binds {@code params} onto {@code namedQuery} and executes it as a SELECT.
     * The returned wrapper closes both {@link java.sql.Statement} and {@link ResultSet}.
     */
    public static ResultSet selectNamedQuery(Connection connection, NamedQuery namedQuery, Map<String, Object> params) throws SQLException {
        NamedPreparedStatement statement = Objects.requireNonNull(namedQuery, "namedQuery").setAll(params).prepare(connection);
        try {
            ResultSet rs = statement.executeQuery();
            return new ResultSetWrapper(statement.unwrap(), rs);
        } catch (SQLException e) {
            statement.close();
            throw e;
        }
    }

    /**
     * Executes a named SELECT and returns detached rows as a {@link CachedRowSet}.
     * The JDBC {@link ResultSet} and {@link PreparedStatement} are closed before this method returns.
     */
    public static ResultSet selectNamedQueryCached(Connection connection, String namedQuery, Map<String, Object> params) throws SQLException {
        return selectNamedQueryCached(connection, NamedQuery.of(namedQuery), params);
    }

    /**
     * Binds {@code params} onto {@code namedQuery}, executes the SELECT, and returns a {@link CachedRowSet}.
     */
    public static ResultSet selectNamedQueryCached(Connection connection, NamedQuery namedQuery, Map<String, Object> params) throws SQLException {
        try (NamedPreparedStatement statement = Objects.requireNonNull(namedQuery, "namedQuery").setAll(params).prepare(connection);
             ResultSet rs = statement.executeQuery()) {
            CachedRowSet cachedRowSet = RowSetProvider.newFactory().createCachedRowSet();
            cachedRowSet.populate(rs);
            return cachedRowSet;
        }
    }

    /**
     * Executes a named non-SELECT query. Return value matches {@link PreparedStatement#execute()}.
     */
    public static boolean executeNamedQuery(Connection connection, String namedQuery, Map<String, Object> params) throws SQLException {
        return executeNamedQuery(connection, NamedQuery.of(namedQuery), params);
    }

    /**
     * Binds {@code params} onto {@code namedQuery} and executes it. Return value matches {@link PreparedStatement#execute()}.
     */
    public static boolean executeNamedQuery(Connection connection, NamedQuery namedQuery, Map<String, Object> params) throws SQLException {
        try (NamedPreparedStatement statement = Objects.requireNonNull(namedQuery, "namedQuery").setAll(params).prepare(connection)) {
            return statement.execute();
        }
    }

    /**
     * Runs {@code namedQuery} once as a JDBC batch with {@code params}. Returns the single update count from {@link PreparedStatement#executeBatch()}.
     */
    public static int[] executeNamedBatch(Connection connection, String namedQuery, Map<String, Object> params) throws SQLException {
        return executeNamedBatch(connection, NamedQuery.of(namedQuery), params);
    }

    /**
     * Binds {@code params} onto {@code namedQuery}, adds that parameter set as one batch entry, and executes the batch.
     */
    public static int[] executeNamedBatch(Connection connection, NamedQuery namedQuery, Map<String, Object> params) throws SQLException {
        try (NamedPreparedStatement statement = Objects.requireNonNull(namedQuery, "namedQuery").setAll(params).prepare(connection)) {
            statement.addBatch();
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
     * wrapped as a managed {@link ResultSet} that closes both resources. IN parameters only.
     */
    public static ResultSet selectFromCall(Connection connection, String call, Object... inParams) throws SQLException {
        Object[] safeParams = inParams == null ? new Object[0] : inParams;
        CallableStatement statement = connection.prepareCall(call);
        try {
            for (int i = 0; i < safeParams.length; i++) {
                statement.setObject(i + 1, safeParams[i]);
            }
            boolean hasResult = statement.execute();
            for (;;) {
                if (hasResult) {
                    ResultSet rs = statement.getResultSet();
                    if (rs != null) {
                        return new ResultSetWrapper(statement, rs);
                    }
                } else if (statement.getUpdateCount() == -1) {
                    break;
                }
                hasResult = statement.getMoreResults();
            }
            throw new SQLException("CallableStatement returned no ResultSet");
        } catch (SQLException e) {
            statement.close();
            throw e;
        }
    }

    /**
     * Executes a call that returns a result set (first non-null {@link ResultSet} from {@link CallableStatement#execute()} / {@link CallableStatement#getMoreResults()}),
     * detached as a {@link CachedRowSet}. Unlike {@link #selectFromCall(Connection, String, Object...)}, the returned
     * data is fully copied, so the original JDBC {@link ResultSet} and {@link CallableStatement} are closed inside
     * this method. Not recommended for large result sets because all rows are materialized in memory. IN parameters only.
     */
    public static ResultSet selectFromCallCached(Connection connection, String call, Object... inParams) throws SQLException {
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
