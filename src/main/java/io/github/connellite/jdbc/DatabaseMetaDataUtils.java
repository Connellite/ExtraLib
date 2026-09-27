package io.github.connellite.jdbc;

import io.github.connellite.collections.NullSkippingLinkedHashSet;
import lombok.experimental.UtilityClass;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Thin helpers over {@link DatabaseMetaData} that read common identifier columns into ordered, de-duplicated string collections.
 */
@UtilityClass
public class DatabaseMetaDataUtils {

    /**
     * {@link DatabaseMetaData#getTables(String, String, String, String[])} — {@code TABLE_NAME} for {@code TABLE} rows.
     */
    public static Collection<String> getTables(Connection connection) throws SQLException {
        return getTables(connection, null);
    }

    /**
     * {@link DatabaseMetaData#getTables(String, String, String, String[])} — {@code TABLE_NAME} for {@code TABLE} rows.
     */
    public static Collection<String> getTables(Connection connection, String catalog) throws SQLException {
        return getTables(connection, catalog, null);
    }

    /**
     * {@link DatabaseMetaData#getTables(String, String, String, String[])} — {@code TABLE_NAME} for {@code TABLE} rows.
     */
    public static Collection<String> getTables(Connection connection, String catalog, String schemaPattern) throws SQLException {
        return getTables(connection, catalog, schemaPattern, "%");
    }

