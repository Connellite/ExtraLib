package io.github.connellite.util;

import io.github.connellite.util.internal.HexDigits;
import lombok.experimental.UtilityClass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Escapes non-ASCII text to {@code \\uXXXX} sequences and decodes those sequences back to Unicode,
 * similar in spirit to the JDK {@code native2ascii} tool.
 */
@UtilityClass
public class ConvertToAscii {
    private static final Pattern UNICODE_ESCAPE_PATTERN = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
    /**
     * Escapes characters above U+007F as {@code \\uXXXX}. ASCII letters, digits, and punctuation stay as-is.
     *
     * @param text source text; {@code null} and empty are returned unchanged
     * @return escaped text, or {@code null} if {@code text} was {@code null}
     */
    public static String native2ascii(String text) {
        return native2ascii(text, false);
    }
    /**
     * Escapes characters to {@code \\uXXXX} form.
     * <p>
     * If {@code ignoreLatinCharacters} is {@code false}, only code points {@code > 0x7F} are escaped.
     * If {@code true}, every character (including ASCII) is written as a {@code \\uXXXX} escape.
     *
     * @param text source text; {@code null} and empty are returned unchanged
     * @param ignoreLatinCharacters when {@code true}, escape all characters; when {@code false}, only non-ASCII
     * @return escaped text, or {@code null} if {@code text} was {@code null}
     */
    public static String native2ascii(String text, boolean ignoreLatinCharacters) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder unicodeText = new StringBuilder();
        byte[] unit = new byte[2];
        for (char character : text.toCharArray()) {
            if ((int) character > Byte.MAX_VALUE || ignoreLatinCharacters) {
                unit[0] = (byte) (character >>> 8);
                unit[1] = (byte) character;
                unicodeText.append("\\u").append(HexDigits.toHex(unit));
            } else {
                unicodeText.append(character);
            }
        }
        return unicodeText.toString();
    }
    /**
     * Replaces each {@code \\uXXXX} substring (four hex digits, case-insensitive) with the corresponding
     * UTF-16 code unit. Unmatched parts of the input are copied unchanged.
     *
     * @param text text possibly containing {@code \\uXXXX} escapes; {@code null} and empty are returned unchanged
     * @return decoded text, or {@code null} if {@code text} was {@code null}
     */
    public static String ascii2native(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher matcher = UNICODE_ESCAPE_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            byte[] encoded = HexDigits.fromHex(matcher.group(1));
            if (encoded == null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            char decoded = (char) (((encoded[0] & 0xff) << 8) | (encoded[1] & 0xff));
            matcher.appendReplacement(sb, Character.toString(decoded));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}