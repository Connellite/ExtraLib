package io.github.connellite.format.internal;

import lombok.experimental.UtilityClass;

/**
 * Parses a {fmt} {@code format_spec}.
 *
 * <p>Based on
 * <a href="https://fmt.dev/11.2/syntax/#format-specification-mini-language">fmt 11.2 syntax</a>
 * and the field order in
 * <a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/base.h">fmt 11.2 {@code parse_format_specs}</a>.
 * {@code L} is also accepted before {@code .precision} (Java extra). Type {@code i} maps to
 * {@code d}; type {@code p} is recognized and rejected with {@link io.github.connellite.exception.FormatException}.
 */
@UtilityClass
final class SpecParser {

    /**
     * @return parsed spec, or {@code null} if the string is not a format_spec (caller may fall back
     *         to {@code String.format})
     */
    static FormatSpec parse(String spec) {
        if (spec == null || spec.isEmpty()) {
            return new FormatSpec(' ', Align.NONE, "", false, false, false, -1, -1, false,
                    FormatSpec.Special.NONE, null, "");
        }
        int n = spec.length();
        int i = 0;

        char fill = ' ';
        Align align = Align.NONE;
        if (n >= 2 && isAlign(spec.charAt(1))) {
            fill = spec.charAt(0);
            align = toAlign(spec.charAt(1));
            i = 2;
        } else if (n >= 1 && isAlign(spec.charAt(0))) {
            align = toAlign(spec.charAt(0));
            i = 1;
        }

        StringBuilder signFlags = new StringBuilder();
        boolean minusSign = false;
        while (i < n) {
            char c = spec.charAt(i);
            if (c == '+') {
                signFlags.append('+');
                i++;
            } else if (c == ' ') {
                signFlags.append(' ');
                i++;
            } else if (c == '-') {
                minusSign = true;
                i++;
            } else {
                break;
            }
        }

        boolean alternate = false;
        if (i < n && spec.charAt(i) == '#') {
            alternate = true;
            i++;
        }

        boolean zero = false;
        int width = -1;
        if (i < n && spec.charAt(i) == '0') {
            if (i + 1 < n && Character.isDigit(spec.charAt(i + 1))) {
                zero = true;
            } else {
                width = 0;
            }
            i++;
        }

        if (width < 0 && i < n && Character.isDigit(spec.charAt(i))) {
            int w = 0;
            while (i < n && Character.isDigit(spec.charAt(i))) {
                w = w * 10 + (spec.charAt(i) - '0');
                i++;
            }
            width = w;
        }

        boolean localeAware = false;
        if (i < n && spec.charAt(i) == 'L') {
            localeAware = true;
            i++;
        }

        int precision = -1;
        if (i < n && spec.charAt(i) == '.') {
            i++;
            if (i >= n || !Character.isDigit(spec.charAt(i))) {
                return null;
            }
            int p = 0;
            while (i < n && Character.isDigit(spec.charAt(i))) {
                p = p * 10 + (spec.charAt(i) - '0');
                i++;
            }
            precision = p;
        }

        if (!localeAware && i < n && spec.charAt(i) == 'L') {
            localeAware = true;
            i++;
        }

        Character type = null;
        FormatSpec.Special special = FormatSpec.Special.NONE;
        if (i < n) {
            if (spec.startsWith("bits", i)) {
                special = FormatSpec.Special.IEEE754_BITS;
                i += 4;
            } else if (spec.startsWith("Bf", i)) {
                special = FormatSpec.Special.IEEE754_BITS;
                i += 2;
            } else {
                char t = spec.charAt(i);
                Character mapped = toType(t);
                if (mapped != null) {
                    type = mapped;
                    i++;
                }
            }
        }

        String remaining = i < n ? spec.substring(i) : "";
        if (type != null && !remaining.isEmpty()) {
            if (!(type == '?' && remaining.equals("s"))) {
                return null;
            }
        }
        if (special != FormatSpec.Special.NONE && !remaining.isEmpty()) {
            return null;
        }
        return new FormatSpec(fill, align, signFlags.toString(), minusSign, alternate, zero, width,
                precision, localeAware, special, type, remaining);
    }

    /**
     * fmt {@code i} maps to Java {@code d}. {@code S} is the Java extra for upper-case strings.
     * {@code p} is accepted by the parser so the formatter can throw.
     */
    private static Character toType(char t) {
        return switch (t) {
            case 'i', 'd' -> 'd';
            case 'p' -> 'p';
            case 's' -> 's';
            case 'S' -> 'S';
            case 'x' -> 'x';
            case 'X' -> 'X';
            case 'o' -> 'o';
            case 'b' -> 'b';
            case 'B' -> 'B';
            case 'f' -> 'f';
            case 'F' -> 'F';
            case 'e' -> 'e';
            case 'E' -> 'E';
            case 'g' -> 'g';
            case 'G' -> 'G';
            case 'a' -> 'a';
            case 'A' -> 'A';
            case 'c' -> 'c';
            case '?' -> '?';
            default -> null;
        };
    }

    private static boolean isAlign(char c) {
        return c == '<' || c == '>' || c == '^';
    }

    private static Align toAlign(char c) {
        return switch (c) {
            case '<' -> Align.LEFT;
            case '>' -> Align.RIGHT;
            case '^' -> Align.CENTER;
            default -> Align.NONE;
        };
    }
}
