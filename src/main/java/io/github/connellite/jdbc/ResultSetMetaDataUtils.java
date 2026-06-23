package io.github.connellite.jdbc;

import io.github.connellite.collections.NullSkippingLinkedHashSet;
import lombok.experimental.UtilityClass;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
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
        return Collections.unmodifiableCollection(out);
    }

    /**
     * Whether a column name exists in {@link ResultSetMetaData#getColumnName(int)} values.
     */
    public static boolean hasColumnName(ResultSet resultSet, String columnName) throws SQLException {
        return hasColumnName(resultSet.getMetaData(), columnName);
    }

    /**
     * Whether a column name exists in {@link ResultSetMetaData#getColumnName(int)} values.
     */
    public static boolean hasColumnName(ResultSetMetaData metaData, String columnName) throws SQLException {
        return getColumnNames(metaData).contains(columnName);
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
        return Collections.unmodifiableCollection(out);
    }

    /**
     * Whether a column label exists in {@link ResultSetMetaData#getColumnLabel(int)} values.
     */
    public static boolean hasColumnLabel(ResultSet resultSet, String columnLabel) throws SQLException {
        return hasColumnLabel(resultSet.getMetaData(), columnLabel);
    }

    /**
     * Whether a column label exists in {@link ResultSetMetaData#getColumnLabel(int)} values.
     */
    public static boolean hasColumnLabel(ResultSetMetaData metaData, String columnLabel) throws SQLException {
        return getColumnLabels(metaData).contains(columnLabel);
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
        return Collections.unmodifiableCollection(out);
    }

    /**
     * Whether a catalog name exists in {@link ResultSetMetaData#getCatalogName(int)} values.
     */
    public static boolean hasCatalogName(ResultSet resultSet, String catalogName) throws SQLException {
        return hasCatalogName(resultSet.getMetaData(), catalogName);
    }

    /**
     * Whether a catalog name exists in {@link ResultSetMetaData#getCatalogName(int)} values.
     */
    public static boolean hasCatalogName(ResultSetMetaData metaData, String catalogName) throws SQLException {
        return getCatalogNames(metaData).contains(catalogName);
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
        return Collections.unmodifiableCollection(out);
    }

    /**
     * Whether a schema name exists in {@link ResultSetMetaData#getSchemaName(int)} values.
     */
    public static boolean hasSchemaName(ResultSet resultSet, String schemaName) throws SQLException {
        return hasSchemaName(resultSet.getMetaData(), schemaName);
    }

    /**
     * Whether a schema name exists in {@link ResultSetMetaData#getSchemaName(int)} values.
     */
    public static boolean hasSchemaName(ResultSetMetaData metaData, String schemaName) throws SQLException {
        return getSchemaNames(metaData).contains(schemaName);
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
        return Collections.unmodifiableCollection(out);
    }

    /**
     * Whether a table name exists in {@link ResultSetMetaData#getTableName(int)} values.
     */
    public static boolean hasTableName(ResultSet resultSet, String tableName) throws SQLException {
        return hasTableName(resultSet.getMetaData(), tableName);
    }

    /**
     * Whether a table name exists in {@link ResultSetMetaData#getTableName(int)} values.
     */
    public static boolean hasTableName(ResultSetMetaData metaData, String tableName) throws SQLException {
        return getTableNames(metaData).contains(tableName);
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
        return Collections.unmodifiableCollection(out);
    }

    /**
     * Whether a column type name exists in {@link ResultSetMetaData#getColumnTypeName(int)} values.
     */
    public static boolean hasColumnTypeName(ResultSet resultSet, String columnTypeName) throws SQLException {
        return hasColumnTypeName(resultSet.getMetaData(), columnTypeName);
    }

    /**
     * Whether a column type name exists in {@link ResultSetMetaData#getColumnTypeName(int)} values.
     */
    public static boolean hasColumnTypeName(ResultSetMetaData metaData, String columnTypeName) throws SQLException {
        return getColumnTypeNames(metaData).contains(columnTypeName);
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
        return Collections.unmodifiableCollection(out);
    }

    /**
     * Whether a column class name exists in {@link ResultSetMetaData#getColumnClassName(int)} values.
     */
    public static boolean hasColumnClassName(ResultSet resultSet, String columnClassName) throws SQLException {
        return hasColumnClassName(resultSet.getMetaData(), columnClassName);
    }

    /**
     * Whether a column class name exists in {@link ResultSetMetaData#getColumnClassName(int)} values.
     */
    public static boolean hasColumnClassName(ResultSetMetaData metaData, String columnClassName) throws SQLException {
        return getColumnClassNames(metaData).contains(columnClassName);
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
        return Collections.unmodifiableCollection(out);
    }

    /**
     * Whether a qualified column label exists in computed {@code catalog.schema.table.label} values.
     */
    public static boolean hasQualifiedColumnLabel(ResultSet resultSet, String qualifiedColumnLabel) throws SQLException {
        return hasQualifiedColumnLabel(resultSet.getMetaData(), qualifiedColumnLabel);
    }

    /**
     * Whether a qualified column label exists in computed {@code catalog.schema.table.label} values.
     */
    public static boolean hasQualifiedColumnLabel(ResultSetMetaData metaData, String qualifiedColumnLabel) throws SQLException {
        return getQualifiedColumnLabels(metaData).contains(qualifiedColumnLabel);
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
