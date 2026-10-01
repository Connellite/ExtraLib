package io.github.connellite.format.internal;

import io.github.connellite.exception.FormatException;
import io.github.connellite.format.FmtFormattable;
import io.github.connellite.util.StringUtils;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.IllegalFormatException;
import java.util.Locale;

/**
 * Formats a single replacement-field value against a {fmt} {@code format_spec}.
 *
 * <p>Main path follows
 * <a href="https://fmt.dev/11.2/syntax/#format-specification-mini-language">fmt 11.2 format_spec</a>.
 * Java extras kept for compatibility: type {@code i} as {@code d}, type {@code S}, {@code bits}/{@code Bf},
 * {@code null} rendered as {@code "null"}, and a {@link String#format} fallback when the spec is not
 * a format_spec. Type {@code p} throws {@link FormatException}.
 */
@UtilityClass
final class ValueFormatter {

    static void append(Appendable out, Object value, String spec, Locale locale) throws IOException {
        if (value instanceof FmtFormattable f) {
            f.appendFormatted(out, locale, spec);
            return;
        }
        if (spec == null || spec.isEmpty()) {
            if (RangeFormatter.isRange(value)) {
                RangeFormatter.format(out, value, SpecParser.parse(""), locale);
                return;
            }
            if (ChronoFormatter.isChronoValue(value)) {
                ChronoFormatter.formatDefault(out, value, locale);
                return;
            }
            out.append(StringUtils.toString(value));
            return;
        }
        FormatSpec parsed = SpecParser.parse(spec);
        if (parsed == null) {
            fallbackStringFormat(out, value, spec, locale);
            return;
        }
        if (parsed.type() != null && parsed.type() == 'p') {
            throw new FormatException("pointer format 'p' is not supported");
        }
        if (ChronoFormatter.isChronoValue(value)
                && (ChronoFormatter.isChronoSpec(parsed) || ChronoFormatter.isDefaultSpec(parsed))) {
            ChronoFormatter.format(out, value, parsed, locale);
            return;
        }
        if (parsed.hasRemaining() && parsed.remaining().indexOf('%') >= 0) {
            throw new FormatException("invalid format specifier: " + spec);
        }
        if (RangeFormatter.isRange(value) && RangeFormatter.isRangeSpec(parsed, value)) {
            RangeFormatter.format(out, value, parsed, locale);
            return;
        }
        if (parsed.type() != null && parsed.type() == '?' && "s".equals(parsed.remaining())) {
            throw new FormatException("range type ?s is valid only for character ranges: " + spec);
        }
        if (parsed.hasRemaining()) {
            fallbackStringFormat(out, value, spec, locale);
            return;
        }
        formatStandard(out, value, parsed, spec, locale);
    }

    private static void formatStandard(Appendable out, Object value, FormatSpec spec, String raw,
                                       Locale locale) throws IOException {
        if (spec.special() == FormatSpec.Special.IEEE754_BITS) {
            out.append(Pad.apply(formatIeee754Bits(value, raw), spec));
            return;
        }
        char conv = spec.type() != null ? spec.type() : inferJavaConversion(value);
        if (spec.minusSign() && !isNumericForSign(conv)) {
            throw new FormatException(
                    "'-' sign flag is valid only for numeric format; use '<' for left alignment");
        }
        if (!spec.signFlags().isEmpty() && !isNumericForSign(conv)) {
            throw new FormatException("sign flag is valid only for numeric format: " + raw);
        }
        if (conv == '?') {
            out.append(Pad.apply(formatDebug(value, raw), spec, Align.LEFT));
            return;
        }
        if (conv == 'c') {
            out.append(Pad.apply(formatAsChar(value, raw), spec, Align.LEFT));
            return;
        }
        if (spec.localeAware() && isLocaleNumeric(conv, value)) {
            out.append(Pad.apply(formatLocaleNumeric(locale, value, conv, spec), spec, defaultAlign(conv)));
            return;
        }
        if (value == null && isRadixConversion(conv)) {
            out.append(Pad.apply("null", spec));
            return;
        }
        if (isRadixConversion(conv)) {
            if (spec.precision() >= 0) {
                throw new FormatException("precision not allowed for integral format: " + raw);
            }
            String core = formatRadixCore(value, conv, spec, raw);
            out.append(finishNumericPad(core, spec, conv));
            return;
        }

        boolean manualPad = spec.align() == Align.CENTER || spec.fill() != ' ';
        if (!manualPad) {
            String javaSpec = toJavaPercentSpec(spec, conv);
            try {
                out.append(String.format(locale, javaSpec, forPercentArg(value, conv)));
            } catch (IllegalFormatException e) {
                throw new FormatException("invalid format specifier: " + raw, e);
            }
            return;
        }
        String inner = innerJavaPercentSpec(spec, conv);
        Object arg = forPercentArg(value, conv);
        String core;
        try {
            core = String.format(locale, inner, arg);
        } catch (IllegalFormatException e) {
            throw new FormatException("invalid format specifier: " + raw, e);
        }
        out.append(Pad.apply(core, spec, defaultAlign(conv)));
    }

