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
                assertTrue(names.contains("ID"));
                Collection<String> labels = ResultSetMetaDataUtils.getColumnLabels(rs);
                assertTrue(labels.contains("label"));
                assertTrue(labels.contains("LABEL"));
                Collection<String> types = ResultSetMetaDataUtils.getColumnTypeNames(rs);
                assertTrue(types.stream().noneMatch(String::isBlank));
                assertTrue(types.contains("integer"));

                assertTrue(ResultSetMetaDataUtils.hasColumnName(rs, "id"));
                assertTrue(ResultSetMetaDataUtils.hasColumnName(rs, "ID"));
                assertFalse(ResultSetMetaDataUtils.hasColumnName(rs, "missing"));
                assertTrue(ResultSetMetaDataUtils.hasColumnName(metaData, "id"));
                assertTrue(ResultSetMetaDataUtils.hasColumnName(metaData, "Id"));
                assertFalse(ResultSetMetaDataUtils.hasColumnName(metaData, "missing"));

                assertTrue(ResultSetMetaDataUtils.hasColumnLabel(rs, "label"));
                assertTrue(ResultSetMetaDataUtils.hasColumnLabel(rs, "Label"));
                assertFalse(ResultSetMetaDataUtils.hasColumnLabel(rs, "name"));
                assertTrue(ResultSetMetaDataUtils.hasColumnLabel(metaData, "label"));
                assertTrue(ResultSetMetaDataUtils.hasColumnLabel(metaData, "LABEL"));
                assertFalse(ResultSetMetaDataUtils.hasColumnLabel(metaData, "name"));

                assertTrue(ResultSetMetaDataUtils.hasColumnTypeName(rs, "INTEGER"));
                assertTrue(ResultSetMetaDataUtils.hasColumnTypeName(rs, "integer"));
                assertTrue(ResultSetMetaDataUtils.hasColumnTypeName(metaData, "INTEGER"));
                assertTrue(ResultSetMetaDataUtils.hasColumnTypeName(metaData, "Integer"));
                assertFalse(ResultSetMetaDataUtils.hasColumnTypeName(rs, "MISSING_TYPE"));
            }
        }
    }
}
