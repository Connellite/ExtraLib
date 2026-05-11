package io.github.connellite.jdbc.internal;

record Token(int type, String text) {
    static final int EOF = -1;
    static final int COMMENT = 1;
    static final int LITERAL = 2;
    static final int QUOTED_TEXT = 3;
    static final int DOUBLE_QUOTED_TEXT = 4;
    static final int NAMED_PARAM = 5;
    static final int POSITIONAL_PARAM = 6;
    static final int ESCAPED_TEXT = 7;
}
