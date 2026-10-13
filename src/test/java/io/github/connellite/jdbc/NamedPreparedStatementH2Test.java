package io.github.connellite.jdbc;

import io.github.connellite.jdbc.parser.BraceTokenSqlParser;
import io.github.connellite.jdbc.parser.HashPrefixSqlParser;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NamedPreparedStatementH2Test {

    @Test
    void hashPrefixParserIgnoresHashInsideCommentsAndHonorsEscapes() throws Exception {
        try (Connection connection = H2Memory.openMssql()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE hash_sample (\"#escaped\" INT)");
            }
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    connection,
                    "SELECT \\#escaped FROM hash_sample -- #ignored\n WHERE #id = 1",
                    new HashPrefixSqlParser())) {
                assertEquals("SELECT #escaped FROM hash_sample -- #ignored\n WHERE ? = 1", nps.getParsedSql());
                assertEquals(List.of("id"), nps.getParameterOrder());
            }
        }
    }

    /**
     * Escaped {@code \#{...}} stays text, and brace names inside comments are not parameters.
     * Dollar quotes hide that text from H2's JDBC escape translator so the statement prepares.
     */
    @Test
    void braceParserIgnoresHashBraceInsideCommentsAndHonorsEscapes() throws Exception {
        try (Connection connection = H2Memory.openPostgreSql()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    connection,
                    "SELECT $$\\#{escaped}$$ AS e, #{id} AS v -- #{ignored}\n/* #{hidden} */",
                    new BraceTokenSqlParser())) {
                assertEquals("SELECT $$#{escaped}$$ AS e, ? AS v -- #{ignored}\n/* #{hidden} */", nps.getParsedSql());
                assertEquals(List.of("id"), nps.getParameterOrder());
            }
        }
    }

    /**
     * PostgreSQL-style cast ({@code ::type}) is not valid SQLite syntax.
     * H2 in PostgreSQL mode prepares {@code ::int}, and the colon pair stays a cast.
     */
    @Test
    void doubleColonCastIsLiteralNotSecondNamedParameter() throws Exception {
        try (Connection connection = H2Memory.openPostgreSql()) {
            try (NamedPreparedStatement nps = new NamedPreparedStatement(
                    connection,
                    "SELECT :id::int AS v")) {
                assertEquals("SELECT ?::int AS v", nps.getParsedSql());
                assertEquals(List.of("id"), nps.getParameterOrder());
            }
        }
    }
}
