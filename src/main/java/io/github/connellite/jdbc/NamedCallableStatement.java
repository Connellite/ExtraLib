package io.github.connellite.jdbc;

import io.github.connellite.collections.LinkedMultiValueHashMap;
import io.github.connellite.jdbc.internal.BeanBindSource;
import io.github.connellite.jdbc.internal.PreparedStatementCreator;
import io.github.connellite.jdbc.internal.ThrowingConsumer;
import io.github.connellite.jdbc.parser.ColonPrefixSqlParser;
import io.github.connellite.jdbc.parser.ParsedParameters;
import io.github.connellite.jdbc.parser.ParsedSql;
import io.github.connellite.jdbc.parser.SqlParser;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * CallableStatement wrapper with named parameters (e.g. {@code :id} inside {@code {call ...}}).
 * Internally converts the call text to positional placeholders ({@code ?}) and binds by index,
 * so drivers that reject {@link CallableStatement} named-parameter methods still work.
 * A function return is named too: {@code {:ret = call func(:id)}} becomes {@code {? = call func(?)}}.
 */
@SuppressWarnings({"UnusedReturnValue"})
public final class NamedCallableStatement implements AutoCloseable {
    private static final SqlParser SQL_PARSER = new ColonPrefixSqlParser();

    private final CallableStatement statement;
    private final ParsedSql parsedSql;
    private final Map<String, List<Integer>> namedIndexes;
    private final Set<String> boundValues = new HashSet<>();
    private final Set<String> registeredOut = new HashSet<>();

    /**
     * Creates a named call and compiles it to JDBC positional placeholders.
     *
     * @param connection JDBC connection used to prepare the call
     * @param sql call text with named parameters (for example {@code {call proc(:id)}})
     */
    public NamedCallableStatement(Connection connection, String sql) throws SQLException {
        this(connection, sql, Connection::prepareCall);
    }

    /**
     * Creates a named call using a custom SQL parser.
     */
    public NamedCallableStatement(Connection connection, String sql, SqlParser sqlParser) throws SQLException {
        this(connection, sql, sqlParser, Connection::prepareCall);
    }

    /**
     * Creates a named call with a target result-set type and concurrency.
     */
    public NamedCallableStatement(Connection connection, String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        this(connection, sql, (conn, parsed) -> conn.prepareCall(parsed, resultSetType, resultSetConcurrency));
    }

    /**
     * Creates a named call with a custom parser, target result-set type and concurrency.
     */
    public NamedCallableStatement(Connection connection, String sql, int resultSetType, int resultSetConcurrency, SqlParser sqlParser) throws SQLException {
        this(connection, sql, sqlParser, (conn, parsed) -> conn.prepareCall(parsed, resultSetType, resultSetConcurrency));
    }

