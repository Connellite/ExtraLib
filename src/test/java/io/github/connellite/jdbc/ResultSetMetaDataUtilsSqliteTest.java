package io.github.connellite.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultSetMetaDataUtilsSqliteTest {

    @Test
    void columnMetadataFromQuery() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, name AS label FROM demo WHERE 1=0")) {
                ResultSetMetaData metaData = rs.getMetaData();

                Collection<String> names = ResultSetMetaDataUtils.getColumnNames(rs);
                assertTrue(names.contains("id"));
                Collection<String> labels = ResultSetMetaDataUtils.getColumnLabels(rs);
                assertTrue(labels.contains("label"));
                Collection<String> types = ResultSetMetaDataUtils.getColumnTypeNames(rs);
                assertTrue(types.stream().noneMatch(String::isBlank));

                assertTrue(ResultSetMetaDataUtils.hasColumnName(rs, "id"));
                assertFalse(ResultSetMetaDataUtils.hasColumnName(rs, "missing"));
                assertTrue(ResultSetMetaDataUtils.hasColumnName(metaData, "id"));
                assertFalse(ResultSetMetaDataUtils.hasColumnName(metaData, "missing"));

                assertTrue(ResultSetMetaDataUtils.hasColumnLabel(rs, "label"));
                assertFalse(ResultSetMetaDataUtils.hasColumnLabel(rs, "name"));
                assertTrue(ResultSetMetaDataUtils.hasColumnLabel(metaData, "label"));
                assertFalse(ResultSetMetaDataUtils.hasColumnLabel(metaData, "name"));

                assertTrue(ResultSetMetaDataUtils.hasColumnTypeName(rs, "INTEGER"));
                assertTrue(ResultSetMetaDataUtils.hasColumnTypeName(metaData, "INTEGER"));
                assertFalse(ResultSetMetaDataUtils.hasColumnTypeName(rs, "MISSING_TYPE"));
            }
        }
    }
}
