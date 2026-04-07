package io.github.connellite.format;

import io.github.connellite.exception.FormatException;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

@UtilityClass
class FormatEngine {

    static CompiledFormat compile(CharSequence pattern) {
        if (pattern == null) {
            throw new FormatException("format string is null");
        }
        int n = pattern.length();
        List<FormatSegment> pieces = new ArrayList<>();
        StringBuilder lit = new StringBuilder();
        int i = 0;
        int nextAuto = 0;
        while (i < n) {
            char c = pattern.charAt(i);
            if (c == '{') {
                if (i + 1 < n && pattern.charAt(i + 1) == '{') {
                    lit.append('{');
                    i += 2;
                    continue;
                }
                if (!lit.isEmpty()) {
                    pieces.add(new LiteralSegment(lit.toString()));
                    lit.setLength(0);
                }
                int close = findClosingBrace(pattern, i);
                if (close < 0) {
                    throw new FormatException("unclosed '{' in format string");
                }
                String inside = pattern.subSequence(i + 1, close).toString();
                ReplacementField field = parseField(inside, nextAuto);
                nextAuto = field.nextAutoIndex();
                pieces.add(new FieldSegment(field));
                i = close + 1;
            } else if (c == '}') {
                if (i + 1 < n && pattern.charAt(i + 1) == '}') {
                    lit.append('}');
                    i += 2;
                    continue;
                }
                throw new FormatException("unmatched '}' in format string");
            } else {
                lit.append(c);
                i++;
            }
        }
        if (!lit.isEmpty()) {
            pieces.add(new LiteralSegment(lit.toString()));
        }
        return new CompiledFormat(pieces, n);
    }

    static String format(CompiledFormat compiled, Object[] args, Locale locale) {
        if (compiled == null) {
            throw new FormatException("compiled format is null");
        }
        StringBuilder sb = new StringBuilder(compiled.patternLength() + 32);
        formatTo(sb, compiled, args, locale);
        return sb.toString();
    }

    static String format(CompiledFormat compiled, Map<String, ?> named, Locale locale) {
        if (compiled == null) {
            throw new FormatException("compiled format is null");
        }
        StringBuilder sb = new StringBuilder(compiled.patternLength() + 32);
        formatTo(sb, compiled, named, locale);
        return sb.toString();
    }

