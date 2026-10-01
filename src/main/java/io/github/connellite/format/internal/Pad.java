package io.github.connellite.format.internal;

import lombok.experimental.UtilityClass;

import java.util.Arrays;

/**
 * Width / fill / align padding, including sign-aware zero fill for {@code +}/{@code -}/{@code 0x}.
 */
@UtilityClass
final class Pad {

    static String apply(String s, FormatSpec spec) {
        return apply(s, spec, Align.RIGHT);
    }

    /**
     * @param defaultAlign used when the spec has no explicit {@code <}/{@code >}/{@code ^};
     *                     fmt uses left for strings/chars and right for numbers
     */
    static String apply(String s, FormatSpec spec, Align defaultAlign) {
        int width = spec.width();
        if (width < 0) {
            return s;
        }
        Align align = spec.align() == Align.NONE ? defaultAlign : spec.align();
        char fill = spec.fill();
        if (spec.zero() && spec.align() == Align.NONE && fill == ' ') {
            fill = '0';
        }
        return pad(s, width, fill, align);
    }

    static String pad(String s, int width, char fill, Align align) {
        int len = s.length();
        if (len >= width) {
            return s;
        }
        int padCount = width - len;
        if (align == Align.LEFT) {
            return s + repeat(fill, padCount);
        }
        if (align == Align.RIGHT) {
            if (fill == '0') {
                String adjusted = signAwareZeroPad(s, padCount);
                if (adjusted != null) {
                    return adjusted;
                }
            }
            return repeat(fill, padCount) + s;
        }
        if (align == Align.CENTER) {
            int left = padCount / 2;
            int right = padCount - left;
            return repeat(fill, left) + s + repeat(fill, right);
        }
        return s;
    }

    static String repeat(char c, int n) {
        if (n <= 0) {
            return "";
        }
        char[] buf = new char[n];
        Arrays.fill(buf, c);
        return new String(buf);
    }

    private static String signAwareZeroPad(String s, int padCount) {
        if (s.isEmpty() || padCount <= 0) {
            return null;
        }
        int prefixLen = 0;
        char first = s.charAt(0);
        if (first == '+' || first == '-' || first == ' ') {
            prefixLen = 1;
        }
        if (s.length() - prefixLen >= 2 && s.charAt(prefixLen) == '0') {
            char x = s.charAt(prefixLen + 1);
            if (x == 'x' || x == 'X' || x == 'b' || x == 'B') {
                prefixLen += 2;
            }
        }
        if (prefixLen == 0) {
            return null;
        }
        return s.substring(0, prefixLen) + repeat('0', padCount) + s.substring(prefixLen);
    }
}
