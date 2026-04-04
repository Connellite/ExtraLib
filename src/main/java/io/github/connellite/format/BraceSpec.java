package io.github.connellite.format;

import io.github.connellite.exception.FormatException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.IllegalFormatException;
import java.util.Locale;

/**
 * Parses a subset of {fmt}-style format specs after {@code ':'} and renders via {@link String#format}
 * or manual padding (center / non-space fill).
 *
 * <p>Grammar (partial): {@code [[fill]align][sign][#][0][width][.precision][L][type]} — same order as
 * <a href="https://fmt.dev/latest/syntax/">fmt format_spec</a>.
 */
final class BraceSpec {

    private enum Align {
        NONE,
        LEFT,
        RIGHT,
        CENTER
    }

    private BraceSpec() {}

    /**
     * @return formatted string, or {@code null} to fall back to legacy {@code "%" + spec}
     */
    static String tryFormat(Locale locale, Object value, String spec) {
        if (spec == null || spec.isEmpty()) {
            return null;
        }
        Parsed p = parse(spec);
        if (p == null) {
            return null;
        }
        return p.render(locale, value, spec);
    }

    private static final class Parsed {
        final char fill;
        final Align align;
        final String signFlags;
        final boolean alternate;
        final boolean zero;
        final int width;
        final int precision;
        final Character explicitJavaConv;

        Parsed(
                char fill,
                Align align,
                String signFlags,
                boolean alternate,
                boolean zero,
                int width,
                int precision,
                Character explicitJavaConv) {
            this.fill = fill;
            this.align = align;
            this.signFlags = signFlags;
            this.alternate = alternate;
            this.zero = zero;
            this.width = width;
            this.precision = precision;
            this.explicitJavaConv = explicitJavaConv;
        }

        String render(Locale locale, Object value, String spec) {
            char conv = explicitJavaConv != null ? explicitJavaConv : inferJavaConversion(value);
            boolean manualPad = align == Align.CENTER || fill != ' ';

            if (!manualPad) {
                String javaSpec = toJavaPercentSpec(conv);
                try {
                    return String.format(locale, javaSpec, unwrapForFormat(value, conv));
                } catch (IllegalFormatException e) {
                    throw new FormatException("invalid format specifier: " + spec, e);
                }
            }

            String inner = innerJavaPercentSpec(conv);
            Object arg = unwrapForFormat(value, conv);
            String core;
            try {
                core = String.format(locale, inner, arg);
            } catch (IllegalFormatException e) {
                throw new FormatException("invalid format specifier: " + spec, e);
            }
            if (width < 0) {
                return core;
            }
            return pad(core, width, fill, align);
        }

        private String toJavaPercentSpec(char conv) {
            StringBuilder sb = new StringBuilder("%");
            if (align == Align.LEFT) {
                sb.append('-');
            }
            sb.append(signFlags);
            if (alternate) {
                sb.append('#');
            }
            if (zero && align != Align.LEFT) {
                sb.append('0');
            }
            if (width >= 0) {
                sb.append(width);
            }
            if (precision >= 0) {
                sb.append('.').append(precision);
            }
            sb.append(conv);
            return sb.toString();
        }

        private String innerJavaPercentSpec(char conv) {
            StringBuilder sb = new StringBuilder("%");
            sb.append(signFlags);
            if (alternate) {
                sb.append('#');
            }
            if (zero) {
                sb.append('0');
            }
            if (precision >= 0) {
                sb.append('.').append(precision);
            }
            sb.append(conv);
            return sb.toString();
        }

        private static Object unwrapForFormat(Object value, char conv) {
            if (value == null) {
                return null;
            }
            if ((conv == 'd' || conv == 'x' || conv == 'X' || conv == 'o') && value instanceof Long) {
                return value;
            }
            return value;
        }

        private static char inferJavaConversion(Object value) {
            if (value == null) {
                return 's';
            }
            if (value instanceof Byte
                    || value instanceof Short
                    || value instanceof Integer
                    || value instanceof Long
                    || value instanceof BigInteger) {
                return 'd';
            }
            if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) {
                return 'f';
            }
            if (value instanceof Boolean || value instanceof Character) {
                return 's';
            }
            return 's';
        }

        private String pad(String s, int w, char f, Align a) {
            int len = s.length();
            if (len >= w) {
                return s;
            }
            int padCount = w - len;
            if (a == Align.LEFT) {
                return s + repeat(f, padCount);
            }
            if (a == Align.RIGHT) {
                return repeat(f, padCount) + s;
            }
            if (a == Align.CENTER) {
                int left = padCount / 2;
                int right = padCount - left;
                return repeat(f, left) + s + repeat(f, right);
            }
            return s;
        }

        private static String repeat(char c, int n) {
            if (n <= 0) {
                return "";
            }
            char[] buf = new char[n];
            java.util.Arrays.fill(buf, c);
            return new String(buf);
        }
    }

    private static Parsed parse(String spec) {
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
        while (i < n) {
            char c = spec.charAt(i);
            if (c == '+') {
                signFlags.append('+');
                i++;
            } else if (c == ' ') {
                signFlags.append(' ');
                i++;
            } else if (c == '-') {
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
        if (i < n && spec.charAt(i) == '0') {
            zero = true;
            i++;
        }

        int width = -1;
        if (i < n && Character.isDigit(spec.charAt(i))) {
            int w = 0;
            while (i < n && Character.isDigit(spec.charAt(i))) {
                w = w * 10 + (spec.charAt(i) - '0');
                i++;
            }
            width = w;
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

        if (i < n && spec.charAt(i) == 'L') {
            i++;
        }

        Character explicitJavaConv = null;
        if (i < n) {
            char t = spec.charAt(i);
            if (!Character.isLetter(t)) {
                return null;
            }
            explicitJavaConv = toJavaConversion(t);
            if (explicitJavaConv == null) {
                return null;
            }
            i++;
        }

        if (i != n) {
            return null;
        }

        return new Parsed(fill, align, signFlags.toString(), alternate, zero, width, precision, explicitJavaConv);
    }

    /**
     * fmt {@code i} maps to Java {@code d}. Other letters are valid Java conversions we support.
     */
    private static Character toJavaConversion(char t) {
        return switch (t) {
            case 'i', 'd' -> 'd';
            case 's' -> 's';
            case 'S' -> 'S';
            case 'x' -> 'x';
            case 'X' -> 'X';
            case 'o' -> 'o';
            case 'f' -> 'f';
            case 'e' -> 'e';
            case 'E' -> 'E';
            case 'g' -> 'g';
            case 'G' -> 'G';
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
