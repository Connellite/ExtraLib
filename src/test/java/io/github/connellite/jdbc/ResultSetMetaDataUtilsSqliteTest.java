package io.github.connellite.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultSetMetaDataUtilsSqliteTest {

    @Test
    void columnMetadataFromQuery() throws Exception {
        try (Connection c = SqliteMemory.open()) {
            SqliteMemory.bootstrapDemoSchema(c);
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, name AS label FROM demo WHERE 1=0")) {
                Collection<String> names = ResultSetMetaDataUtils.getColumnNames(rs);
                assertTrue(names.contains("id"));
                Collection<String> labels = ResultSetMetaDataUtils.getColumnLabels(rs);
                assertTrue(labels.contains("label"));
                Collection<String> types = ResultSetMetaDataUtils.getColumnTypeNames(rs);
                assertTrue(types.stream().noneMatch(String::isBlank));
            }
        }
    }
}
