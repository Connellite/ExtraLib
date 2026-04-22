package io.github.connellite.format;

import java.io.IOException;
import java.util.Locale;

/**
 * Custom formatting hook, analogous in role to a C++ {@code fmt::formatter<T, CharT>} specialization.
 *
 * <p>In {fmt}, the library picks {@code formatter<T>} by static type and calls {@code parse()} on the
 * part after {@code ':'} inside a replacement field, then {@code format()} to write the value. Here
 * there is no separate parse step exposed: the substring after {@code ':'} (or {@code null} / empty if
 * omitted) is passed as {@code spec}, and the implementation decides how to interpret it—often
 * mirroring what {@code parse()} would have accepted.
 *
 * <p>When a value implements this interface, {@link Fmt} delegates to {@link #appendFormatted}
 * instead of {@link String#valueOf} / {@link String#format}. For an empty or {@code null}
 * {@code spec}, use your type’s default textual form; for a non-empty {@code spec}, you may delegate
 * to {@code String.format(locale, "%" + spec, ...)} or apply custom rules.
 *
 * <p>Example (sketch of a “proxy string” like {@code StringProxy}):
 *
 * <pre>{@code
 * public final class StringProxy implements FmtFormattable {
 *     private final CharSequence data;
 *     public StringProxy(CharSequence data) { this.data = data; }
 *
 *     @Override
 *     public void appendFormatted(Appendable out, Locale locale, String spec) throws IOException {
 *         String s = data.toString();
 *         if (spec == null || spec.isEmpty()) {
 *             out.append(s);
 *         } else {
 *             out.append(String.format(locale, "%" + spec, s));
 *         }
 *     }
 * }
 * }</pre>
 */
@FunctionalInterface
public interface FmtFormattable {

    /**
     * Writes this value to {@code out} using the given locale and format spec.
     *
     * @param out    destination (same idea as {fmt}’s {@code format_context::out()})
     * @param locale locale for locale-sensitive formatting
     * @param spec   text after {@code ':'} in a field like {@code {name:spec}}; {@code null} or empty
     *               if the field has no colon part (e.g. {@code {}} or {@code {x}})
     */
    void appendFormatted(Appendable out, Locale locale, String spec) throws IOException;
}