    /**
     * {@link DatabaseMetaData#getTables(String, String, String, String[])} — {@code TABLE_NAME} for {@code TABLE} rows.
     */
    public static Collection<String> getTables(Connection connection, String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        Set<String> tablesList = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet tables = meta.getTables(catalog, schemaPattern, tableNamePattern, new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                tablesList.add(tableName);
            }
        }
        return Collections.unmodifiableCollection(tablesList);
    }

    /**
     * {@link DatabaseMetaData#getTables(String, String, String, String[])} — {@code TABLE_NAME} for {@code VIEW} rows.
     */
    public static Collection<String> getViews(Connection connection) throws SQLException {
        return getViews(connection, null);
    }

    /**
     * {@link DatabaseMetaData#getTables(String, String, String, String[])} — {@code TABLE_NAME} for {@code VIEW} rows.
     */
    public static Collection<String> getViews(Connection connection, String catalog) throws SQLException {
        return getViews(connection, catalog, null);
    }

    /**
     * {@link DatabaseMetaData#getTables(String, String, String, String[])} — {@code TABLE_NAME} for {@code VIEW} rows.
     */
    public static Collection<String> getViews(Connection connection, String catalog, String schemaPattern) throws SQLException {
        return getViews(connection, catalog, schemaPattern, "%");
    }

    /**
     * {@link DatabaseMetaData#getTables(String, String, String, String[])} — {@code TABLE_NAME} for {@code VIEW} rows.
     */
    public static Collection<String> getViews(Connection connection, String catalog, String schemaPattern, String tableNamePattern)
            throws SQLException {
        Set<String> viewsList = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet views = meta.getTables(catalog, schemaPattern, tableNamePattern, new String[]{"VIEW"})) {
            while (views.next()) {
                String viewName = views.getString("TABLE_NAME");
                viewsList.add(viewName);
            }
        }
        return Collections.unmodifiableCollection(viewsList);
    }

    /**
     * {@link DatabaseMetaData#getTables(String, String, String, String[])} — {@code TABLE_NAME} for {@code TABLE} and {@code VIEW} rows.
     */
    public static Collection<String> getTablesAndViews(Connection connection) throws SQLException {
        return getTablesAndViews(connection, null);
    }

    /**
     * {@link DatabaseMetaData#getTables(String, String, String, String[])} — {@code TABLE_NAME} for {@code TABLE} and {@code VIEW} rows.
     */
    public static Collection<String> getTablesAndViews(Connection connection, String catalog) throws SQLException {
        return getTablesAndViews(connection, catalog, null);
    }

    /**
     * {@link DatabaseMetaData#getTables(String, String, String, String[])} — {@code TABLE_NAME} for {@code TABLE} and {@code VIEW} rows.
     */
    public static Collection<String> getTablesAndViews(Connection connection, String catalog, String schemaPattern) throws SQLException {
        return getTablesAndViews(connection, catalog, schemaPattern, "%");
    }

    /**
     * {@link DatabaseMetaData#getTables(String, String, String, String[])} — {@code TABLE_NAME} for {@code TABLE} and {@code VIEW} rows.
     */
    public static Collection<String> getTablesAndViews(Connection connection, String catalog, String schemaPattern, String tableNamePattern)
            throws SQLException {
        Set<String> names = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getTables(catalog, schemaPattern, tableNamePattern, new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                names.add(tableName);
            }
        }
        return Collections.unmodifiableCollection(names);
    }

    /**
     * {@link DatabaseMetaData#getColumns(String, String, String, String)} — {@code COLUMN_NAME} values.
     */
    public static Collection<String> getColumns(Connection connection, String tableName) throws SQLException {
        return getColumns(connection, null, tableName);
    }

    /**
     * {@link DatabaseMetaData#getColumns(String, String, String, String)} — {@code COLUMN_NAME} values.
     */
    public static Collection<String> getColumns(Connection connection, String catalog, String tableName) throws SQLException {
        return getColumns(connection, catalog, null, tableName);
    }

    /**
     * {@link DatabaseMetaData#getColumns(String, String, String, String)} — {@code COLUMN_NAME} values.
     */
    public static Collection<String> getColumns(Connection connection, String catalog, String schemaPattern, String tableName) throws SQLException {
        return getColumns(connection, catalog, schemaPattern, tableName, "%");
    }

    /**
     * {@link DatabaseMetaData#getColumns(String, String, String, String)} — {@code COLUMN_NAME} values.
     */
    public static Collection<String> getColumns(Connection connection, String catalog, String schemaPattern, String tableName, String columnNamePattern) throws SQLException {
        Set<String> columnsList = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet columns = meta.getColumns(catalog, schemaPattern, tableName, columnNamePattern)) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                columnsList.add(columnName);
            }
        }
        return Collections.unmodifiableCollection(columnsList);
    }

    /**
     * {@link DatabaseMetaData#getCatalogs()} — {@code TABLE_CAT} values.
     */
    public static Collection<String> getCatalogs(Connection connection) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getCatalogs()) {
            while (rs.next()) {
                String catalogName = rs.getString("TABLE_CAT");
                out.add(catalogName);
            }
        }
        return Collections.unmodifiableCollection(out);
    }

    /**
     * {@link DatabaseMetaData#getSchemas()} — {@code TABLE_CATALOG.TABLE_SCHEM} when catalog is present, else {@code TABLE_SCHEM}.
     */
    public static Collection<String> getSchemas(Connection connection) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getSchemas()) {
            return collectSchemas(rs);
        }
    }

    /**
     * {@link DatabaseMetaData#getSchemas(String, String)} — {@code TABLE_CATALOG.TABLE_SCHEM} when catalog is present, else {@code TABLE_SCHEM}.
     */
    public static Collection<String> getSchemas(Connection connection, String catalog) throws SQLException {
        return getSchemas(connection, catalog, null);
    }

    /**
     * {@link DatabaseMetaData#getSchemas(String, String)} — {@code TABLE_CATALOG.TABLE_SCHEM} when catalog is present, else {@code TABLE_SCHEM}.
     */
    public static Collection<String> getSchemas(Connection connection, String catalog, String schemaPattern) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getSchemas(catalog, schemaPattern)) {
            return collectSchemas(rs);
        }
    }

    /**
     * {@link DatabaseMetaData#getFunctions(String, String, String)} — qualified {@code FUNCTION_SCHEM.FUNCTION_NAME} or {@code FUNCTION_NAME}.
     */
    public static Collection<String> getFunctions(Connection connection) throws SQLException {
        return getFunctions(connection, null);
    }

    /**
     * {@link DatabaseMetaData#getFunctions(String, String, String)} — qualified {@code FUNCTION_SCHEM.FUNCTION_NAME} or {@code FUNCTION_NAME}.
     */
    public static Collection<String> getFunctions(Connection connection, String catalog) throws SQLException {
        return getFunctions(connection, catalog, null);
    }

    /**
     * {@link DatabaseMetaData#getFunctions(String, String, String)} — qualified {@code FUNCTION_SCHEM.FUNCTION_NAME} or {@code FUNCTION_NAME}.
     */
    public static Collection<String> getFunctions(Connection connection, String catalog, String schemaPattern) throws SQLException {
        return getFunctions(connection, catalog, schemaPattern, "%");
    }

    /**
     * {@link DatabaseMetaData#getFunctions(String, String, String)} — qualified {@code FUNCTION_SCHEM.FUNCTION_NAME} or {@code FUNCTION_NAME}.
     */
    public static Collection<String> getFunctions(Connection connection, String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getFunctions(catalog, schemaPattern, functionNamePattern)) {
            while (rs.next()) {
                String schema = rs.getString("FUNCTION_SCHEM");
                String name = rs.getString("FUNCTION_NAME");
                String qualified = qualify(schema, name);
                out.add(qualified);
            }
        }
        return Collections.unmodifiableCollection(out);
    }

    /**
     * {@link DatabaseMetaData#getFunctionColumns(String, String, String, String)} — {@code COLUMN_NAME} per row (function parameter/result columns).
     */
    public static Collection<String> getFunctionColumns(Connection connection, String functionNamePattern) throws SQLException {
        return getFunctionColumns(connection, null, functionNamePattern);
    }

    /**
     * {@link DatabaseMetaData#getFunctionColumns(String, String, String, String)} — {@code COLUMN_NAME} per row (function parameter/result columns).
     */
    public static Collection<String> getFunctionColumns(Connection connection, String catalog, String functionNamePattern)
            throws SQLException {
        return getFunctionColumns(connection, catalog, null, functionNamePattern);
    }

    /**
     * {@link DatabaseMetaData#getFunctionColumns(String, String, String, String)} — {@code COLUMN_NAME} per row (function parameter/result columns).
     */
    public static Collection<String> getFunctionColumns(
            Connection connection, String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
        return getFunctionColumns(connection, catalog, schemaPattern, functionNamePattern, "%");
    }

    /**
     * {@link DatabaseMetaData#getFunctionColumns(String, String, String, String)} — {@code COLUMN_NAME} per row (function parameter/result columns).
     */
    public static Collection<String> getFunctionColumns(
            Connection connection,
            String catalog,
            String schemaPattern,
            String functionNamePattern,
            String columnNamePattern) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                out.add(columnName);
            }
        }
        return Collections.unmodifiableCollection(out);
    }

    /**
     * {@link DatabaseMetaData#getProcedures(String, String, String)} — qualified {@code PROCEDURE_SCHEM.PROCEDURE_NAME} or {@code PROCEDURE_NAME}.
     */
    public static Collection<String> getProcedures(Connection connection) throws SQLException {
        return getProcedures(connection, null);
    }

    /**
     * {@link DatabaseMetaData#getProcedures(String, String, String)} — qualified {@code PROCEDURE_SCHEM.PROCEDURE_NAME} or {@code PROCEDURE_NAME}.
     */
    public static Collection<String> getProcedures(Connection connection, String catalog) throws SQLException {
        return getProcedures(connection, catalog, null);
    }

    /**
     * {@link DatabaseMetaData#getProcedures(String, String, String)} — qualified {@code PROCEDURE_SCHEM.PROCEDURE_NAME} or {@code PROCEDURE_NAME}.
     */
    public static Collection<String> getProcedures(Connection connection, String catalog, String schemaPattern) throws SQLException {
        return getProcedures(connection, catalog, schemaPattern, "%");
    }

    /**
     * {@link DatabaseMetaData#getProcedures(String, String, String)} — qualified {@code PROCEDURE_SCHEM.PROCEDURE_NAME} or {@code PROCEDURE_NAME}.
     */
    public static Collection<String> getProcedures(Connection connection, String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getProcedures(catalog, schemaPattern, procedureNamePattern)) {
            while (rs.next()) {
                String schema = rs.getString("PROCEDURE_SCHEM");
                String name = rs.getString("PROCEDURE_NAME");
                String qualified = qualify(schema, name);
                out.add(qualified);
            }
        }
        return Collections.unmodifiableCollection(out);
    }

    /**
     * {@link DatabaseMetaData#getProcedureColumns(String, String, String, String)} — {@code COLUMN_NAME} per row (procedure parameter/result columns).
     */
    public static Collection<String> getProcedureColumns(Connection connection, String procedureNamePattern) throws SQLException {
        return getProcedureColumns(connection, null, procedureNamePattern);
    }

    /**
     * {@link DatabaseMetaData#getProcedureColumns(String, String, String, String)} — {@code COLUMN_NAME} per row (procedure parameter/result columns).
     */
    public static Collection<String> getProcedureColumns(Connection connection, String catalog, String procedureNamePattern)
            throws SQLException {
        return getProcedureColumns(connection, catalog, null, procedureNamePattern);
    }

    /**
     * {@link DatabaseMetaData#getProcedureColumns(String, String, String, String)} — {@code COLUMN_NAME} per row (procedure parameter/result columns).
     */
    public static Collection<String> getProcedureColumns(
            Connection connection, String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
        return getProcedureColumns(connection, catalog, schemaPattern, procedureNamePattern, "%");
    }

    /**
     * {@link DatabaseMetaData#getProcedureColumns(String, String, String, String)} — {@code COLUMN_NAME} per row (procedure parameter/result columns).
     */
    public static Collection<String> getProcedureColumns(
            Connection connection,
            String catalog,
            String schemaPattern,
            String procedureNamePattern,
            String columnNamePattern) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                out.add(columnName);
            }
        }
        return Collections.unmodifiableCollection(out);
    }

    /**
     * {@link DatabaseMetaData#getPrimaryKeys(String, String, String)} — {@code COLUMN_NAME} in JDBC key-sequence order.
     */
    public static Collection<String> getPrimaryKeys(Connection connection, String table) throws SQLException {
        return getPrimaryKeys(connection, null, table);
    }

    /**
     * {@link DatabaseMetaData#getPrimaryKeys(String, String, String)} — {@code COLUMN_NAME} in JDBC key-sequence order.
     */
    public static Collection<String> getPrimaryKeys(Connection connection, String catalog, String table) throws SQLException {
        return getPrimaryKeys(connection, catalog, null, table);
    }

    /**
     * {@link DatabaseMetaData#getPrimaryKeys(String, String, String)} — {@code COLUMN_NAME} in JDBC key-sequence order.
     */
    public static Collection<String> getPrimaryKeys(Connection connection, String catalog, String schema, String table) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getPrimaryKeys(catalog, schema, table)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                out.add(columnName);
            }
        }
        return out;
    }

    /**
     * {@link DatabaseMetaData#getImportedKeys(String, String, String)} — local {@code FKCOLUMN_NAME} values.
     */
    public static Collection<String> getImportedKeys(Connection connection, String table) throws SQLException {
        return getImportedKeys(connection, null, table);
    }

    /**
     * {@link DatabaseMetaData#getImportedKeys(String, String, String)} — local {@code FKCOLUMN_NAME} values.
     */
    public static Collection<String> getImportedKeys(Connection connection, String catalog, String table) throws SQLException {
        return getImportedKeys(connection, catalog, null, table);
    }

    /**
     * {@link DatabaseMetaData#getImportedKeys(String, String, String)} — local {@code FKCOLUMN_NAME} values.
     */
    public static Collection<String> getImportedKeys(Connection connection, String catalog, String schema, String table) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getImportedKeys(catalog, schema, table)) {
            while (rs.next()) {
                String columnName = rs.getString("FKCOLUMN_NAME");
                out.add(columnName);
            }
        }
        return Collections.unmodifiableCollection(out);
    }

    /**
     * {@link DatabaseMetaData#getExportedKeys(String, String, String)} — {@code PKCOLUMN_NAME} on this table as referenced primary key.
     */
    public static Collection<String> getExportedKeys(Connection connection, String table) throws SQLException {
        return getExportedKeys(connection, null, table);
    }

    /**
     * {@link DatabaseMetaData#getExportedKeys(String, String, String)} — {@code PKCOLUMN_NAME} on this table as referenced primary key.
     */
    public static Collection<String> getExportedKeys(Connection connection, String catalog, String table) throws SQLException {
        return getExportedKeys(connection, catalog, null, table);
    }

    /**
     * {@link DatabaseMetaData#getExportedKeys(String, String, String)} — {@code PKCOLUMN_NAME} on this table as referenced primary key.
     */
    public static Collection<String> getExportedKeys(Connection connection, String catalog, String schema, String table) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getExportedKeys(catalog, schema, table)) {
            while (rs.next()) {
                String columnName = rs.getString("PKCOLUMN_NAME");
                out.add(columnName);
            }
        }
        return Collections.unmodifiableCollection(out);
    }

    /**
     * {@link DatabaseMetaData#getIndexInfo(String, String, String, boolean, boolean)} — {@code INDEX_NAME}
     * ({@code unique=false}, {@code approximate=false}).
     */
    public static Collection<String> getIndexInfo(Connection connection, String table) throws SQLException {
        return getIndexInfo(connection, null, table);
    }

    /**
     * {@link DatabaseMetaData#getIndexInfo(String, String, String, boolean, boolean)} — {@code INDEX_NAME}
     * ({@code unique=false}, {@code approximate=false}).
     */
    public static Collection<String> getIndexInfo(Connection connection, String catalog, String table) throws SQLException {
        return getIndexInfo(connection, catalog, null, table);
    }

    /**
     * {@link DatabaseMetaData#getIndexInfo(String, String, String, boolean, boolean)} — {@code INDEX_NAME}
     * ({@code unique=false}, {@code approximate=false}).
     */
    public static Collection<String> getIndexInfo(Connection connection, String catalog, String schema, String table)
            throws SQLException {
        return getIndexInfo(connection, catalog, schema, table, false, false);
    }

    /**
     * {@link DatabaseMetaData#getIndexInfo(String, String, String, boolean, boolean)} — {@code INDEX_NAME}.
     */
    public static Collection<String> getIndexInfo(
            Connection connection,
            String catalog,
            String schema,
            String table,
            boolean unique,
            boolean approximate) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getIndexInfo(catalog, schema, table, unique, approximate)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                out.add(indexName);
            }
        }
        return Collections.unmodifiableCollection(out);
    }

    private static Collection<String> collectSchemas(ResultSet rs) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        while (rs.next()) {
            String cat = rs.getString("TABLE_CATALOG");
            if (cat == null) {
                cat = rs.getString("TABLE_CAT");
            }
            String schema = rs.getString("TABLE_SCHEM");
            out.add(qualify(cat, schema));
        }
        return Collections.unmodifiableCollection(out);
    }

    private static String qualify(String first, String second) {
        if (second == null) {
            return null;
        }
        if (first == null || first.isEmpty()) {
            return second;
        }
        return first + "." + second;
    }
}
