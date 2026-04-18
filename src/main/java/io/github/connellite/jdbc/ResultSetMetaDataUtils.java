package io.github.connellite.jdbc;

import io.github.connellite.collections.NullSkippingLinkedHashSet;
import lombok.experimental.UtilityClass;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;

/**
 * Thin helpers over {@link ResultSetMetaData} that read common column metadata into ordered, de-duplicated string collections.
 */
@UtilityClass
public class ResultSetMetaDataUtils {

    /**
     * {@link ResultSetMetaData#getColumnName(int)} for each column, in order.
     */
    public static Collection<String> getColumnNames(ResultSet resultSet) throws SQLException {
        return getColumnNames(resultSet.getMetaData());
    }

    /**
     * {@link ResultSetMetaData#getColumnName(int)} for each column, in order.
     */
    public static Collection<String> getColumnNames(ResultSetMetaData metaData) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            out.add(columnName);
        }
        return out;
    }

    /**
     * {@link ResultSetMetaData#getColumnLabel(int)} for each column, in order.
     */
    public static Collection<String> getColumnLabels(ResultSet resultSet) throws SQLException {
        return getColumnLabels(resultSet.getMetaData());
    }

    /**
     * {@link ResultSetMetaData#getColumnLabel(int)} for each column, in order.
     */
    public static Collection<String> getColumnLabels(ResultSetMetaData metaData) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String columnLabel = metaData.getColumnLabel(i);
            out.add(columnLabel);
        }
        return out;
    }

    /**
     * Distinct {@link ResultSetMetaData#getCatalogName(int)} values in column order.
     */
    public static Collection<String> getCatalogNames(ResultSet resultSet) throws SQLException {
        return getCatalogNames(resultSet.getMetaData());
    }

    /**
     * Distinct {@link ResultSetMetaData#getCatalogName(int)} values in column order.
     */
    public static Collection<String> getCatalogNames(ResultSetMetaData metaData) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String catalogName = metaData.getCatalogName(i);
            out.add(catalogName);
        }
        return out;
    }

    /**
     * Distinct {@link ResultSetMetaData#getSchemaName(int)} values in column order.
     */
    public static Collection<String> getSchemaNames(ResultSet resultSet) throws SQLException {
        return getSchemaNames(resultSet.getMetaData());
    }

    /**
     * Distinct {@link ResultSetMetaData#getSchemaName(int)} values in column order.
     */
    public static Collection<String> getSchemaNames(ResultSetMetaData metaData) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String schemaName = metaData.getSchemaName(i);
            out.add(schemaName);
        }
        return out;
    }

    /**
     * Distinct {@link ResultSetMetaData#getTableName(int)} values in column order.
     */
    public static Collection<String> getTableNames(ResultSet resultSet) throws SQLException {
        return getTableNames(resultSet.getMetaData());
    }

    /**
     * Distinct {@link ResultSetMetaData#getTableName(int)} values in column order.
     */
    public static Collection<String> getTableNames(ResultSetMetaData metaData) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String tableName = metaData.getTableName(i);
            out.add(tableName);
        }
        return out;
    }

    /**
     * {@link ResultSetMetaData#getColumnTypeName(int)} for each column, in order.
     */
    public static Collection<String> getColumnTypeNames(ResultSet resultSet) throws SQLException {
        return getColumnTypeNames(resultSet.getMetaData());
    }

    /**
     * {@link ResultSetMetaData#getColumnTypeName(int)} for each column, in order.
     */
    public static Collection<String> getColumnTypeNames(ResultSetMetaData metaData) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String typeName = metaData.getColumnTypeName(i);
            out.add(typeName);
        }
        return out;
    }

    /**
     * {@link ResultSetMetaData#getColumnClassName(int)} for each column, in order.
     */
    public static Collection<String> getColumnClassNames(ResultSet resultSet) throws SQLException {
        return getColumnClassNames(resultSet.getMetaData());
    }

    /**
     * {@link ResultSetMetaData#getColumnClassName(int)} for each column, in order.
     */
    public static Collection<String> getColumnClassNames(ResultSetMetaData metaData) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String className = metaData.getColumnClassName(i);
            out.add(className);
        }
        return out;
    }

    /**
     * Qualified label per column: {@code catalog.schema.table.label} with empty/ absent segments omitted (same chaining rules as {@code DatabaseMetaDataUtils}).
     */
    public static Collection<String> getQualifiedColumnLabels(ResultSet resultSet) throws SQLException {
        return getQualifiedColumnLabels(resultSet.getMetaData());
    }

    /**
     * Qualified label per column: {@code catalog.schema.table.label} with empty/ absent segments omitted.
     */
    public static Collection<String> getQualifiedColumnLabels(ResultSetMetaData metaData) throws SQLException {
        Set<String> out = new NullSkippingLinkedHashSet<>();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String catalogName = metaData.getCatalogName(i);
            String schemaName = metaData.getSchemaName(i);
            String tableName = metaData.getTableName(i);
            String columnLabel = metaData.getColumnLabel(i);
            String qualified = qualifyCatalogSchemaTableColumn(catalogName, schemaName, tableName, columnLabel);
            out.add(qualified);
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

    private static String qualifyCatalogSchemaTableColumn(String catalog, String schema, String table, String columnLabel) {
        String chain = qualify(table, columnLabel);
        chain = qualify(schema, chain);
        return qualify(catalog, chain);
    }
}
