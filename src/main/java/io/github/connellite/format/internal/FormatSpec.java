package io.github.connellite.format.internal;

/**
 * Parsed {fmt} {@code format_spec} after {@code ':'}.
 *
 * <p>Grammar:
 * <a href="https://fmt.dev/11.2/syntax/#format-specification-mini-language">{@code [[fill]align][sign][#][0][width][.precision][L][type]}</a>,
 * matching
 * <a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/format.h">fmt 11.2 {@code format.h}</a>.
 * {@code remaining} holds a chrono or range suffix such as {@code %Y-%m-%d} or {@code :#x}.
 */
final class FormatSpec {

    enum Special {
        NONE,
        IEEE754_BITS
    }

    private final char fill;
    private final Align align;
    private final String signFlags;
    private final boolean minusSign;
    private final boolean alternate;
    private final boolean zero;
    private final int width;
    private final int precision;
    private final boolean localeAware;
    private final Special special;
    private final Character type;
    private final String remaining;

    FormatSpec(char fill, Align align, String signFlags, boolean minusSign, boolean alternate,
               boolean zero, int width, int precision, boolean localeAware, Special special,
               Character type, String remaining) {
        this.fill = fill;
        this.align = align;
        this.signFlags = signFlags;
        this.minusSign = minusSign;
        this.alternate = alternate;
        this.zero = zero;
        this.width = width;
        this.precision = precision;
        this.localeAware = localeAware;
        this.special = special;
        this.type = type;
        this.remaining = remaining;
    }

    char fill() {
        return fill;
    }

    Align align() {
        return align;
    }

    String signFlags() {
        return signFlags;
    }

    boolean minusSign() {
        return minusSign;
    }

    boolean alternate() {
        return alternate;
    }

    boolean zero() {
        return zero;
    }

    int width() {
        return width;
    }

    int precision() {
        return precision;
    }

    boolean localeAware() {
        return localeAware;
    }

    Special special() {
        return special;
    }

    Character type() {
        return type;
    }

    String remaining() {
        return remaining;
    }

    boolean hasRemaining() {
        return remaining != null && !remaining.isEmpty();
    }
}
