package io.github.connellite.jdbc.parser;

/**
 * Shared SQL lexer state: comments, double quotes, and lookahead.
 */
abstract class StatementLexer {
    final String sql;
    int pos;

    StatementLexer(String sql) {
        this.sql = sql;
    }

    final Token readBlockComment() {
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

    final Token readLineComment() {
        int start = pos;
        pos += 2;
        while (pos < sql.length() && sql.charAt(pos) != '\r' && sql.charAt(pos) != '\n') {
            pos++;
        }
        return new Token(Token.COMMENT, sql.substring(start, pos));
    }

    final Token readDoubleQuotedText() {
        int start = pos++;
        while (pos < sql.length()) {
            char c = sql.charAt(pos++);
            if (c == '"') {
                break;
            }
        }
        return new Token(Token.DOUBLE_QUOTED_TEXT, sql.substring(start, pos));
    }

    final char lookAhead(int offset) {
        int index = pos + offset;
        return index < sql.length() ? sql.charAt(index) : '\0';
    }
}
