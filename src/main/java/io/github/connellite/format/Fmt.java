package io.github.connellite.format;

import io.github.connellite.exception.FormatException;
import lombok.experimental.UtilityClass;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Minimal Java-only "fmt-style" formatting: {@code String} in UTF-16, no wide-string layer.
 *
 * <p><b>Null arguments</b> (similar in spirit to SLF4J / typical Java logging): with no format spec,
 * {@link String#valueOf(Object)} is used, so a {@code null} reference becomes the text {@code "null"}
 * (no NPE). With a spec, behaviour follows {@link String#format}.
 *
 * @see FormatException
 * @see Named
 * @see FmtFormattable
 */
@UtilityClass
public final class Fmt {
    public static Named arg(String name, Object value) {
        return new Named(name, value);
    }

    public static String format(CharSequence pattern, Object... args) {
        return FormatEngine.format(pattern, args, Locale.getDefault());
    }

    public static String format(Locale locale, CharSequence pattern, Object... args) {
        return FormatEngine.format(pattern, args, locale);
    }

    public static void print(CharSequence pattern, Object... args) {
        System.out.print(format(pattern, args));
    }

    public static void println(CharSequence pattern, Object... args) {
        System.out.println(format(pattern, args));
    }

    public static void print(PrintStream out, CharSequence pattern, Object... args) {
        out.print(format(pattern, args));
    }

    public static void println(PrintStream out, CharSequence pattern, Object... args) {
        out.println(format(pattern, args));
    }

    public static void print(PrintWriter out, CharSequence pattern, Object... args) {
        out.print(format(pattern, args));
    }

    public static void println(PrintWriter out, CharSequence pattern, Object... args) {
        out.println(format(pattern, args));
    }

    public static void format_to(StringBuilder out, CharSequence pattern, Object... args) {
        FormatEngine.formatTo(out, pattern, args, Locale.getDefault());
    }

    public static void format_to(StringBuilder out, Locale locale, CharSequence pattern, Object... args) {
        FormatEngine.formatTo(out, pattern, args, locale);
    }
}
