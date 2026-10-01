package io.github.connellite.format.internal;

import io.github.connellite.exception.FormatException;
import io.github.connellite.util.StringUtils;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Range / map formatting: {@code ["n"][s|?s][":" underlying]}.
 *
 * <p>Based on
 * <a href="https://fmt.dev/11.2/syntax/#range-format-specifications">fmt 11.2 {@code range_format_spec}</a>
 * and
 * <a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/ranges.h">fmt 11.2 {@code ranges.h}</a>.
 * Default {@code {}} quotes strings and characters as in fmt; {@code {::spec}}, {@code n},
 * {@code s}/{@code ?s} follow {@code range_format_spec}. Empty underlying {@code {::}} formats
 * each element with an empty spec (unquoted strings/chars).
 */
@UtilityClass
final class RangeFormatter {

    static boolean isRange(Object value) {
        if (value == null) {
            return false;
        }
        return value.getClass().isArray() || value instanceof Iterable<?> || value instanceof Map<?, ?>;
    }

    static boolean isRangeSpec(FormatSpec spec, Object value) {
        if (spec == null) {
            return false;
        }
        if (spec.type() != null && spec.type() == '?' && "s".equals(spec.remaining())) {
            return isCharElementRange(value);
        }
        if (spec.type() != null && spec.type() == '?' && !spec.hasRemaining()) {
            return true;
        }
        if (spec.type() != null && (spec.type() == 's') && isCharElementRange(value)
                && !spec.hasRemaining()) {
            return true;
        }
        if (spec.type() != null || spec.special() != FormatSpec.Special.NONE) {
            return false;
        }
        if (!spec.hasRemaining()) {
            return true;
        }
        char c = spec.remaining().charAt(0);
        return c == ':' || c == 'n' || c == '?';
    }

    static boolean isCharElementRange(Object value) {
        if (value instanceof char[]) {
            return true;
        }
        if (value != null && value.getClass().isArray()
                && value.getClass().getComponentType() == Character.class) {
            return true;
        }
        if (value instanceof java.util.Collection<?> col && !col.isEmpty()) {
            return col.iterator().next() instanceof Character;
        }
        return false;
    }

    static void format(Appendable out, Object value, FormatSpec spec, Locale locale) throws IOException {
        RangeSpec range = parseRange(spec);
        String core = render(value, range, locale);
        out.append(Pad.apply(core, spec, Align.LEFT));
    }

    private static RangeSpec parseRange(FormatSpec spec) {
        boolean noBrackets = false;
        boolean asString = false;
        boolean debugString = false;
        boolean debugElements = false;
        String underlying = null;

        if (spec.type() != null && spec.type() == '?') {
            if ("s".equals(spec.remaining())) {
                debugString = true;
                asString = true;
            } else {
                debugElements = true;
            }
            return new RangeSpec(noBrackets, asString, debugString, debugElements, underlying);
        }
        if (spec.type() != null && spec.type() == 's') {
            return new RangeSpec(false, true, false, false, null);
        }

        String rem = spec.remaining();
        int i = 0;
        if (i < rem.length() && rem.charAt(i) == 'n') {
            noBrackets = true;
            i++;
        }
        if (i < rem.length() && rem.charAt(i) == '?') {
            debugString = true;
            i++;
            if (i >= rem.length() || rem.charAt(i) != 's') {
                throw new FormatException("invalid range format specifier: " + rem);
            }
            i++;
            asString = true;
        } else if (i < rem.length() && rem.charAt(i) == 's') {
            asString = true;
            i++;
        }
        if (i < rem.length()) {
            if (rem.charAt(i) != ':') {
                throw new FormatException("invalid range format specifier: " + rem);
            }
            if (asString) {
                throw new FormatException("range type s/?s cannot be combined with an underlying spec");
            }
            i++;
            underlying = rem.substring(i);
        }
        return new RangeSpec(noBrackets, asString, debugString, debugElements, underlying);
    }

