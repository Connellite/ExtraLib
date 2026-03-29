package io.github.connellite.jdbc;

import io.github.connellite.collections.NullSkippingLinkedHashSet;
import lombok.experimental.UtilityClass;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;

/**
 * Thin helpers over {@link DatabaseMetaData} that read common identifier columns into ordered, de-duplicated string collections.
 */
@UtilityClass
public class DatabaseMetaDataUtils {

    public static Collection<String> getTables(Connection connection) throws SQLException {
        Set<String> tablesList = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                tablesList.add(tableName);
            }
        }
        return tablesList;
    }

    public static Collection<String> getColumns(Connection connection, String tableName) throws SQLException {
        Set<String> columnsList = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet columns = meta.getColumns(null, null, tableName, "%")) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                columnsList.add(columnName);
            }
        }
        return columnsList;
    }

    /** {@link DatabaseMetaData#getCatalogs()} — {@code TABLE_CAT} values. */
    public static Collection<String> getCatalogs(Connection connection) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getCatalogs()) {
            while (rs.next()) {
                String catalogName = rs.getString("TABLE_CAT");
                out.add(catalogName);
            }
        }
        return out;
    }

    /**
     * {@link DatabaseMetaData#getSchemas(String, String)} — {@code TABLE_CATALOG.TABLE_SCHEM} when catalog is present, else {@code TABLE_SCHEM}.
     */
    public static Collection<String> getSchemas(Connection connection, String catalog, String schemaPattern) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getSchemas(catalog, schemaPattern)) {
            while (rs.next()) {
                String cat = rs.getString("TABLE_CATALOG");
                if (cat == null) {
                    cat = rs.getString("TABLE_CAT");
                }
                String schema = rs.getString("TABLE_SCHEM");
                String qualified = qualify(cat, schema);
                out.add(qualified);
            }
        }
        return out;
    }

    /**
     * {@link DatabaseMetaData#getFunctions(String, String, String)} — qualified {@code FUNCTION_SCHEM.FUNCTION_NAME} or {@code FUNCTION_NAME}.
     */
    public static Collection<String> getFunctions(Connection connection, String catalog, String schemaPattern, String functionNamePattern)
            throws SQLException {
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
        return out;
    }

    /**
     * {@link DatabaseMetaData#getProcedures(String, String, String)} — qualified {@code PROCEDURE_SCHEM.PROCEDURE_NAME} or {@code PROCEDURE_NAME}.
     */
    public static Collection<String> getProcedures(Connection connection, String catalog, String schemaPattern, String procedureNamePattern)
            throws SQLException {
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
        return out;
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
        return out;
    }

    /** {@link DatabaseMetaData#getPrimaryKeys(String, String, String)} — {@code COLUMN_NAME} in JDBC key-sequence order. */
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

    /** {@link DatabaseMetaData#getImportedKeys(String, String, String)} — local {@code FKCOLUMN_NAME} values. */
    public static Collection<String> getImportedKeys(Connection connection, String catalog, String schema, String table) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getImportedKeys(catalog, schema, table)) {
            while (rs.next()) {
                String columnName = rs.getString("FKCOLUMN_NAME");
                out.add(columnName);
            }
        }
        return out;
    }

    /** {@link DatabaseMetaData#getExportedKeys(String, String, String)} — {@code PKCOLUMN_NAME} on this table as referenced primary key. */
    public static Collection<String> getExportedKeys(Connection connection, String catalog, String schema, String table) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getExportedKeys(catalog, schema, table)) {
            while (rs.next()) {
                String columnName = rs.getString("PKCOLUMN_NAME");
                out.add(columnName);
            }
        }
        return out;
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
