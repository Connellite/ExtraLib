package io.github.connellite.jdbc.internal;

public record Token(int type, String text) {
    public static final int EOF = -1;
    public static final int COMMENT = 1;
    public static final int LITERAL = 2;
    public static final int QUOTED_TEXT = 3;
    public static final int DOUBLE_QUOTED_TEXT = 4;
    public static final int NAMED_PARAM = 5;
    public static final int POSITIONAL_PARAM = 6;
    public static final int ESCAPED_TEXT = 7;
}