    private static String finishNumericPad(String core, FormatSpec spec, char conv) {
        if (spec.width() < 0) {
            return core;
        }
        if (spec.zero() && spec.align() == Align.NONE && isNumericForSign(conv)) {
            return Pad.apply(core, spec);
        }
        if (spec.zero() && spec.align() == Align.NONE) {
            return Pad.pad(core, spec.width(), '0', Align.RIGHT);
        }
        return Pad.apply(core, spec);
    }

    private static String formatDebug(Object value, String spec) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Character ch) {
            return DebugFormat.escapedChar(ch);
        }
        if (value instanceof CharSequence cs) {
            return DebugFormat.escapedString(cs);
        }
        throw new FormatException("debug format '?' is valid only for strings and characters: " + spec);
    }

    /**
     * fmt integer radix: sign and magnitude ({@code {:b}} of {@code -42} is {@code -101010}),
     * with {@code #} prefix after the sign ({@code -0b101010}, {@code -0x2a}, {@code -052}).
     */
    private static String formatRadixCore(Object value, char conv, FormatSpec spec, String raw) {
        int radix = switch (conv) {
            case 'b', 'B' -> 2;
            case 'o' -> 8;
            default -> 16;
        };
        boolean upper = conv == 'B' || conv == 'X';
        BigInteger number = toSignedBigInteger(value, raw);
        boolean negative = number.signum() < 0;
        BigInteger magnitude = number.abs();
        String digits = magnitude.toString(radix);
        if (upper) {
            digits = digits.toUpperCase(Locale.ROOT);
        }
        String prefix = "";
        if (spec.alternate()) {
            if (radix == 2) {
                prefix = upper ? "0B" : "0b";
            } else if (radix == 16) {
                prefix = upper ? "0X" : "0x";
            } else if (magnitude.signum() != 0) {
                prefix = "0";
            }
        }
        return signPrefix(negative, spec) + prefix + digits;
    }

    private static String signPrefix(boolean negative, FormatSpec spec) {
        if (negative) {
            return "-";
        }
        if (spec.signFlags().indexOf('+') >= 0) {
            return "+";
        }
        if (spec.signFlags().indexOf(' ') >= 0) {
            return " ";
        }
        return "";
    }

    private static BigInteger toSignedBigInteger(Object value, String spec) {
        if (value instanceof Boolean b) {
            return b ? BigInteger.ONE : BigInteger.ZERO;
        }
        if (value instanceof Character ch) {
            return BigInteger.valueOf(ch);
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long) {
            return BigInteger.valueOf(((Number) value).longValue());
        }
        if (value instanceof BigInteger bi) {
            return bi;
        }
        throw new FormatException("invalid type for integer format: " + spec);
    }

    private static boolean isRadixConversion(char conv) {
        return conv == 'b' || conv == 'B' || conv == 'x' || conv == 'X' || conv == 'o';
    }

    private static String toJavaPercentSpec(FormatSpec spec, char conv) {
        StringBuilder sb = new StringBuilder("%");
        if ((spec.align() == Align.LEFT
                || (spec.align() == Align.NONE && defaultAlign(conv) == Align.LEFT))
                && spec.width() > 0) {
            sb.append('-');
        }
        sb.append(spec.signFlags());
        if (spec.alternate()) {
            sb.append('#');
        }
        if (spec.zero() && spec.align() == Align.NONE) {
            sb.append('0');
        }
        if (spec.width() > 0) {
            sb.append(spec.width());
        }
        if (spec.precision() >= 0) {
            sb.append('.').append(spec.precision());
        }
        sb.append(conv);
        return sb.toString();
    }

    private static String innerJavaPercentSpec(FormatSpec spec, char conv) {
        StringBuilder sb = new StringBuilder("%");
        sb.append(spec.signFlags());
        if (spec.alternate()) {
            sb.append('#');
        }
        if (spec.precision() >= 0) {
            sb.append('.').append(spec.precision());
        }
        sb.append(conv);
        return sb.toString();
    }

    private static Object forPercentArg(Object value, char conv) {
        if ((conv == 's' || conv == 'S') && value != null && value.getClass().isArray()) {
            return StringUtils.toString(value);
        }
        if (value instanceof Character ch && isIntegralConversion(conv)) {
            return (int) ch;
        }
        if (value instanceof Boolean b && isIntegralConversion(conv)) {
            return b ? 1 : 0;
        }
        return value;
    }

    private static boolean isIntegralConversion(char conv) {
        return conv == 'd' || conv == 'x' || conv == 'X' || conv == 'o'
                || conv == 'b' || conv == 'B';
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
            return 'g';
        }
        return 's';
    }

    /**
     * fmt default: strings and characters left, numbers right.
     *
     * @see <a href="https://fmt.dev/11.2/syntax/#format-specification-mini-language">format_spec align</a>
     */
    private static Align defaultAlign(char conv) {
        return conv == 's' || conv == 'S' || conv == 'c' || conv == '?' ? Align.LEFT : Align.RIGHT;
    }

    private static boolean isLocaleNumeric(char conv, Object value) {
        if (!(value instanceof Number) && !(value instanceof Character)) {
            return false;
        }
        return conv == 'd' || conv == 'f' || conv == 'e' || conv == 'E' || conv == 'g' || conv == 'G'
                || conv == 'F';
    }

    private static boolean isNumericForSign(char conv) {
        return conv == 'd' || conv == 'f' || conv == 'F' || conv == 'e' || conv == 'E' || conv == 'g'
                || conv == 'G' || conv == 'a' || conv == 'A'
                || conv == 'b' || conv == 'B' || conv == 'x' || conv == 'X' || conv == 'o';
    }

    private static String formatLocaleNumeric(Locale locale, Object value, char conv, FormatSpec spec) {
        Number number = value instanceof Character ch ? (int) ch : (Number) value;
        NumberFormat nf;
        if (conv == 'd') {
            nf = NumberFormat.getIntegerInstance(locale);
        } else {
            nf = NumberFormat.getNumberInstance(locale);
            if (spec.precision() >= 0) {
                nf.setMinimumFractionDigits(spec.precision());
                nf.setMaximumFractionDigits(spec.precision());
            }
        }
        nf.setGroupingUsed(true);
        String s = nf.format(number);
        if (!spec.signFlags().isEmpty() && number.doubleValue() >= 0) {
            if (spec.signFlags().indexOf('+') >= 0) {
                s = "+" + s;
            } else if (spec.signFlags().indexOf(' ') >= 0) {
                s = " " + s;
            }
        }
        return s;
    }

    private static String formatAsChar(Object value, String spec) {
        if (value == null) {
            throw new FormatException("invalid type for character format: " + spec);
        }
        if (value instanceof Character ch) {
            return String.valueOf(ch);
        }
        int cp;
        if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
            cp = ((Number) value).intValue();
        } else if (value instanceof Long l) {
            if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
                throw new FormatException("code point out of range for character format: " + spec);
            }
            cp = l.intValue();
        } else if (value instanceof BigInteger bi) {
            try {
                cp = bi.intValueExact();
            } catch (ArithmeticException e) {
                throw new FormatException("code point out of range for character format: " + spec, e);
            }
        } else {
            throw new FormatException("invalid type for character format: " + spec);
        }
        if (!Character.isValidCodePoint(cp)) {
            throw new FormatException("invalid Unicode code point for character format: " + spec);
        }
        return new String(Character.toChars(cp));
    }

    private static String formatIeee754Bits(Object value, String spec) {
        if (value instanceof Float f) {
            int bits = Float.floatToIntBits(f);
            String raw = Integer.toBinaryString(bits);
            return "0".repeat(Math.max(0, 32 - raw.length())) + raw;
        }
        if (value instanceof Double d) {
            long bits = Double.doubleToLongBits(d);
            String raw = Long.toBinaryString(bits);
            return "0".repeat(Math.max(0, 64 - raw.length())) + raw;
        }
        throw new FormatException("invalid type for IEEE-754 bits format: " + spec);
    }

    private static void fallbackStringFormat(Appendable out, Object value, String spec, Locale locale)
            throws IOException {
        try {
            out.append(String.format(locale, "%" + spec, value));
        } catch (IllegalFormatException ex) {
            throw new FormatException("invalid format specifier: " + spec, ex);
        }
    }
}
