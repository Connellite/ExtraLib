package io.github.connellite.format;

import io.github.connellite.exception.FormatException;
import io.github.connellite.format.internal.FormatEngine;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Java port of {fmt} replacement-field formatting (fmtlib 11.2). Pointers ({@code p}) are not formatted.
 *
 * <p>Grammar and presentation types follow
 * <a href="https://fmt.dev/11.2/syntax/">fmt 11.2 syntax</a>:
 * {@code replacement_field ::= "{" [arg_id] [":" format_spec] "}"} and
 * {@code format_spec ::= [[fill]align][sign]["#"]["0"][width]["." precision]["L"][type]}.
 * Implementation references:
 * <a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/format.h">format.h</a>
 * ({@code write_escaped_string} at
 * <a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/format.h#L1763">L1763</a>),
 * <a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/ranges.h">ranges.h</a>,
 * <a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/chrono.h">chrono.h</a>.
 *
 * <p><b>Null arguments</b> (similar in spirit to SLF4J / typical Java logging): with no format spec,
 * values use a default string form (like {@link String#valueOf(Object)} for non-arrays); {@code null}
 * becomes {@code "null"} (no NPE). Arrays render as {@code [1, 2, 3]} with nesting, not {@code [I@…}.
 *
 * <p><b>Radix</b>: types {@code b}/{@code B} produce binary (optional alternate {@code #} → {@code 0b}/{@code 0B});
 * {@code d}/{@code i}, {@code x}, {@code o} use sign-and-magnitude for negatives ({@code {:x}} of
 * {@code -42} is {@code -2a}); {@code #} adds {@code 0x}/{@code 0}/{@code 0b} after the sign.
 * Type {@code S} upper-cases a string. {@code bits} / {@code Bf} print IEEE-754 bit patterns (Java extras).
 *
 * <p><b>Debug</b> ({@code ?}): strings and characters are quoted and escaped as in fmt
 * {@code write_escaped_string} / {@code write_escaped_char}.
 *
 * <p><b>Ranges</b> (arrays, {@link Iterable}, {@link java.util.Map}): empty spec follows fmt
 * ({@code ["a"]}, {@code {'x'}}, {@code {"k": 1}}); {@code {::spec}}, {@code n}, {@code s}/{@code ?s}
 * follow
 * <a href="https://fmt.dev/11.2/syntax/#range-format-specifications">{@code range_format_spec}</a>.
 * {@code {::}} uses an empty element spec (unquoted strings/chars).
 *
 * <p><b>Chrono</b>: empty spec is fmt default ({@code %F %T} for date-times, {@code %F} for dates,
 * tick+unit for {@link java.time.Duration}). With a spec, {@code [[fill]align][width][.precision]}
 * then strftime ({@code %Y}, {@code %F}, glibc {@code %-H}/{@code %_S}/{@code %OS}, …) per
 * <a href="https://fmt.dev/11.2/syntax/#chrono-format-specifications">{@code chrono_format_spec}</a>.
 * Duration also accepts time-of-day conversions and {@code %Q}/{@code %q} (Java Duration ticks:
 * seconds, or ms/us/ns when there is a fraction).
 *
 * <p><b>Sign</b> (after {@code ':'}, before type): {@code +} always; a single leading space flag for positives;
 * {@code -} default (only minus for negatives), same as omitting a sign flag.
 *
 * <p><b>Dynamic spec</b>: nested braces in the part after {@code ':'} pull further arguments, e.g.
 * {@code Fmt.format(Locale.US, "{:.{}f}", 3.14, 1)} → {@code "3.1"} (decimal separator follows {@link Locale}).
 *
 * <p>Named fields ({@code {name}}) are supplied with {@link #arg(String, Object)} in the varargs list.
 * Specs that are not a format_spec fall back to {@link String#format}({@code "%" + spec}).
 *
 * <p>String overloads of {@code format} and {@code formatTo} reuse a compiled form for a pattern of
 * at most 256 characters. {@link #compile(CharSequence)} always parses anew.
 *
 * @see FormatException
 * @see Named
 * @see FmtFormattable
 * @see CompiledFormat
 */
@UtilityClass
public final class Fmt {
    @Getter
    private static volatile Locale defaultLocale = Locale.getDefault();

    private static final class SingleArgFormatHolder {
        private static final CompiledFormat ONE_ARG = compile("{}");
    }

    /**
     * Creates a named argument that can be referenced from a template as {@code {name}}.
     *
     * @param name argument name used in the template
     * @param value argument value
     * @return immutable named argument wrapper
     */
    public static Named arg(String name, Object value) {
        return new Named(name, value);
    }

    /**
     * Sets the locale used by overloads that do not accept a locale parameter.
     *
     * @param locale new default locale, must not be {@code null}
     */
    public static void setDefaultLocale(Locale locale) {
        defaultLocale = Objects.requireNonNull(locale, "locale");
    }

    /**
     * Converts a value to text using the library formatting pipeline.
     *
     * @param value value to render
     * @return formatted value string
     */
    public static String toString(Object value) {
        return format(SingleArgFormatHolder.ONE_ARG, value);
    }

    /**
     * Parses a template and returns a reusable compiled representation.
     * Each call parses {@code pattern} again and returns a new instance.
     *
     * @param pattern template text
     * @return compiled template
     */
    public static CompiledFormat compile(CharSequence pattern) {
        return FormatEngine.compile(pattern);
    }

    /**
     * Formats a template with positional and named arguments using the current default locale.
     *
     * @param pattern template text
     * @param args formatting arguments
     * @return rendered text
     */
    public static String format(CharSequence pattern, Object... args) {
        return FormatEngine.format(FormatEngine.compileCached(pattern), args, defaultLocale);
    }

    /**
     * Formats a template with an explicit locale.
     *
     * @param locale locale for locale-sensitive conversions
     * @param pattern template text
     * @param args formatting arguments
     * @return rendered text
     */
    public static String format(Locale locale, CharSequence pattern, Object... args) {
        return FormatEngine.format(FormatEngine.compileCached(pattern), args, locale);
    }

    /**
     * Formats with a precompiled template using the current default locale.
     *
     * @param compiled precompiled template
     * @param args formatting arguments
     * @return rendered text
     */
    public static String format(CompiledFormat compiled, Object... args) {
        return FormatEngine.format(compiled, args, defaultLocale);
    }

    /**
     * Formats with a precompiled template and an explicit locale.
     *
     * @param locale locale for locale-sensitive conversions
     * @param compiled precompiled template
     * @param args formatting arguments
     * @return rendered text
     */
    public static String format(Locale locale, CompiledFormat compiled, Object... args) {
        return FormatEngine.format(compiled, args, locale);
    }

    /**
     * Formats and prints to {@link System#out} without a trailing newline.
     *
     * @param pattern template text
     * @param args formatting arguments
     */
    public static void print(CharSequence pattern, Object... args) {
        System.out.print(format(pattern, args));
    }

    /**
     * Formats and prints to {@link System#out} with a trailing newline.
     *
     * @param pattern template text
     * @param args formatting arguments
     */
    public static void println(CharSequence pattern, Object... args) {
        System.out.println(format(pattern, args));
    }

    /**
     * Formats with a precompiled template and prints to {@link System#out} without newline.
     *
     * @param compiled precompiled template
     * @param args formatting arguments
     */
    public static void print(CompiledFormat compiled, Object... args) {
        System.out.print(format(compiled, args));
    }

    /**
     * Formats with a precompiled template and prints to {@link System#out} with newline.
     *
     * @param compiled precompiled template
     * @param args formatting arguments
     */
    public static void println(CompiledFormat compiled, Object... args) {
        System.out.println(format(compiled, args));
    }

    /**
     * Formats and prints to a {@link PrintStream} without a trailing newline.
     *
     * @param out destination stream
     * @param pattern template text
     * @param args formatting arguments
     */
    public static void print(PrintStream out, CharSequence pattern, Object... args) {
        out.print(format(pattern, args));
    }

    /**
     * Formats and prints to a {@link PrintStream} with a trailing newline.
     *
     * @param out destination stream
     * @param pattern template text
     * @param args formatting arguments
     */
    public static void println(PrintStream out, CharSequence pattern, Object... args) {
        out.println(format(pattern, args));
    }

    /**
     * Formats with a precompiled template and prints to a {@link PrintStream}.
     *
     * @param out destination stream
     * @param compiled precompiled template
     * @param args formatting arguments
     */
    public static void print(PrintStream out, CompiledFormat compiled, Object... args) {
        out.print(format(compiled, args));
    }

    /**
     * Formats with a precompiled template and prints to a {@link PrintStream} with newline.
     *
     * @param out destination stream
     * @param compiled precompiled template
     * @param args formatting arguments
     */
    public static void println(PrintStream out, CompiledFormat compiled, Object... args) {
        out.println(format(compiled, args));
    }

    /**
     * Formats and prints to a {@link PrintWriter} without a trailing newline.
     *
     * @param out destination writer
     * @param pattern template text
     * @param args formatting arguments
     */
    public static void print(PrintWriter out, CharSequence pattern, Object... args) {
        out.print(format(pattern, args));
    }

    /**
     * Formats and prints to a {@link PrintWriter} with a trailing newline.
     *
     * @param out destination writer
     * @param pattern template text
     * @param args formatting arguments
     */
    public static void println(PrintWriter out, CharSequence pattern, Object... args) {
        out.println(format(pattern, args));
    }

    /**
     * Formats with a precompiled template and prints to a {@link PrintWriter}.
     *
     * @param out destination writer
     * @param compiled precompiled template
     * @param args formatting arguments
     */
    public static void print(PrintWriter out, CompiledFormat compiled, Object... args) {
        out.print(format(compiled, args));
    }

    /**
     * Formats with a precompiled template and prints to a {@link PrintWriter} with newline.
     *
     * @param out destination writer
     * @param compiled precompiled template
     * @param args formatting arguments
     */
    public static void println(PrintWriter out, CompiledFormat compiled, Object... args) {
        out.println(format(compiled, args));
    }

    /**
     * Formats and appends output to an {@link Appendable} using the default locale.
     *
     * @param out destination appendable
     * @param pattern template text
     * @param args formatting arguments
     */
    public static void formatTo(Appendable out, CharSequence pattern, Object... args) {
        FormatEngine.formatTo(out, FormatEngine.compileCached(pattern), args, defaultLocale);
    }

    /**
     * Formats and appends output to an {@link Appendable} with an explicit locale.
     *
     * @param out destination appendable
     * @param locale locale for locale-sensitive conversions
     * @param pattern template text
     * @param args formatting arguments
     */
    public static void formatTo(Appendable out, Locale locale, CharSequence pattern, Object... args) {
        FormatEngine.formatTo(out, FormatEngine.compileCached(pattern), args, locale);
    }

    /**
     * Formats with a precompiled template and appends output using the default locale.
     *
     * @param out destination appendable
     * @param compiled precompiled template
     * @param args formatting arguments
     */
    public static void formatTo(Appendable out, CompiledFormat compiled, Object... args) {
        FormatEngine.formatTo(out, compiled, args, defaultLocale);
    }

    /**
     * Formats with a precompiled template and appends output with an explicit locale.
     *
     * @param out destination appendable
     * @param locale locale for locale-sensitive conversions
     * @param compiled precompiled template
     * @param args formatting arguments
     */
    public static void formatTo(Appendable out, Locale locale, CompiledFormat compiled, Object... args) {
        FormatEngine.formatTo(out, compiled, args, locale);
    }

    /**
     * Formats and passes result chunks to a string sink using the default locale.
     *
     * @param sink destination consumer
     * @param pattern template text
     * @param args formatting arguments
     */
    public static void formatTo(Consumer<? super String> sink, CharSequence pattern, Object... args) {
        FormatEngine.formatTo(sink, FormatEngine.compileCached(pattern), args, defaultLocale);
    }

    /**
     * Formats and passes result chunks to a string sink with an explicit locale.
     *
     * @param sink destination consumer
     * @param locale locale for locale-sensitive conversions
     * @param pattern template text
     * @param args formatting arguments
     */
    public static void formatTo(Consumer<? super String> sink, Locale locale, CharSequence pattern, Object... args) {
        FormatEngine.formatTo(sink, FormatEngine.compileCached(pattern), args, locale);
    }

    /**
     * Formats with a precompiled template and passes output chunks to a string sink.
     *
     * @param sink destination consumer
     * @param compiled precompiled template
     * @param args formatting arguments
     */
    public static void formatTo(Consumer<? super String> sink, CompiledFormat compiled, Object... args) {
        FormatEngine.formatTo(sink, compiled, args, defaultLocale);
    }

    /**
     * Formats with a precompiled template and passes output chunks to a sink with explicit locale.
     *
     * @param sink destination consumer
     * @param locale locale for locale-sensitive conversions
     * @param compiled precompiled template
     * @param args formatting arguments
     */
    public static void formatTo(Consumer<? super String> sink, Locale locale, CompiledFormat compiled, Object... args) {
        FormatEngine.formatTo(sink, compiled, args, locale);
    }
}
