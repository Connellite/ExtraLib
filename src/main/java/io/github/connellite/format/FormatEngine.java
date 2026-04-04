package io.github.connellite.format;

import io.github.connellite.exception.FormatException;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.IllegalFormatException;
import java.util.Locale;

@UtilityClass
class FormatEngine {
    static String format(CharSequence pattern, Object[] args, Locale locale) {
        if (pattern == null) {
            throw new FormatException("format string is null");
        }
        StringBuilder sb = new StringBuilder(pattern.length() + 32);
        formatTo(sb, pattern, args, locale);
        return sb.toString();
    }

    static void formatTo(StringBuilder out, CharSequence pattern, Object[] args, Locale locale) {
        if (pattern == null) {
            throw new FormatException("format string is null");
        }
        try {
            formatToImpl(out, pattern, args, locale);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void formatToImpl(Appendable out, CharSequence pattern, Object[] args, Locale locale) throws IOException {
        ArgPack pack = ArgPack.of(args);
        int i = 0;
        int n = pattern.length();
        int nextAuto = 0;
        while (i < n) {
            char c = pattern.charAt(i);
            if (c == '{') {
                if (i + 1 < n && pattern.charAt(i + 1) == '{') {
                    out.append('{');
                    i += 2;
                    continue;
                }
                int close = indexOf(pattern, '}', i + 1);
                if (close < 0) {
                    throw new FormatException("unclosed '{' in format string");
                }
                String inside = pattern.subSequence(i + 1, close).toString();
                ReplacementField field = parseField(inside, nextAuto);
                nextAuto = field.nextAutoIndex();
                Object arg = pack.resolve(field.id());
                append(out, arg, field.spec(), locale);
                i = close + 1;
            } else if (c == '}') {
                if (i + 1 < n && pattern.charAt(i + 1) == '}') {
                    out.append('}');
                    i += 2;
                    continue;
                }
                throw new FormatException("unmatched '}' in format string");
            } else {
                out.append(c);
                i++;
            }
        }
    }

    private static void append(Appendable out, Object value, String spec, Locale locale)
            throws IOException {
        if (value instanceof FmtFormattable f) {
            f.appendFormatted(out, locale, spec);
            return;
        }
        if (spec == null || spec.isEmpty()) {
            out.append(String.valueOf(value));
            return;
        }
        if (spec.indexOf('%') >= 0) {
            throw new FormatException("'%' is not allowed inside format specifier");
        }
        try {
            out.append(String.format(locale, "%" + spec, value));
        } catch (IllegalFormatException ex) {
            throw new FormatException("invalid format specifier: " + spec, ex);
        }
    }

    private static ReplacementField parseField(String inside, int nextAuto) {
        String s = inside.trim();
        int colon = s.indexOf(':');
        String head;
        String spec;
        if (colon < 0) {
            head = s;
            spec = null;
        } else {
            head = s.substring(0, colon).trim();
            spec = s.substring(colon + 1).trim();
            if (spec.isEmpty()) {
                spec = null;
            }
        }
        if (head.isEmpty()) {
            return new ReplacementField(new AutoArgId(nextAuto), spec, nextAuto + 1);
        }
        if (isAllDigits(head)) {
            try {
                int idx = Integer.parseInt(head);
                if (idx < 0) {
                    throw new FormatException("negative argument index: " + idx);
                }
                return new ReplacementField(new IndexArgId(idx), spec, nextAuto);
            } catch (NumberFormatException e) {
                throw new FormatException("invalid argument index: " + head);
            }
        }
        if (isIdentifier(head)) {
            return new ReplacementField(new NameArgId(head), spec, nextAuto);
        }
        throw new FormatException("invalid replacement field: {" + inside + "}");
    }

    private static boolean isAllDigits(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return !str.isEmpty();
    }

    private static boolean isIdentifier(String str) {
        if (str.isEmpty()) {
            return false;
        }
        char c0 = str.charAt(0);
        if (!(Character.isLetter(c0) || c0 == '_')) {
            return false;
        }
        for (int i = 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_')) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("SameParameterValue")
    private static int indexOf(CharSequence s, char ch, int from) {
        int n = s.length();
        for (int i = Math.max(0, from); i < n; i++) {
            if (s.charAt(i) == ch) {
                return i;
            }
        }
        return -1;
    }
}
