package io.github.connellite.jdbc.parser;

public final class PBraceTokenSqlParser extends CachingSqlParser {
    /**
     * SQL parser which recognizes named parameter tokens of the form {@code $P{tokenName}}.
     *
     * @param rawName parameter name without the {@code $P{...}} wrapper
     * @return parser-specific name wrapped as {@code $P{tokenName}}
     */
    @Override
    public String nameParameter(String rawName) {
        return "$P{" + rawName + "}";
    }

    /**
     * Parses SQL into SQL text + parameter metadata by token stream,
     * preserving comments/literals and converting {@code $P{...}} named parameters to {@code ?}.
     *
     * @param sql SQL to parse
     * @return parsed SQL
     */
    @Override
    ParsedSql internalParse(String sql) {
        ParsedSql.Builder builder = ParsedSql.builder();
        PBraceTokenSqlLexer lexer = new PBraceTokenSqlLexer(sql);
        Token t = lexer.nextToken();
        while (t.type() != Token.EOF) {
            switch (t.type()) {
                case Token.COMMENT:
                case Token.LITERAL:
                case Token.QUOTED_TEXT:
                case Token.DOUBLE_QUOTED_TEXT:
                    builder.append(t.text());
                    break;
                case Token.NAMED_PARAM: {
                    String name = t.text();
                    if (name.isEmpty()) {
                        throw new IllegalArgumentException("Parameter name is empty");
                    }
                    builder.appendNamedParameter(name);
                    break;
                }
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
