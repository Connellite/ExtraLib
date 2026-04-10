package io.github.connellite.format;

import java.util.Collection;

/**
 * Parsed format pattern; build with {@link Fmt#compile(CharSequence)} and reuse across
 * {@link Fmt#format(CompiledFormat, Object...)} / {@link Fmt#formatTo} calls.
 */
public record CompiledFormat (Collection<FormatSegment> segments, int patternLength) {

}
