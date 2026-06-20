package io.github.connellite.jdbc;

import io.github.connellite.jdbc.annotation.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleResultSetBeanMapperJdbcTypesTest {

    @Test
    void normalizesOracleTimestampToLocalDateTime() throws Exception {
        Timestamp ts = Timestamp.valueOf("2024-07-10 09:30:45");
        ResultSet rs = fakeRowResultSet(
                Map.of("created_at", new oracle.sql.TIMESTAMP()),
                Map.of("created_at", ts),
                Map.of("created_at", "java.sql.Timestamp")
        );

        SimpleResultSetBeanMapper<TemporalRow> mapper = new SimpleResultSetBeanMapper<>(TemporalRow.class);
        TemporalRow row = mapper.mapRow(rs);

        assertEquals(LocalDateTime.of(2024, 7, 10, 9, 30, 45), row.createdAt);
    }

    @Test
    void normalizesOracleTimestamptzToLocalDateTime() throws Exception {
        Timestamp ts = Timestamp.valueOf("2024-07-11 10:15:30");
        ResultSet rs = fakeRowResultSet(
                Map.of("created_at", new oracle.sql.TIMESTAMPTZ()),
                Map.of("created_at", ts),
                Map.of("created_at", "java.sql.Timestamp")
        );

        SimpleResultSetBeanMapper<TemporalRow> mapper = new SimpleResultSetBeanMapper<>(TemporalRow.class);
        TemporalRow row = mapper.mapRow(rs);

        assertEquals(LocalDateTime.of(2024, 7, 11, 10, 15, 30), row.createdAt);
    }

    @Test
    void normalizesOracleDateUsingTimestampMetadata() throws Exception {
        Timestamp ts = Timestamp.valueOf("2024-07-12 08:00:00");
        ResultSet rs = fakeRowResultSet(
                Map.of("created_at", new oracle.sql.DATE()),
                Map.of("created_at", ts),
                Map.of("created_at", "oracle.sql.TIMESTAMP")
        );

        SimpleResultSetBeanMapper<TemporalRow> mapper = new SimpleResultSetBeanMapper<>(TemporalRow.class);
        TemporalRow row = mapper.mapRow(rs);

        assertEquals(LocalDateTime.of(2024, 7, 12, 8, 0), row.createdAt);
    }

    @Test
    void normalizesSqlDateToTimestampWhenMetadataSaysTimestamp() throws Exception {
        Timestamp ts = Timestamp.valueOf("2024-07-13 12:45:00");
        ResultSet rs = fakeRowResultSet(
                Map.of("created_at", new java.sql.Date(ts.getTime())),
                Map.of("created_at", ts),
                Map.of("created_at", "java.sql.Timestamp")
        );

        SimpleResultSetBeanMapper<TemporalRow> mapper = new SimpleResultSetBeanMapper<>(TemporalRow.class);
        TemporalRow row = mapper.mapRow(rs);

        assertEquals(LocalDateTime.of(2024, 7, 13, 12, 45), row.createdAt);
    }

    static class TemporalRow {
        @Column("created_at")
        private LocalDateTime createdAt;
    }

    private static ResultSet fakeRowResultSet(
            Map<String, Object> row,
            Map<String, Object> typedValues,
            Map<String, String> columnClassNames) {
        Map<String, Integer> columnIndexes = new HashMap<>();
        int index = 1;
        for (String column : row.keySet()) {
            columnIndexes.put(column, index++);
        }

        ResultSetMetaData metaData = (ResultSetMetaData) Proxy.newProxyInstance(
                ResultSetMetaData.class.getClassLoader(),
                new Class[]{ResultSetMetaData.class},
                (proxy, method, args) -> {
                    if ("getColumnClassName".equals(method.getName()) && args != null && args.length == 1) {
                        String column = findColumnName(columnIndexes, (Integer) args[0]);
                        return columnClassNames.get(column);
                    }
                    throw new UnsupportedOperationException("Unsupported ResultSetMetaData method: " + method.getName());
                });

        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, args) -> {
                    if ("getObject".equals(method.getName()) && args != null && args.length == 1) {
                        Object arg = args[0];
                        if (arg instanceof Integer columnIndex) {
                            return row.get(findColumnName(columnIndexes, columnIndex));
                        }
                        return row.get(String.valueOf(arg));
                    }
                    if ("findColumn".equals(method.getName()) && args != null && args.length == 1) {
                        String column = String.valueOf(args[0]);
                        Integer indexValue = columnIndexes.get(column);
                        if (indexValue == null) {
                            throw new java.sql.SQLException("Column not found: " + column);
                        }
                        return indexValue;
                    }
                    if ("getTimestamp".equals(method.getName()) && args != null && args.length == 1) {
                        Object arg = args[0];
                        if (arg instanceof Integer columnIndex) {
                            return typedValues.get(findColumnName(columnIndexes, columnIndex));
                        }
                        return typedValues.get(String.valueOf(arg));
                    }
                    if ("getDate".equals(method.getName()) && args != null && args.length == 1) {
                        Object arg = args[0];
                        String column = arg instanceof Integer columnIndex
                                ? findColumnName(columnIndexes, columnIndex)
                                : String.valueOf(arg);
                        Object value = typedValues.get(column);
                        if (value instanceof Timestamp timestamp) {
                            return new java.sql.Date(timestamp.getTime());
                        }
                        return value;
                    }
                    if ("getMetaData".equals(method.getName())) {
                        return metaData;
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    if ("isClosed".equals(method.getName())) {
                        return false;
                    }
                    throw new UnsupportedOperationException("Unsupported ResultSet method: " + method.getName());
                });
    }

    private static String findColumnName(Map<String, Integer> columnIndexes, int columnIndex) {
        for (Map.Entry<String, Integer> entry : columnIndexes.entrySet()) {
            if (entry.getValue() == columnIndex) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Unknown column index: " + columnIndex);
    }
}