    private static String render(Object value, RangeSpec range, Locale locale) throws IOException {
        if (value instanceof Map<?, ?> map) {
            return renderMap(map, range, locale);
        }
        if (range.asString) {
            return renderAsString(value, range.debugString);
        }
        String open;
        String close;
        if (range.noBrackets) {
            open = "";
            close = "";
        } else if (value instanceof Set<?>) {
            open = "{";
            close = "}";
        } else {
            open = "[";
            close = "]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(open);
        boolean first = true;
        Iterator<?> it = elements(value);
        while (it.hasNext()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            Object el = it.next();
            appendElement(sb, el, range, locale);
        }
        sb.append(close);
        return sb.toString();
    }

    private static String renderMap(Map<?, ?> map, RangeSpec range, Locale locale) throws IOException {
        if (range.asString) {
            throw new FormatException("range type s/?s is not valid for Map");
        }
        String open = range.noBrackets ? "" : "{";
        String close = range.noBrackets ? "" : "}";
        StringBuilder sb = new StringBuilder();
        sb.append(open);
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            appendElement(sb, e.getKey(), range, locale);
            sb.append(": ");
            appendElement(sb, e.getValue(), range, locale);
        }
        sb.append(close);
        return sb.toString();
    }

    private static void appendElement(StringBuilder sb, Object el, RangeSpec range, Locale locale)
            throws IOException {
        if (range.debugElements) {
            appendDebugElement(sb, el);
            return;
        }
        if (range.underlying != null) {
            String u = range.underlying.isEmpty() ? null : range.underlying;
            ValueFormatter.append(sb, el, u, locale);
            return;
        }
        if (el instanceof Character ch) {
            sb.append(DebugFormat.escapedChar(ch));
            return;
        }
        if (el instanceof CharSequence cs) {
            sb.append(DebugFormat.escapedString(cs));
            return;
        }
        if (el == null) {
            sb.append("null");
            return;
        }
        if (isRange(el)) {
            format(sb, el, SpecParser.parse(""), locale);
            return;
        }
        sb.append(StringUtils.toString(el));
    }

    private static void appendDebugElement(StringBuilder sb, Object el) throws IOException {
        if (el instanceof Character ch) {
            sb.append(DebugFormat.escapedChar(ch));
            return;
        }
        if (el instanceof CharSequence cs) {
            sb.append(DebugFormat.escapedString(cs));
            return;
        }
        sb.append(StringUtils.toString(el));
    }

    private static String renderAsString(Object value, boolean debug) {
        StringBuilder raw = new StringBuilder();
        Iterator<?> it = elements(value);
        while (it.hasNext()) {
            Object el = it.next();
            if (el instanceof Character ch) {
                raw.append(ch);
            } else if (el instanceof CharSequence cs) {
                raw.append(cs);
            } else if (el == null) {
                throw new FormatException("range type s requires character elements");
            } else {
                throw new FormatException("range type s requires character elements");
            }
        }
        if (debug) {
            return DebugFormat.escapedString(raw);
        }
        return "\"" + raw + "\"";
    }

    private static Iterator<?> elements(Object value) {
        if (value instanceof Iterable<?> it) {
            return it.iterator();
        }
        if (value.getClass().isArray()) {
            return new ArrayIterator(value);
        }
        throw new FormatException("not a range: " + value.getClass().getName());
    }

    private record RangeSpec(boolean noBrackets, boolean asString, boolean debugString,
                             boolean debugElements, String underlying) {
    }

    private static final class ArrayIterator implements Iterator<Object> {
        private final Object array;
        private final int length;
        private int index;

        ArrayIterator(Object array) {
            this.array = array;
            this.length = Array.getLength(array);
        }

        @Override
        public boolean hasNext() {
            return index < length;
        }

        @Override
        public Object next() {
            return Array.get(array, index++);
        }
    }
}