    /**
     * Creates a named call with result-set type, concurrency and holdability.
     */
    public NamedCallableStatement(Connection connection, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        this(connection, sql, (conn, parsed) -> conn.prepareCall(parsed, resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    /**
     * Creates a named call with a custom parser, result-set type, concurrency and holdability.
     */
    public NamedCallableStatement(Connection connection, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability, SqlParser sqlParser) throws SQLException {
        this(connection, sql, sqlParser, (conn, parsed) -> conn.prepareCall(parsed, resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    private NamedCallableStatement(Connection connection, String sql, PreparedStatementCreator statementCreator) throws SQLException {
        this(connection, sql, SQL_PARSER, statementCreator);
    }

    private NamedCallableStatement(Connection connection, String sql, SqlParser sqlParser, PreparedStatementCreator statementCreator) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(sqlParser, "sqlParser");
        Objects.requireNonNull(statementCreator, "statementCreator");
        this.parsedSql = sqlParser.parse(sql);
        this.namedIndexes = indexNamedParameters(parsedSql.parameters());
        PreparedStatement created = statementCreator.create(connection, parsedSql.sql());
        if (!(created instanceof CallableStatement callable)) {
            if (created != null) {
                created.close();
            }
            throw new SQLException("PreparedStatementCreator must return a CallableStatement");
        }
        this.statement = callable;
    }

    private NamedCallableStatement(CallableStatement statement, ParsedSql parsedSql, Map<String, List<Integer>> namedIndexes) {
        this.statement = statement;
        this.parsedSql = parsedSql;
        this.namedIndexes = namedIndexes;
    }

    /**
     * Creates a call from expanded JDBC SQL and a named-parameter index map.
     * Used by {@link NamedQuery#prepareCall(Connection)}.
     */
    static NamedCallableStatement fromExpanded(Connection connection, String jdbcSql, Map<String, List<Integer>> namedIndexes) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(jdbcSql, "jdbcSql");
        Objects.requireNonNull(namedIndexes, "namedIndexes");

        int parameterCount = namedIndexes.values().stream().mapToInt(List::size).sum();
        CallableStatement statement = connection.prepareCall(jdbcSql);
        ParsedSql parsedSql = ParsedSql.of(jdbcSql, ParsedParameters.positional(parameterCount));
        return new NamedCallableStatement(statement, parsedSql, namedIndexes);
    }

    NamedCallableStatement applyByIndex(String name, ThrowingConsumer<Integer> setter) throws SQLException {
        return setByIndex(name, setter);
    }

    List<Integer> bindingIndexesFor(String name) {
        return indexesFor(name);
    }

    void markBound(String name) {
        boundValues.add(name);
    }

    /**
     * Static factory for {@link NamedCallableStatement}.
     */
    public static NamedCallableStatement of(Connection connection, String sql) throws SQLException {
        return new NamedCallableStatement(connection, sql);
    }

    /**
     * Static factory for {@link NamedCallableStatement} with a custom SQL parser.
     */
    public static NamedCallableStatement of(Connection connection, String sql, SqlParser sqlParser) throws SQLException {
        return new NamedCallableStatement(connection, sql, sqlParser);
    }

    /**
     * Static factory for {@link NamedCallableStatement} with result-set type and concurrency.
     */
    public static NamedCallableStatement of(Connection connection, String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return new NamedCallableStatement(connection, sql, resultSetType, resultSetConcurrency);
    }

    /**
     * Static factory for {@link NamedCallableStatement} with result-set type, concurrency and a custom SQL parser.
     */
    public static NamedCallableStatement of(Connection connection, String sql, int resultSetType, int resultSetConcurrency, SqlParser sqlParser) throws SQLException {
        return new NamedCallableStatement(connection, sql, resultSetType, resultSetConcurrency, sqlParser);
    }

    /**
     * Static factory for {@link NamedCallableStatement} with result-set type, concurrency and holdability.
     */
    public static NamedCallableStatement of(Connection connection, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return new NamedCallableStatement(connection, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    /**
     * Static factory for {@link NamedCallableStatement} with result-set type, concurrency, holdability and a custom SQL parser.
     */
    public static NamedCallableStatement of(Connection connection, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability, SqlParser sqlParser) throws SQLException {
        return new NamedCallableStatement(connection, sql, resultSetType, resultSetConcurrency, resultSetHoldability, sqlParser);
    }

    /**
     * Binds object value to all occurrences of named parameter.
     */
    public NamedCallableStatement setObject(String name, Object value) throws SQLException {
        return setByIndex(name, index -> statement.setObject(index, value));
    }

    /**
     * Binds int value to all occurrences of named parameter.
     */
    public NamedCallableStatement setInt(String name, int value) throws SQLException {
        return setByIndex(name, index -> statement.setInt(index, value));
    }

    /**
     * Binds long value to all occurrences of named parameter.
     */
    public NamedCallableStatement setLong(String name, long value) throws SQLException {
        return setByIndex(name, index -> statement.setLong(index, value));
    }

    /**
     * Binds string value to all occurrences of named parameter.
     */
    public NamedCallableStatement setString(String name, String value) throws SQLException {
        return setByIndex(name, index -> statement.setString(index, value));
    }

    /**
     * Binds boolean value to all occurrences of named parameter.
     */
    public NamedCallableStatement setBoolean(String name, boolean value) throws SQLException {
        return setByIndex(name, index -> statement.setBoolean(index, value));
    }

    /**
     * Binds byte value to all occurrences of named parameter.
     */
    public NamedCallableStatement setByte(String name, byte value) throws SQLException {
        return setByIndex(name, index -> statement.setByte(index, value));
    }

    /**
     * Binds short value to all occurrences of named parameter.
     */
    public NamedCallableStatement setShort(String name, short value) throws SQLException {
        return setByIndex(name, index -> statement.setShort(index, value));
    }

    /**
     * Binds float value to all occurrences of named parameter.
     */
    public NamedCallableStatement setFloat(String name, float value) throws SQLException {
        return setByIndex(name, index -> statement.setFloat(index, value));
    }

    /**
     * Binds double value to all occurrences of named parameter.
     */
    public NamedCallableStatement setDouble(String name, double value) throws SQLException {
        return setByIndex(name, index -> statement.setDouble(index, value));
    }

    /**
     * Binds {@link BigDecimal} value to all occurrences of named parameter.
     */
    public NamedCallableStatement setBigDecimal(String name, BigDecimal value) throws SQLException {
        return setByIndex(name, index -> statement.setBigDecimal(index, value));
    }

    /**
     * Binds byte array value to all occurrences of named parameter.
     */
    public NamedCallableStatement setBytes(String name, byte[] value) throws SQLException {
        return setByIndex(name, index -> statement.setBytes(index, value));
    }

    /**
     * Binds {@link Date} value to all occurrences of named parameter.
     */
    public NamedCallableStatement setDate(String name, Date value) throws SQLException {
        return setByIndex(name, index -> statement.setDate(index, value));
    }

    /**
     * Binds {@link Date} value with calendar to all occurrences of named parameter.
     */
    public NamedCallableStatement setDate(String name, Date value, Calendar cal) throws SQLException {
        return setByIndex(name, index -> statement.setDate(index, value, cal));
    }

    /**
     * Binds {@link Time} value to all occurrences of named parameter.
     */
    public NamedCallableStatement setTime(String name, Time value) throws SQLException {
        return setByIndex(name, index -> statement.setTime(index, value));
    }

    /**
     * Binds {@link Time} value with calendar to all occurrences of named parameter.
     */
    public NamedCallableStatement setTime(String name, Time value, Calendar cal) throws SQLException {
        return setByIndex(name, index -> statement.setTime(index, value, cal));
    }

    /**
     * Binds {@link Timestamp} value to all occurrences of named parameter.
     */
    public NamedCallableStatement setTimestamp(String name, Timestamp value) throws SQLException {
        return setByIndex(name, index -> statement.setTimestamp(index, value));
    }

    /**
     * Binds {@link Timestamp} value with calendar to all occurrences of named parameter.
     */
    public NamedCallableStatement setTimestamp(String name, Timestamp value, Calendar cal) throws SQLException {
        return setByIndex(name, index -> statement.setTimestamp(index, value, cal));
    }

    /**
     * Binds SQL NULL of generic type {@link Types#NULL}.
     */
    public NamedCallableStatement setNull(String name) throws SQLException {
        return setNull(name, Types.NULL);
    }

    /**
     * Binds SQL NULL with explicit JDBC type.
     */
    public NamedCallableStatement setNull(String name, int sqlType) throws SQLException {
        return setByIndex(name, index -> statement.setNull(index, sqlType));
    }

    /**
     * Binds SQL NULL with explicit JDBC type and database-specific type name.
     */
    public NamedCallableStatement setNull(String name, int sqlType, String typeName) throws SQLException {
        return setByIndex(name, index -> statement.setNull(index, sqlType, typeName));
    }

    /**
     * Binds object with explicit JDBC SQL type.
     */
    public NamedCallableStatement setObject(String name, Object value, int targetSqlType) throws SQLException {
        return setByIndex(name, index -> statement.setObject(index, value, targetSqlType));
    }

    /**
     * Binds object with explicit JDBC SQL type and scale/length.
     */
    public NamedCallableStatement setObject(String name, Object value, int targetSqlType, int scaleOrLength) throws SQLException {
        return setByIndex(name, index -> statement.setObject(index, value, targetSqlType, scaleOrLength));
    }

    /**
     * Binds object with {@link SQLType}.
     */
    public NamedCallableStatement setObject(String name, Object value, SQLType targetSqlType) throws SQLException {
        return setByIndex(name, index -> statement.setObject(index, value, targetSqlType));
    }

    /**
     * Binds object with {@link SQLType} and scale/length.
     */
    public NamedCallableStatement setObject(String name, Object value, SQLType targetSqlType, int scaleOrLength) throws SQLException {
        return setByIndex(name, index -> statement.setObject(index, value, targetSqlType, scaleOrLength));
    }

    /**
     * Binds SQL array value.
     */
    public NamedCallableStatement setArray(String name, Array value) throws SQLException {
        return setByIndex(name, index -> statement.setArray(index, value));
    }

    /**
     * Binds SQL reference value.
     */
    public NamedCallableStatement setRef(String name, Ref value) throws SQLException {
        return setByIndex(name, index -> statement.setRef(index, value));
    }

    /**
     * Binds URL value.
     */
    public NamedCallableStatement setURL(String name, URL value) throws SQLException {
        return setByIndex(name, index -> statement.setURL(index, value));
    }

    /**
     * Binds JDBC row id value.
     */
    public NamedCallableStatement setRowId(String name, RowId value) throws SQLException {
        return setByIndex(name, index -> statement.setRowId(index, value));
    }

    /**
     * Binds SQLXML value.
     */
    public NamedCallableStatement setSQLXML(String name, SQLXML value) throws SQLException {
        return setByIndex(name, index -> statement.setSQLXML(index, value));
    }

    /**
     * Binds {@link Blob} value.
     */
    public NamedCallableStatement setBlob(String name, Blob value) throws SQLException {
        return setByIndex(name, index -> statement.setBlob(index, value));
    }

    /**
     * Binds blob binary stream value.
     */
    public NamedCallableStatement setBlob(String name, InputStream inputStream) throws SQLException {
        return setByIndex(name, index -> statement.setBlob(index, inputStream));
    }

    /**
     * Binds blob binary stream value with explicit length.
     */
    public NamedCallableStatement setBlob(String name, InputStream inputStream, long length) throws SQLException {
        return setByIndex(name, index -> statement.setBlob(index, inputStream, length));
    }

    /**
     * Binds {@link Clob} value.
     */
    public NamedCallableStatement setClob(String name, Clob value) throws SQLException {
        return setByIndex(name, index -> statement.setClob(index, value));
    }

    /**
     * Binds character reader as CLOB.
     */
    public NamedCallableStatement setClob(String name, Reader reader) throws SQLException {
        return setByIndex(name, index -> statement.setClob(index, reader));
    }

    /**
     * Binds character reader as CLOB with explicit length.
     */
    public NamedCallableStatement setClob(String name, Reader reader, long length) throws SQLException {
        return setByIndex(name, index -> statement.setClob(index, reader, length));
    }

    /**
     * Binds {@link NClob} value.
     */
    public NamedCallableStatement setNClob(String name, NClob value) throws SQLException {
        return setByIndex(name, index -> statement.setNClob(index, value));
    }

    /**
     * Binds reader as NCLOB.
     */
    public NamedCallableStatement setNClob(String name, Reader reader) throws SQLException {
        return setByIndex(name, index -> statement.setNClob(index, reader));
    }

    /**
     * Binds reader as NCLOB with explicit length.
     */
    public NamedCallableStatement setNClob(String name, Reader reader, long length) throws SQLException {
        return setByIndex(name, index -> statement.setNClob(index, reader, length));
    }

    /**
     * Binds ASCII stream.
     */
    public NamedCallableStatement setAsciiStream(String name, InputStream value) throws SQLException {
        return setByIndex(name, index -> statement.setAsciiStream(index, value));
    }

    /**
     * Binds ASCII stream with int length.
     */
    public NamedCallableStatement setAsciiStream(String name, InputStream value, int length) throws SQLException {
        return setByIndex(name, index -> statement.setAsciiStream(index, value, length));
    }

    /**
     * Binds ASCII stream with long length.
     */
    public NamedCallableStatement setAsciiStream(String name, InputStream value, long length) throws SQLException {
        return setByIndex(name, index -> statement.setAsciiStream(index, value, length));
    }

    /**
     * Binds binary stream.
     */
    public NamedCallableStatement setBinaryStream(String name, InputStream value) throws SQLException {
        return setByIndex(name, index -> statement.setBinaryStream(index, value));
    }

    /**
     * Binds binary stream with int length.
     */
    public NamedCallableStatement setBinaryStream(String name, InputStream value, int length) throws SQLException {
        return setByIndex(name, index -> statement.setBinaryStream(index, value, length));
    }

    /**
     * Binds binary stream with long length.
     */
    public NamedCallableStatement setBinaryStream(String name, InputStream value, long length) throws SQLException {
        return setByIndex(name, index -> statement.setBinaryStream(index, value, length));
    }

    /**
     * Binds character stream.
     */
    public NamedCallableStatement setCharacterStream(String name, Reader reader) throws SQLException {
        return setByIndex(name, index -> statement.setCharacterStream(index, reader));
    }

    /**
     * Binds character stream with int length.
     */
    public NamedCallableStatement setCharacterStream(String name, Reader reader, int length) throws SQLException {
        return setByIndex(name, index -> statement.setCharacterStream(index, reader, length));
    }

    /**
     * Binds character stream with long length.
     */
    public NamedCallableStatement setCharacterStream(String name, Reader reader, long length) throws SQLException {
        return setByIndex(name, index -> statement.setCharacterStream(index, reader, length));
    }

    /**
     * Binds NCHAR character stream.
     */
    public NamedCallableStatement setNCharacterStream(String name, Reader value) throws SQLException {
        return setByIndex(name, index -> statement.setNCharacterStream(index, value));
    }

    /**
     * Binds NCHAR character stream with explicit length.
     */
    public NamedCallableStatement setNCharacterStream(String name, Reader value, long length) throws SQLException {
        return setByIndex(name, index -> statement.setNCharacterStream(index, value, length));
    }

    /**
     * Binds national character string value.
     */
    public NamedCallableStatement setNString(String name, String value) throws SQLException {
        return setByIndex(name, index -> statement.setNString(index, value));
    }

    /**
     * Binds all entries from map using {@link #setObject(String, Object)}.
     */
    @SuppressWarnings("resource")
    public NamedCallableStatement setAll(Map<String, ?> params) throws SQLException {
        if (params == null || params.isEmpty()) {
            return this;
        }
        for (Map.Entry<String, ?> e : params.entrySet()) {
            if (e.getValue() != null) {
                setObject(e.getKey(), e.getValue());
            } else {
                setNull(e.getKey(), Types.NULL);
            }
        }
        return this;
    }

    /**
     * Binds {@link io.github.connellite.jdbc.annotation.Bind @Bind} members of {@code bean}.
     * <p>
     * Each member is read by reflection. When {@link io.github.connellite.jdbc.annotation.Bind#converter()}
     * is not {@link TypeConverter.DefaultConverter}, {@link TypeConverter#convert(Object)} runs first.
     * A {@code null} result is bound with {@link #setNull(String)}; any other result uses
     * {@link #setObject(String, Object)}. Members whose names are not parameters of this statement
     * are ignored. Collection values are not expanded.
     *
     * @throws SQLException when a converter cannot be created or {@code convert} fails
     * @throws IllegalArgumentException when {@code bean} has no {@code @Bind} members or none of those
     *         names are parameters of this statement
     * @throws IllegalStateException when a member cannot be read
     * @throws NullPointerException when {@code bean} is {@code null}
     */
    @SuppressWarnings("resource")
    public NamedCallableStatement setBean(Object bean) throws SQLException {
        Objects.requireNonNull(bean, "bean");
        for (BeanBindSource.BindValue value : BeanBindSource.read(bean, namedIndexes.keySet())) {
            if (value.value() == null) {
                setNull(value.name());
            } else {
                setObject(value.name(), value.value());
            }
        }
        return this;
    }

    /**
     * Registers every occurrence of {@code name} as an OUT parameter.
     * An INOUT parameter is both registered and set.
     */
    public NamedCallableStatement registerOutParameter(String name, int sqlType) throws SQLException {
        return registerByIndex(name, index -> statement.registerOutParameter(index, sqlType));
    }

    /**
     * Registers every occurrence of {@code name} as an OUT parameter with scale.
     */
    public NamedCallableStatement registerOutParameter(String name, int sqlType, int scale) throws SQLException {
        return registerByIndex(name, index -> statement.registerOutParameter(index, sqlType, scale));
    }

    /**
     * Registers every occurrence of {@code name} as an OUT parameter with a database-specific type name.
     */
    public NamedCallableStatement registerOutParameter(String name, int sqlType, String typeName) throws SQLException {
        return registerByIndex(name, index -> statement.registerOutParameter(index, sqlType, typeName));
    }

    /**
     * Registers every occurrence of {@code name} as an OUT parameter of the given {@link SQLType}.
     */
    public NamedCallableStatement registerOutParameter(String name, SQLType sqlType) throws SQLException {
        return registerByIndex(name, index -> statement.registerOutParameter(index, sqlType));
    }

    /**
     * Registers every occurrence of {@code name} as an OUT parameter of the given {@link SQLType} and scale.
     */
    public NamedCallableStatement registerOutParameter(String name, SQLType sqlType, int scale) throws SQLException {
        return registerByIndex(name, index -> statement.registerOutParameter(index, sqlType, scale));
    }

    /**
     * Registers every occurrence of {@code name} as an OUT parameter of the given {@link SQLType} and type name.
     */
    public NamedCallableStatement registerOutParameter(String name, SQLType sqlType, String typeName) throws SQLException {
        return registerByIndex(name, index -> statement.registerOutParameter(index, sqlType, typeName));
    }

    /**
     * Reads an OUT or INOUT value. {@code name} must occur once in the call text.
     */
    public String getString(String name) throws SQLException {
        return statement.getString(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT boolean. {@code name} must occur once in the call text.
     */
    public boolean getBoolean(String name) throws SQLException {
        return statement.getBoolean(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT byte. {@code name} must occur once in the call text.
     */
    public byte getByte(String name) throws SQLException {
        return statement.getByte(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT short. {@code name} must occur once in the call text.
     */
    public short getShort(String name) throws SQLException {
        return statement.getShort(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT int. {@code name} must occur once in the call text.
     */
    public int getInt(String name) throws SQLException {
        return statement.getInt(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT long. {@code name} must occur once in the call text.
     */
    public long getLong(String name) throws SQLException {
        return statement.getLong(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT float. {@code name} must occur once in the call text.
     */
    public float getFloat(String name) throws SQLException {
        return statement.getFloat(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT double. {@code name} must occur once in the call text.
     */
    public double getDouble(String name) throws SQLException {
        return statement.getDouble(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT {@link BigDecimal}. {@code name} must occur once in the call text.
     */
    public BigDecimal getBigDecimal(String name) throws SQLException {
        return statement.getBigDecimal(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT byte array. {@code name} must occur once in the call text.
     */
    public byte[] getBytes(String name) throws SQLException {
        return statement.getBytes(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT {@link Date}. {@code name} must occur once in the call text.
     */
    public Date getDate(String name) throws SQLException {
        return statement.getDate(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT {@link Date} with calendar. {@code name} must occur once in the call text.
     */
    public Date getDate(String name, Calendar cal) throws SQLException {
        return statement.getDate(singleIndex(name), cal);
    }

    /**
     * Reads an OUT or INOUT {@link Time}. {@code name} must occur once in the call text.
     */
    public Time getTime(String name) throws SQLException {
        return statement.getTime(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT {@link Time} with calendar. {@code name} must occur once in the call text.
     */
    public Time getTime(String name, Calendar cal) throws SQLException {
        return statement.getTime(singleIndex(name), cal);
    }

    /**
     * Reads an OUT or INOUT {@link Timestamp}. {@code name} must occur once in the call text.
     */
    public Timestamp getTimestamp(String name) throws SQLException {
        return statement.getTimestamp(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT {@link Timestamp} with calendar. {@code name} must occur once in the call text.
     */
    public Timestamp getTimestamp(String name, Calendar cal) throws SQLException {
        return statement.getTimestamp(singleIndex(name), cal);
    }

    /**
     * Reads an OUT or INOUT object. {@code name} must occur once in the call text.
     */
    public Object getObject(String name) throws SQLException {
        return statement.getObject(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT object using a SQL-to-Java type map. {@code name} must occur once in the call text.
     */
    public Object getObject(String name, Map<String, Class<?>> map) throws SQLException {
        return statement.getObject(singleIndex(name), map);
    }

    /**
     * Reads an OUT or INOUT value converted to {@code type}. {@code name} must occur once in the call text.
     */
    public <T> T getObject(String name, Class<T> type) throws SQLException {
        return statement.getObject(singleIndex(name), type);
    }

    /**
     * Reads an OUT or INOUT {@link Ref}. {@code name} must occur once in the call text.
     */
    public Ref getRef(String name) throws SQLException {
        return statement.getRef(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT {@link Blob}. {@code name} must occur once in the call text.
     */
    public Blob getBlob(String name) throws SQLException {
        return statement.getBlob(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT {@link Clob}. {@code name} must occur once in the call text.
     */
    public Clob getClob(String name) throws SQLException {
        return statement.getClob(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT {@link Array}. {@code name} must occur once in the call text.
     */
    public Array getArray(String name) throws SQLException {
        return statement.getArray(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT {@link URL}. {@code name} must occur once in the call text.
     */
    public URL getURL(String name) throws SQLException {
        return statement.getURL(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT {@link RowId}. {@code name} must occur once in the call text.
     */
    public RowId getRowId(String name) throws SQLException {
        return statement.getRowId(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT {@link NClob}. {@code name} must occur once in the call text.
     */
    public NClob getNClob(String name) throws SQLException {
        return statement.getNClob(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT {@link SQLXML}. {@code name} must occur once in the call text.
     */
    public SQLXML getSQLXML(String name) throws SQLException {
        return statement.getSQLXML(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT national string. {@code name} must occur once in the call text.
     */
    public String getNString(String name) throws SQLException {
        return statement.getNString(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT character stream. {@code name} must occur once in the call text.
     */
    public Reader getCharacterStream(String name) throws SQLException {
        return statement.getCharacterStream(singleIndex(name));
    }

    /**
     * Reads an OUT or INOUT national character stream. {@code name} must occur once in the call text.
     */
    public Reader getNCharacterStream(String name) throws SQLException {
        return statement.getNCharacterStream(singleIndex(name));
    }

    /**
     * Reports whether the last value read by a {@code get} method was SQL NULL.
     */
    public boolean wasNull() throws SQLException {
        return statement.wasNull();
    }

    /**
     * Clears wrapper binding state, OUT registration bookkeeping, and underlying JDBC parameter values.
     */
    public void clearParameters() throws SQLException {
        boundValues.clear();
        registeredOut.clear();
        statement.clearParameters();
    }

    /**
     * Executes the call after every named parameter has been set, registered, or both.
     *
     * @return same meaning as {@link CallableStatement#execute()}
     */
    public boolean execute() throws SQLException {
        ensureAllNamedParametersAreBound();
        return statement.execute();
    }

    /**
     * Executes the call and returns the first result set, skipping update counts the same way as
     * {@link QueryUtils#selectFromCall(Connection, String, Object...)}.
     * The returned result set does not close this statement, so OUT values can still be read.
     */
    public ResultSet executeQuery() throws SQLException {
        ensureAllNamedParametersAreBound();
        boolean hasResult = statement.execute();
        for (;;) {
            if (hasResult) {
                ResultSet resultSet = statement.getResultSet();
                if (resultSet != null) {
                    return resultSet;
                }
            } else if (statement.getUpdateCount() == -1) {
                break;
            }
            hasResult = statement.getMoreResults();
        }
        throw new SQLException("CallableStatement returned no ResultSet");
    }

    /**
     * Returns the current result set of the underlying call.
     */
    public ResultSet getResultSet() throws SQLException {
        return statement.getResultSet();
    }

    /**
     * Moves to the next result of the underlying call.
     */
    public boolean getMoreResults() throws SQLException {
        return statement.getMoreResults();
    }

    /**
     * Returns the current update count of the underlying call.
     */
    public int getUpdateCount() throws SQLException {
        return statement.getUpdateCount();
    }

    /**
     * Adds the current parameter set to a batch. Every name must be an IN value:
     * OUT and INOUT registration is rejected because batched OUT parameters are not portable.
     */
    public void addBatch() throws SQLException {
        if (!registeredOut.isEmpty()) {
            throw new IllegalStateException("Batch is only supported for IN parameters");
        }
        ensureAllNamedParametersAreSet();
        statement.addBatch();
    }

    /**
     * Executes the JDBC batch on the underlying call.
     */
    public int[] executeBatch() throws SQLException {
        return statement.executeBatch();
    }

    /**
     * Returns the underlying JDBC callable statement.
     */
    public CallableStatement unwrap() {
        return statement;
    }

    /**
     * Names of parameters bound with {@code set} since the last {@link #clearParameters()}.
     */
    public Set<String> getBoundValues() {
        return Collections.unmodifiableSet(boundValues);
    }

    /**
     * Returns parsed call text transformed to JDBC placeholders.
     */
    public String getParsedSql() {
        return parsedSql.sql();
    }

    /**
     * Returns parameter names in positional order (including repeated occurrences).
     */
    public List<String> getParameterOrder() {
        return parsedSql.parameters().parameterNames();
    }

    private NamedCallableStatement setByIndex(String name, ThrowingConsumer<Integer> setter) throws SQLException {
        List<Integer> indexes = indexesFor(name);
        for (int index : indexes) {
            setter.accept(index);
        }
        boundValues.add(name);
        return this;
    }

    private NamedCallableStatement registerByIndex(String name, ThrowingConsumer<Integer> registrar) throws SQLException {
        List<Integer> indexes = indexesFor(name);
        for (int index : indexes) {
            registrar.accept(index);
        }
        registeredOut.add(name);
        return this;
    }

    private void ensureAllNamedParametersAreBound() {
        List<String> missing = new ArrayList<>();
        for (String name : namedIndexes.keySet()) {
            if (!boundValues.contains(name) && !registeredOut.contains(name)) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Not all named parameters are bound: " + missing);
        }
    }

    private void ensureAllNamedParametersAreSet() {
        List<String> missing = new ArrayList<>();
        for (String name : namedIndexes.keySet()) {
            if (!boundValues.contains(name)) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Not all named parameters are bound: " + missing);
        }
    }

    private int singleIndex(String name) {
        List<Integer> indexes = indexesFor(name);
        if (indexes.size() != 1) {
            throw new IllegalArgumentException("Named parameter occurs more than once: " + name);
        }
        return indexes.get(0);
    }

    private List<Integer> indexesFor(String name) {
        List<Integer> indexes = namedIndexes.get(name);
        if (indexes == null || indexes.isEmpty()) {
            throw new IllegalArgumentException("Unknown named parameter: " + name);
        }
        return indexes;
    }

    private static Map<String, List<Integer>> indexNamedParameters(ParsedParameters parameters) {
        if (parameters.positional()) {
            return Collections.emptyMap();
        }

        LinkedMultiValueHashMap<String, Integer> indexes = new LinkedMultiValueHashMap<>();
        List<String> parameterNames = parameters.parameterNames();
        for (int i = 0; i < parameterNames.size(); i++) {
            indexes.add(parameterNames.get(i), i + 1);
        }

        return indexes.toUnmodifiableMap();
    }

    @Override
    public void close() throws SQLException {
        if (statement != null) {
            statement.close();
        }
    }
}
