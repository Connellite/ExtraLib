package io.github.connellite.jdbc.internal;

@SuppressWarnings("JavadocLinkAsPlainText")
public final class ColonStatementLexer {
    private final String sql;
    private int pos;

    public ColonStatementLexer(String sql) {
        this.sql = sql;
    }

    /**
     * Returns next lexer token according to Jdbi's colon statement lexer rules.
     *
     * @return next token or {@link Token#EOF}
     * <p>
     * Jdbi reference:
     * https://github.com/jdbi/jdbi/blob/6e959ba70365fb0e15f19f39e8b2bf32d4998b6c/core/src/main/antlr4/org/jdbi/v3/core/internal/lexer/ColonStatementLexer.g4
     */
    public Token nextToken() {
        if (pos >= sql.length()) {
            return new Token(Token.EOF, "");
        }

        char c = sql.charAt(pos);
        char next = lookAhead(1);

        if (c == '/' && next == '*') {
            return readBlockComment();
        }
        if (c == '-' && next == '-') {
            return readLineComment();
        }
        if (c == '/' && next == '/') {
            return readLineComment();
        }
        if (c == '\'') {
            return readQuotedText();
        }
        if (c == '"') {
            return readDoubleQuotedText();
        }
        if (c == '\\' && pos + 1 < sql.length()) {
            String text = sql.substring(pos, pos + 2);
            pos += 2;
            return new Token(Token.ESCAPED_TEXT, text);
        }
        if (c == ':' && next == ':') {
            pos += 2;
            return new Token(Token.LITERAL, "::");
        }
        if (c == ':' && isNameChar(next)) {
            return readNamedParam();
        }
        if (c == '?' && next == '?') {
            pos += 2;
            return new Token(Token.LITERAL, "??");
        }
        if (c == '?') {
            pos++;
            return new Token(Token.POSITIONAL_PARAM, "?");
        }
        return readLiteral();
    }

    private Token readBlockComment() {
        int start = pos;
        pos += 2;
        while (pos < sql.length()) {
            if (sql.charAt(pos) == '*' && lookAhead(1) == '/') {
                pos += 2;
                break;
            }
            pos++;
        }
        return new Token(Token.COMMENT, sql.substring(start, pos));
    }

    private Token readLineComment() {
        int start = pos;
        pos += 2;
        while (pos < sql.length() && sql.charAt(pos) != '\r' && sql.charAt(pos) != '\n') {
            pos++;
        }
        return new Token(Token.COMMENT, sql.substring(start, pos));
    }

    private Token readQuotedText() {
        int start = pos++;
        while (pos < sql.length()) {
            char c = sql.charAt(pos);
            if (c == '\\' && lookAhead(1) == '\'') {
                pos += 2;
            } else if (c == '\'' && lookAhead(1) == '\'') {
                pos += 2;
            } else if (c == '\'') {
                pos++;
                break;
            } else {
                pos++;
            }
        }
        return new Token(Token.QUOTED_TEXT, sql.substring(start, pos));
    }

    private Token readDoubleQuotedText() {
        int start = pos++;
        while (pos < sql.length()) {
            char c = sql.charAt(pos++);
            if (c == '"') {
                break;
            }
        }
        return new Token(Token.DOUBLE_QUOTED_TEXT, sql.substring(start, pos));
    }

    private Token readNamedParam() {
        int start = pos++;
        while (pos < sql.length()) {
            if (sql.charAt(pos) == '?' && lookAhead(1) == '.') {
                pos += 2;
            } else if (isNameChar(sql.charAt(pos))) {
                pos++;
            } else {
                break;
            }
        }
        return new Token(Token.NAMED_PARAM, sql.substring(start, pos));
    }

    private Token readLiteral() {
        int start = pos++;
        while (pos < sql.length()) {
            char c = sql.charAt(pos);
            char next = lookAhead(1);
            if (c == '\''
                    || c == '"'
                    || c == '\\'
                    || c == '?'
                    || (c == ':' && (next == ':' || isNameChar(next)))
                    || (c == '/' && (next == '*' || next == '/'))
                    || (c == '-' && next == '-')) {
                break;
            }
            pos++;
        }
        return new Token(Token.LITERAL, sql.substring(start, pos));
    }

    private char lookAhead(int offset) {
        int index = pos + offset;
        return index < sql.length() ? sql.charAt(index) : '\0';
    }

    private static boolean isNameChar(char c) {
        return c == '.'
                || c == '_'
                || c == '$'
                || Character.isLetterOrDigit(c)
                || c > 127;
    }
}