package io.github.connellite.format.internal;

import lombok.experimental.UtilityClass;

import java.io.IOException;

/**
 * Debug presentation {@code ?}: quoted strings and characters with C-style escapes.
 *
 * <p>Based on fmt 11.2
 * <a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/format.h#L1763">{@code write_escaped_string}</a>
 * and
 * <a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/format.h#L1779">{@code write_escaped_char}</a>.
 */
@UtilityClass
final class DebugFormat {

    static void appendEscapedString(Appendable out, CharSequence str) throws IOException {
        out.append('"');
        appendEscapedBody(out, str, '"');
        out.append('"');
    }

    static String escapedString(CharSequence str) {
        StringBuilder sb = new StringBuilder(str.length() + 2);
        try {
            appendEscapedString(sb, str);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return sb.toString();
    }

    static void appendEscapedChar(Appendable out, int cp) throws IOException {
        out.append('\'');
        if (cp == '\'' || (needsEscape(cp) && cp != '"')) {
            appendEscapedCp(out, cp);
        } else {
            appendCodePoint(out, cp);
        }
        out.append('\'');
    }

    static String escapedChar(int cp) {
        StringBuilder sb = new StringBuilder(8);
        try {
            appendEscapedChar(sb, cp);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return sb.toString();
    }

    private static void appendEscapedBody(Appendable out, CharSequence str, char quote) throws IOException {
        int i = 0;
        int n = str.length();
        while (i < n) {
            int cp = Character.codePointAt(str, i);
            if (needsEscape(cp) || cp == quote) {
                appendEscapedCp(out, cp);
            } else {
                appendCodePoint(out, cp);
            }
            i += Character.charCount(cp);
        }
    }

    /**
     * fmt {@code needs_escape}: control characters, DEL, {@code "} and {@code \}, plus non-printables.
     *
     * @see <a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/format.h#L1677">needs_escape</a>
     */
    static boolean needsEscape(int cp) {
        if (cp < 0x20 || cp == 0x7f || cp == '"' || cp == '\\') {
            return true;
        }
        return !isPrintable(cp);
    }

    private static boolean isPrintable(int cp) {
        if (!Character.isValidCodePoint(cp) || !Character.isDefined(cp)) {
            return false;
        }
        int type = Character.getType(cp);
        return type != Character.CONTROL
                && type != Character.FORMAT
                && type != Character.PRIVATE_USE
                && type != Character.SURROGATE
                && type != Character.UNASSIGNED
                && type != Character.LINE_SEPARATOR
                && type != Character.PARAGRAPH_SEPARATOR;
    }

    private static void appendEscapedCp(Appendable out, int cp) throws IOException {
        switch (cp) {
            case '\n' -> out.append("\\n");
            case '\r' -> out.append("\\r");
            case '\t' -> out.append("\\t");
            case '"' -> out.append("\\\"");
            case '\'' -> out.append("\\'");
            case '\\' -> out.append("\\\\");
            default -> {
                if (cp < 0x100) {
                    appendPrefixedHex(out, 'x', 2, cp);
                } else if (cp < 0x10000) {
                    appendPrefixedHex(out, 'u', 4, cp);
                } else {
                    appendPrefixedHex(out, 'U', 8, cp);
                }
            }
        }
    }

    private static void appendPrefixedHex(Appendable out, char prefix, int width, int cp) throws IOException {
        out.append('\\');
        out.append(prefix);
        String hex = Integer.toHexString(cp);
        for (int i = hex.length(); i < width; i++) {
            out.append('0');
        }
        out.append(hex);
    }

    private static void appendCodePoint(Appendable out, int cp) throws IOException {
        if (Character.isBmpCodePoint(cp)) {
            out.append((char) cp);
        } else {
            out.append(Character.highSurrogate(cp));
            out.append(Character.lowSurrogate(cp));
        }
    }
}
