package io.github.connellite.jdbc.parser;

public final class PBraceTokenSqlLexer {
    private final String sql;
    private int pos;

    public PBraceTokenSqlLexer(String sql) {
        this.sql = sql;
    }

    /**
     * Returns the next token. {@code $P{...}} is a named parameter.
     * A backslash escapes only that opener.
     *
     * @return next token or {@link Token#EOF}
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
        if (c == '\\' && pOpenAt(pos + 1)) {
            pos++;
            String opener = sql.substring(pos, pos + 3);
            pos += 3;
            return new Token(Token.LITERAL, opener);
        }
        if (pOpenAt(pos)) {
            return readPToken();
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

    private Token readLiteral() {
        int start = pos++;
        while (pos < sql.length()) {
            char c = sql.charAt(pos);
            char next = lookAhead(1);
            if (c == '\''
                    || c == '"'
                    || c == '?'
                    || pOpenAt(pos)
                    || (c == '\\' && pOpenAt(pos + 1))
                    || (c == '/' && (next == '*' || next == '/'))
                    || (c == '-' && next == '-')) {
                break;
            }
            pos++;
        }
        return new Token(Token.LITERAL, sql.substring(start, pos));
    }

    /**
     * Reads {@code $P{...}}. An escaped {@code \}} does not close the token.
     * A missing closer keeps the original tail, starting at the opener, as literal text.
     */
    private Token readPToken() {
        int start = pos;
        pos += 3;
        StringBuilder unescaped = new StringBuilder();
        while (pos < sql.length()) {
            char c = sql.charAt(pos);
            if (c == '\\' && lookAhead(1) == '}') {
                unescaped.append('}');
                pos += 2;
            } else if (c == '}') {
                pos++;
                return new Token(Token.NAMED_PARAM, unescaped.toString());
            } else {
                unescaped.append(c);
                pos++;
            }
        }
        return new Token(Token.LITERAL, sql.substring(start, pos));
    }

    private boolean pOpenAt(int index) {
        return index + 2 < sql.length()
                && sql.charAt(index) == '$'
                && sql.charAt(index + 1) == 'P'
                && sql.charAt(index + 2) == '{';
    }

    private char lookAhead(int offset) {
        int index = pos + offset;
        return index < sql.length() ? sql.charAt(index) : '\0';
    }
}
