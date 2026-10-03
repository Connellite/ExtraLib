package io.github.connellite.format;

import io.github.connellite.format.internal.FormatSegment;

import java.util.List;

/**
 * Parsed format pattern; build with {@link Fmt#compile(CharSequence)} and reuse across
 * {@link Fmt#format(CompiledFormat, Object...)} / {@link Fmt#formatTo} calls.
 *
 * <p>Opaque handle: construct only via {@link Fmt#compile}. Segment accessors exist for the
 * formatting engine in this module and are not a supported client API.
 */
@SuppressWarnings("ClassEscapesDefinedScope")
public record CompiledFormat(List<FormatSegment> segments, int patternLength) {

    /**
     * @param segments      compiled pieces; copied
     * @param patternLength original pattern length, used to size the output buffer
     */
    public CompiledFormat(List<FormatSegment> segments, int patternLength) {
        this.segments = List.copyOf(segments);
        this.patternLength = patternLength;
    }
}
