package io.github.connellite.format;

import java.util.Collection;

/**
 * Parsed format pattern; build with {@link Fmt#compile(CharSequence)} and reuse across
 * {@link Fmt#format(CompiledFormat, Object...)} / {@link Fmt#formatTo} calls.
 */
public final class CompiledFormat {

    /** Alternating literal strings and pre-parsed replacement fields. */
    private final Collection<FormatSegment> segments;

    private final int patternLength;

    CompiledFormat(Collection<FormatSegment> segments, int patternLength) {
        this.segments = segments;
        this.patternLength = patternLength;
    }

    Collection<FormatSegment> segments() {
        return segments;
    }

    int patternLength() {
        return patternLength;
    }
}