    static void formatTo(Appendable out, CompiledFormat compiled, Object[] args, Locale locale) {
        if (compiled == null) {
            throw new FormatException("compiled format is null");
        }
        try {
            formatToImpl(out, compiled, ArgPack.of(args), locale);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void formatTo(Appendable out, CompiledFormat compiled, Map<String, ?> named, Locale locale) {
        if (compiled == null) {
            throw new FormatException("compiled format is null");
        }
        try {
            formatToImpl(out, compiled, ArgPack.named(named), locale);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void formatTo(Consumer<CharSequence> sink, CompiledFormat compiled, Object[] args, Locale locale) {
        if (compiled == null) {
            throw new FormatException("compiled format is null");
        }
        if (sink == null) {
            throw new FormatException("sink is null");
        }
        try {
            formatToImplConsumer(sink, compiled, ArgPack.of(args), locale);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void formatTo(Consumer<CharSequence> sink, CompiledFormat compiled, Map<String, ?> named, Locale locale) {
        if (compiled == null) {
            throw new FormatException("compiled format is null");
        }
        if (sink == null) {
            throw new FormatException("sink is null");
        }
        try {
            formatToImplConsumer(sink, compiled, ArgPack.named(named), locale);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void formatToImplConsumer(Consumer<CharSequence> sink, CompiledFormat compiled, ArgPack pack, Locale locale) throws IOException {
        StringBuilder sb = new StringBuilder(compiled.patternLength() + 32);
        formatToImpl(sb, compiled, pack, locale);
        sink.accept(sb);
    }

    private static void formatToImpl(Appendable out, CompiledFormat compiled, ArgPack pack, Locale locale)
            throws IOException {
        for (FormatSegment piece : compiled.segments()) {
            if (piece instanceof LiteralSegment literal) {
                out.append(literal.value());
            } else if (piece instanceof FieldSegment fieldSegment) {
                ReplacementField field = fieldSegment.field();
                Object arg = pack.resolve(field.id());
                append(out, arg, expandDynamicSpec(field.spec(), pack, field.nextAutoIndex()), locale);
            }
        }
    }

    private static String expandDynamicSpec(String spec, ArgPack pack, int nextAutoStart) {
        if (spec == null || spec.isEmpty() || spec.indexOf('{') < 0) {
            return spec;
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        int[] counter = {nextAutoStart};
        int n = spec.length();
        while (i < n) {
            char c = spec.charAt(i);
            if (c == '{' && i + 1 < n && spec.charAt(i + 1) == '{') {
                out.append('{');
                i += 2;
                continue;
            }
            if (c == '}' && i + 1 < n && spec.charAt(i + 1) == '}') {
                out.append('}');
                i += 2;
                continue;
            }
            if (c == '{') {
                int close = findClosingBrace(spec, i);
                if (close < 0) {
                    throw new FormatException("unclosed '{' in format specifier");
                }
                String inner = spec.substring(i + 1, close).trim();
                Object v = resolveSpecPlaceholder(inner, pack, counter);
                out.append(FormatStrings.defaultArgString(v));
                i = close + 1;
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static Object resolveSpecPlaceholder(String inner, ArgPack pack, int[] counter) {
        if (inner.isEmpty()) {
            return pack.resolve(new AutoArgId(counter[0]++));
        }
        if (isAllDigits(inner)) {
            try {
                int idx = Integer.parseInt(inner);
                if (idx < 0) {
                    throw new FormatException("negative argument index: " + idx);
                }
                return pack.resolve(new IndexArgId(idx));
            } catch (NumberFormatException e) {
                throw new FormatException("invalid argument index: " + inner);
            }
        }
        if (isIdentifier(inner)) {
            return pack.resolve(new NameArgId(inner));
        }
        throw new FormatException("invalid nested replacement in specifier: {" + inner + "}");
    }

    /**
     * {@code open} is the index of '{'; returns the index of the matching '}' or {@code -1}.
     * Doubled {@code {{}} and {@code }}} are literal braces and do not affect nesting depth.
     */
    private static int findClosingBrace(CharSequence s, int open) {
        if (open < 0 || open >= s.length() || s.charAt(open) != '{') {
            return -1;
        }
        int depth = 1;
        int i = open + 1;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '{' && i + 1 < n && s.charAt(i + 1) == '{') {
                i += 2;
                continue;
            }
            // Do not treat "}}" as one escape here: in "{0:─^{2}}" the two "}" close nested "{2" then the field.
            // Literal "}}" in pattern text is handled in compile()'s main loop, not while matching one field.
            if (c == '{') {
                depth++;
                i++;
                continue;
            }
            if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
                i++;
                continue;
            }
            i++;
        }
        return -1;
    }

    private static void append(Appendable out, Object value, String spec, Locale locale)
            throws IOException {
        if (value instanceof FmtFormattable f) {
            f.appendFormatted(out, locale, spec);
            return;
        }
        if (spec == null || spec.isEmpty()) {
            out.append(FormatStrings.defaultArgString(value));
            return;
        }
        if (spec.indexOf('%') >= 0) {
            out.append(DatePercentSpec.format(locale, value, spec));
            return;
        }
        String bridged = BraceSpec.tryFormat(locale, value, spec);
        if (bridged != null) {
            out.append(bridged);
            return;
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
            // Do not strip leading whitespace: fmt sign " " (space flag) lives there, e.g. "{: f}".
            spec = s.substring(colon + 1).stripTrailing();
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
                return new ReplacementField(new IndexArgId(idx), spec, Math.max(nextAuto, idx + 1));
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

}
