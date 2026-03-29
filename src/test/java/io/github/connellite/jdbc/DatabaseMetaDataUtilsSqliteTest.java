package io.github.connellite.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMetaDataUtilsSqliteTest {

    @Test
    void getTablesAndColumns() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            Collection<String> tables = DatabaseMetaDataUtils.getTables(c);
            assertTrue(tables.contains("demo"));
            assertTrue(tables.contains("child"));

            Collection<String> cols = DatabaseMetaDataUtils.getColumns(c, "demo");
            assertTrue(cols.contains("id"));
            assertTrue(cols.contains("name"));
        }
    }

    @Test
    void getPrimaryKeysImportedExported() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            Collection<String> pk = DatabaseMetaDataUtils.getPrimaryKeys(c, null, null, "demo");
            assertTrue(pk.contains("id"));

            Collection<String> imp = DatabaseMetaDataUtils.getImportedKeys(c, null, null, "child");
            assertNotNull(imp);
            if (!imp.isEmpty()) {
                assertTrue(imp.contains("demo_id"));
            }

            Collection<String> exp = DatabaseMetaDataUtils.getExportedKeys(c, null, null, "demo");
            assertNotNull(exp);
            if (!exp.isEmpty()) {
                assertTrue(exp.contains("id"));
            }
        }
    }

    @Test
    void getCatalogsAndSchemasDoNotThrow() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try {
                Collection<String> catalogs = DatabaseMetaDataUtils.getCatalogs(c);
                assertNotNull(catalogs);
            } catch (SQLFeatureNotSupportedException ignore) {
            }

            try {
                Collection<String> schemas = DatabaseMetaDataUtils.getSchemas(c, null, null);
                assertNotNull(schemas);
            } catch (SQLFeatureNotSupportedException ignore) {
            }
        }
    }
}
