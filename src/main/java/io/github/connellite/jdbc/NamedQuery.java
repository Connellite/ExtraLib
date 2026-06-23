package io.github.connellite.jdbc;

import io.github.connellite.collections.LinkedMultiValueHashMap;
import io.github.connellite.jdbc.parser.ColonPrefixSqlParser;
import io.github.connellite.jdbc.parser.ParsedSql;
import io.github.connellite.jdbc.parser.SqlParser;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Fluent builder that collects named-parameter bindings and materializes a
 * fully bound {@link NamedPreparedStatement} only at {@link #prepare(Connection)}.
 * <p>
 * Unlike {@link NamedPreparedStatement}, this type does not call JDBC
 * {@code prepareStatement} in a constructor. Bindings are stored first; SQL is
 * parsed, optionally expanded, and only then prepared. That makes it possible
 * to expand collection parameters into multiple JDBC placeholders before the
 * statement is created.
 * <p>
 * The builder exposes the same {@code set*} methods as {@link NamedPreparedStatement}
 * plus {@link #setCollection(String, Collection)} for {@code IN (...)} style
 * bindings. Scalar values are bound with the same JDBC setter semantics as
 * {@link NamedPreparedStatement} when {@link #prepare(Connection)} is called.
 * <p>
 * Collection parameters expand each SQL occurrence into a comma-separated list
 * of positional placeholders. For example, {@code WHERE id IN (:ids)} with three
 * values becomes {@code WHERE id IN (?,?,?)}. If the same collection parameter
 * appears more than once, each occurrence is expanded independently.
 * <p>
 * Use {@link #resolve()} to inspect JDBC-ready SQL and bind values without
 * creating a {@link PreparedStatement}. By default colon-prefixed parameters
 * ({@code :name}) are parsed with {@link ColonPrefixSqlParser}; a custom
 * {@link SqlParser} can be supplied via {@link #of(String, SqlParser)}.
 *
 * @see NamedPreparedStatement
 * @see ColonPrefixSqlParser
 */
@SuppressWarnings({"UnusedReturnValue"})
public final class NamedQuery {
    private static final SqlParser DEFAULT_SQL_PARSER = new ColonPrefixSqlParser();

    private final String sql;
    private final SqlParser sqlParser;
    private final Map<String, Binding> bindings = new LinkedHashMap<>();
    private ParsedSql parsedSql;

    private NamedQuery(String sql, SqlParser sqlParser) {
        this.sql = Objects.requireNonNull(sql, "sql");
        this.sqlParser = Objects.requireNonNull(sqlParser, "sqlParser");
    }

    /**
     * Creates a query builder with colon-prefixed named parameters.
     */
    public static NamedQuery of(String sql) {
        return new NamedQuery(sql, DEFAULT_SQL_PARSER);
    }

    /**
     * Creates a query builder with a custom SQL parser.
     */
    public static NamedQuery of(String sql, SqlParser sqlParser) {
        return new NamedQuery(sql, sqlParser);
    }

    /**
     * Returns the original SQL text supplied to this builder.
     */
    public String getSql() {
        return sql;
    }

    public NamedQuery setObject(String name, Object value) {
        return bindScalar(name, (statement, index) -> statement.setObject(index, value), value);
    }

    public NamedQuery setInt(String name, int value) {
        return bindByIndex(name, (statement, index) -> statement.setInt(index, value), value);
    }

    public NamedQuery setLong(String name, long value) {
        return bindByIndex(name, (statement, index) -> statement.setLong(index, value), value);
    }

    public NamedQuery setString(String name, String value) {
        return bindByIndex(name, (statement, index) -> statement.setString(index, value), value);
    }

    public NamedQuery setBoolean(String name, boolean value) {
        return bindByIndex(name, (statement, index) -> statement.setBoolean(index, value), value);
    }

    public NamedQuery setByte(String name, byte value) {
        return bindByIndex(name, (statement, index) -> statement.setByte(index, value), value);
    }

    public NamedQuery setShort(String name, short value) {
        return bindByIndex(name, (statement, index) -> statement.setShort(index, value), value);
    }

    public NamedQuery setFloat(String name, float value) {
        return bindByIndex(name, (statement, index) -> statement.setFloat(index, value), value);
    }

    public NamedQuery setDouble(String name, double value) {
        return bindByIndex(name, (statement, index) -> statement.setDouble(index, value), value);
    }

    public NamedQuery setBigDecimal(String name, BigDecimal value) {
        return bindByIndex(name, (statement, index) -> statement.setBigDecimal(index, value), value);
    }

    public NamedQuery setBytes(String name, byte[] value) {
        return bindByIndex(name, (statement, index) -> statement.setBytes(index, value), value);
    }

    public NamedQuery setDate(String name, Date value) {
        return bindByIndex(name, (statement, index) -> statement.setDate(index, value), value);
    }

    public NamedQuery setDate(String name, Date value, java.util.Calendar cal) {
        return bindByIndex(name, (statement, index) -> statement.setDate(index, value, cal), value);
    }

    public NamedQuery setTime(String name, Time value) {
        return bindByIndex(name, (statement, index) -> statement.setTime(index, value), value);
    }

    public NamedQuery setTime(String name, Time value, java.util.Calendar cal) {
        return bindByIndex(name, (statement, index) -> statement.setTime(index, value, cal), value);
    }

    public NamedQuery setTimestamp(String name, Timestamp value) {
        return bindByIndex(name, (statement, index) -> statement.setTimestamp(index, value), value);
    }

    public NamedQuery setTimestamp(String name, Timestamp value, java.util.Calendar cal) {
        return bindByIndex(name, (statement, index) -> statement.setTimestamp(index, value, cal), value);
    }

    public NamedQuery setNull(String name) {
        return setNull(name, Types.NULL);
    }

    public NamedQuery setNull(String name, int sqlType) {
        return bindByIndex(name, (statement, index) -> statement.setNull(index, sqlType), null);
    }

    public NamedQuery setNull(String name, int sqlType, String typeName) {
        return bindByIndex(name, (statement, index) -> statement.setNull(index, sqlType, typeName), null);
    }

    public NamedQuery setObject(String name, Object value, int targetSqlType) {
        return bindByIndex(name, (statement, index) -> statement.setObject(index, value, targetSqlType), value);
    }

    public NamedQuery setObject(String name, Object value, int targetSqlType, int scaleOrLength) {
        return bindByIndex(name, (statement, index) -> statement.setObject(index, value, targetSqlType, scaleOrLength), value);
    }

    public NamedQuery setObject(String name, Object value, SQLType targetSqlType) {
        return bindByIndex(name, (statement, index) -> statement.setObject(index, value, targetSqlType), value);
    }

    public NamedQuery setObject(String name, Object value, SQLType targetSqlType, int scaleOrLength) {
        return bindByIndex(name, (statement, index) -> statement.setObject(index, value, targetSqlType, scaleOrLength), value);
    }

    public NamedQuery setArray(String name, Array value) {
        return bindByIndex(name, (statement, index) -> statement.setArray(index, value), value);
    }

    public NamedQuery setRef(String name, Ref value) {
        return bindByIndex(name, (statement, index) -> statement.setRef(index, value), value);
    }

    public NamedQuery setURL(String name, URL value) {
        return bindByIndex(name, (statement, index) -> statement.setURL(index, value), value);
    }

    public NamedQuery setRowId(String name, RowId value) {
        return bindByIndex(name, (statement, index) -> statement.setRowId(index, value), value);
    }

    public NamedQuery setSQLXML(String name, SQLXML value) {
        return bindByIndex(name, (statement, index) -> statement.setSQLXML(index, value), value);
    }

    public NamedQuery setBlob(String name, Blob value) {
        return bindByIndex(name, (statement, index) -> statement.setBlob(index, value), value);
    }

    public NamedQuery setBlob(String name, InputStream inputStream) {
        return bindByIndex(name, (statement, index) -> statement.setBlob(index, inputStream), inputStream);
    }

    public NamedQuery setBlob(String name, InputStream inputStream, long length) {
        return bindByIndex(name, (statement, index) -> statement.setBlob(index, inputStream, length), inputStream);
    }

    public NamedQuery setClob(String name, Clob value) {
        return bindByIndex(name, (statement, index) -> statement.setClob(index, value), value);
    }

    public NamedQuery setClob(String name, Reader reader) {
        return bindByIndex(name, (statement, index) -> statement.setClob(index, reader), reader);
    }

    public NamedQuery setClob(String name, Reader reader, long length) {
        return bindByIndex(name, (statement, index) -> statement.setClob(index, reader, length), reader);
    }

    public NamedQuery setNClob(String name, NClob value) {
        return bindByIndex(name, (statement, index) -> statement.setNClob(index, value), value);
    }

    public NamedQuery setNClob(String name, Reader reader) {
        return bindByIndex(name, (statement, index) -> statement.setNClob(index, reader), reader);
    }

    public NamedQuery setNClob(String name, Reader reader, long length) {
        return bindByIndex(name, (statement, index) -> statement.setNClob(index, reader, length), reader);
    }

    public NamedQuery setAsciiStream(String name, InputStream value) {
        return bindByIndex(name, (statement, index) -> statement.setAsciiStream(index, value), value);
    }

    public NamedQuery setAsciiStream(String name, InputStream value, int length) {
        return bindByIndex(name, (statement, index) -> statement.setAsciiStream(index, value, length), value);
    }

    public NamedQuery setAsciiStream(String name, InputStream value, long length) {
        return bindByIndex(name, (statement, index) -> statement.setAsciiStream(index, value, length), value);
    }

    public NamedQuery setBinaryStream(String name, InputStream value) {
        return bindByIndex(name, (statement, index) -> statement.setBinaryStream(index, value), value);
    }

    public NamedQuery setBinaryStream(String name, InputStream value, int length) {
        return bindByIndex(name, (statement, index) -> statement.setBinaryStream(index, value, length), value);
    }

    public NamedQuery setBinaryStream(String name, InputStream value, long length) {
        return bindByIndex(name, (statement, index) -> statement.setBinaryStream(index, value, length), value);
    }

    public NamedQuery setCharacterStream(String name, Reader reader) {
        return bindByIndex(name, (statement, index) -> statement.setCharacterStream(index, reader), reader);
    }

    public NamedQuery setCharacterStream(String name, Reader reader, int length) {
        return bindByIndex(name, (statement, index) -> statement.setCharacterStream(index, reader, length), reader);
    }

    public NamedQuery setCharacterStream(String name, Reader reader, long length) {
        return bindByIndex(name, (statement, index) -> statement.setCharacterStream(index, reader, length), reader);
    }

    public NamedQuery setNCharacterStream(String name, Reader value) {
        return bindByIndex(name, (statement, index) -> statement.setNCharacterStream(index, value), value);
    }

    public NamedQuery setNCharacterStream(String name, Reader value, long length) {
        return bindByIndex(name, (statement, index) -> statement.setNCharacterStream(index, value, length), value);
    }

    public NamedQuery setNString(String name, String value) {
        return bindByIndex(name, (statement, index) -> statement.setNString(index, value), value);
    }

    /**
     * Binds a collection value, expanding each SQL occurrence into multiple
     * positional placeholders bound with {@link PreparedStatement#setObject(int, Object)}.
     *
     * @throws IllegalArgumentException when the collection is empty
     */
    public NamedQuery setCollection(String name, Collection<?> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Collection parameter cannot be empty: " + name);
        }
        List<Object> copiedValues = new ArrayList<>(values);
        bind(name, new CollectionBinding(copiedValues));
        return this;
    }

    /**
     * Binds all map entries using {@link #setObject(String, Object)}.
     */
    public NamedQuery setAll(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return this;
        }
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            setObject(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * Removes the binding for {@code name}, if present.
     */
    public NamedQuery clearBinding(String name) {
        bindings.remove(Objects.requireNonNull(name, "name"));
        return this;
    }

    /**
     * Removes all parameter bindings.
     */
    public NamedQuery clearBindings() {
        bindings.clear();
        return this;
    }

    /**
     * Resolves named parameters to JDBC SQL and positional values without
     * creating a {@link PreparedStatement}.
     *
     * @throws IllegalStateException when required named parameters are not bound
     */
    public ResolvedQuery resolve() {
        ExpandedQuery expanded = expand();
        return new ResolvedQuery(expanded.sql(), expanded.resolveValues());
    }

    /**
     * Resolves bindings and creates a fully bound {@link NamedPreparedStatement}.
     */
    public NamedPreparedStatement prepare(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        ExpandedQuery expanded = expand();
        NamedPreparedStatement statement = NamedPreparedStatement.fromExpanded(
                connection,
                expanded.sql(),
                expanded.namedIndexes());
        for (Map.Entry<String, Binding> entry : bindings.entrySet()) {
            entry.getValue().apply(statement, entry.getKey());
        }
        return statement;
    }

    private NamedQuery bindByIndex(String name, IndexStatementBinder binder, Object resolveValue) {
        return bindScalar(name, binder, resolveValue);
    }

    private NamedQuery bindScalar(String name, IndexStatementBinder binder, Object resolveValue) {
        bind(name, new ScalarBinding(binder, resolveValue));
        return this;
    }

    private void bind(String name, Binding binding) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(binding, "binding");
        if (bindings.containsKey(name)) {
            throw new IllegalArgumentException("Parameter already bound: " + name);
        }
        ensureKnownParameter(name);
        bindings.put(name, binding);
    }

    private ExpandedQuery expand() {
        ParsedSql parsed = parsedSql();
        List<String> parameterOrder = parsed.parameters().parameterNames();
        ensureAllNamedParametersAreBound(parameterOrder);

        String jdbcSql = parsed.sql();
        StringBuilder expandedSql = new StringBuilder(jdbcSql.length() + 16);
        LinkedMultiValueHashMap<String, Integer> namedIndexes = new LinkedMultiValueHashMap<>();
        List<Object> resolveValues = new ArrayList<>(parameterOrder.size());
        int searchFrom = 0;
        int nextIndex = 1;

        for (String name : parameterOrder) {
            int questionMarkIndex = jdbcSql.indexOf(ParsedSql.POSITIONAL_PARAM, searchFrom);
            if (questionMarkIndex < 0) {
                throw new IllegalStateException("Mismatch between parsed SQL and parameter metadata");
            }
            expandedSql.append(jdbcSql, searchFrom, questionMarkIndex);

            Binding binding = bindings.get(name);
            if (binding instanceof CollectionBinding collectionBinding) {
                nextIndex = appendCollectionPlaceholders(
                        expandedSql,
                        namedIndexes,
                        resolveValues,
                        name,
                        collectionBinding.values(),
                        nextIndex);
            } else if (binding instanceof ScalarBinding scalarBinding) {
                expandedSql.append(ParsedSql.POSITIONAL_PARAM);
                namedIndexes.add(name, nextIndex++);
                scalarBinding.addResolveValue(resolveValues);
            } else {
                throw new IllegalStateException("Unbound named parameter: " + name);
            }

            searchFrom = questionMarkIndex + 1;
        }

        expandedSql.append(jdbcSql.substring(searchFrom));

        return new ExpandedQuery(expandedSql.toString(), namedIndexes.toUnmodifiableMap(), Collections.unmodifiableList(resolveValues));
    }

    private ParsedSql parsedSql() {
        if (parsedSql == null) {
            parsedSql = sqlParser.parse(sql);
        }
        return parsedSql;
    }

    private Set<String> requiredParameterNames(List<String> parameterOrder) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(parameterOrder));
    }

    private void ensureKnownParameter(String name) {
        if (!requiredParameterNames(parsedSql().parameters().parameterNames()).contains(name)) {
            throw new IllegalArgumentException("Unknown named parameter: " + name);
        }
    }

    private void ensureAllNamedParametersAreBound(List<String> parameterOrder) {
        List<String> missing = new ArrayList<>();
        for (String name : requiredParameterNames(parameterOrder)) {
            if (!bindings.containsKey(name)) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Not all named parameters are bound: " + missing);
        }
    }

    private static int appendCollectionPlaceholders(
            StringBuilder expandedSql,
            LinkedMultiValueHashMap<String, Integer> namedIndexes,
            List<Object> resolveValues,
            String name,
            List<Object> collectionValues,
            int nextIndex) {
        if (collectionValues.isEmpty()) {
            throw new IllegalArgumentException("Collection parameter cannot be empty: " + name);
        }
        StringJoiner placeholders = new StringJoiner(",");
        for (Object value : collectionValues) {
            placeholders.add(ParsedSql.POSITIONAL_PARAM);
            namedIndexes.add(name, nextIndex++);
            resolveValues.add(value);
        }
        expandedSql.append(placeholders);
        return nextIndex;
    }

    /**
     * JDBC-ready SQL and positional bind values produced by {@link #resolve()}.
     * Values are available for {@link #setObject(String, Object)} and {@link #setCollection(String, Collection)}.
     *
     * @param sql    JDBC SQL with positional placeholders
     * @param values bind values in placeholder order
     */
    public record ResolvedQuery(String sql, List<Object> values) {
    }

    private record ExpandedQuery(String sql, Map<String, List<Integer>> namedIndexes, List<Object> resolveValues) {
    }

    @FunctionalInterface
    private interface IndexStatementBinder {
        void bind(PreparedStatement statement, int index) throws SQLException;
    }

    private sealed interface Binding permits ScalarBinding, CollectionBinding {
        void apply(NamedPreparedStatement statement, String name) throws SQLException;
    }

    private record ScalarBinding(IndexStatementBinder binder, Object resolveValue) implements Binding {
        @Override
        public void apply(NamedPreparedStatement statement, String name) throws SQLException {
            statement.applyByIndex(name, index -> binder.bind(statement.unwrap(), index));
        }

        private void addResolveValue(List<Object> resolveValues) {
            resolveValues.add(resolveValue);
        }
    }

    private record CollectionBinding(List<Object> values) implements Binding {
        @Override
        public void apply(NamedPreparedStatement statement, String name) throws SQLException {
            PreparedStatement preparedStatement = statement.unwrap();
            List<Integer> indexes = statement.bindingIndexesFor(name);
            int valuesPerOccurrence = values.size();
            for (int i = 0; i < indexes.size(); i++) {
                int index = indexes.get(i);
                Object value = values.get(i % valuesPerOccurrence);
                if (value == null) {
                    preparedStatement.setNull(index, Types.NULL);
                } else {
                    preparedStatement.setObject(index, value);
                }
            }
            statement.markBound(name);
        }
    }
}
