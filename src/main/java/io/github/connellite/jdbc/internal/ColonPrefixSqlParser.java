package io.github.connellite.jdbc.internal;

@SuppressWarnings("JavadocLinkAsPlainText")
public final class ColonPrefixSqlParser extends CachingSqlParser {
    /**
     * SQL parser which recognizes named parameter tokens of the form {@code :tokenName}.
     *
     * @param rawName parameter name without {@code :}
     * @return parser-specific name with {@code :} prefix
     * <p>
     * Jdbi reference:
     * https://github.com/jdbi/jdbi/blob/6e959ba70365fb0e15f19f39e8b2bf32d4998b6c/core/src/main/java/org/jdbi/v3/core/statement/ColonPrefixSqlParser.java
     */
    @Override
    public String nameParameter(String rawName) {
        return ":" + rawName;
    }

    /**
     * Parses SQL into SQL text + parameter metadata by token stream,
     * preserving comments/literals and converting named parameters to {@code ?}.
     *
     * @param sql SQL to parse
     * @return parsed SQL
     * <p>
     * Jdbi reference:
     * https://github.com/jdbi/jdbi/blob/6e959ba70365fb0e15f19f39e8b2bf32d4998b6c/core/src/main/java/org/jdbi/v3/core/statement/ColonPrefixSqlParser.java
     */
    @Override
    ParsedSql internalParse(String sql) {
        ParsedSql.Builder builder = ParsedSql.builder();
        ColonStatementLexer lexer = new ColonStatementLexer(sql);
        Token t = lexer.nextToken();
        while (t.type() != Token.EOF) {
            switch (t.type()) {
                case Token.COMMENT:
                case Token.LITERAL:
                case Token.QUOTED_TEXT:
                case Token.DOUBLE_QUOTED_TEXT:
                    builder.append(t.text());
                    break;
                case Token.NAMED_PARAM:
                    builder.appendNamedParameter(t.text().substring(1));
                    break;
                case Token.POSITIONAL_PARAM:
                    builder.appendPositionalParameter();
                    break;
                case Token.ESCAPED_TEXT:
                    builder.append(t.text().substring(1));
                    break;
                default:
                    break;
            }
            t = lexer.nextToken();
        }
        return builder.build();
    }
}
