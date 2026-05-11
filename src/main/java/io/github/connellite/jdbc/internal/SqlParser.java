package io.github.connellite.jdbc.internal;

@SuppressWarnings("JavadocLinkAsPlainText")
public interface SqlParser {
    /**
     * Parses the given SQL statement and returns the parsed representation.
     *
     * @param sql the SQL statement to parse
     * @return parsed SQL with JDBC-ready SQL and parameter metadata
     * <p>
     * Jdbi reference:
     * https://github.com/jdbi/jdbi/blob/6e959ba70365fb0e15f19f39e8b2bf32d4998b6c/core/src/main/java/org/jdbi/v3/core/statement/SqlParser.java
     */
    ParsedSql parse(String sql);

    /**
     * Convert a raw parameter name into a name recognized by this parser.
     *
     * @param rawName raw name to transform
     * @return parser-specific parameter name
     * <p>
     * Jdbi reference:
     * https://github.com/jdbi/jdbi/blob/6e959ba70365fb0e15f19f39e8b2bf32d4998b6c/core/src/main/java/org/jdbi/v3/core/statement/SqlParser.java
     */
    String nameParameter(String rawName);
}
