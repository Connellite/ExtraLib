package io.github.connellite.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StringUtilsTest {

    @Test
    void join_matchesStringJoin_forCharSequenceIterableWithNulls() {
        Iterable<String> elements = Arrays.asList("a", null, "b");
        assertEquals(String.join(", ", elements), StringUtils.join(elements, ", "));
    }

    @Test
    void join_uuidsWithExplicitToString() {
        UUID a = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        UUID b = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        assertEquals(
                a + " | " + b,
                StringUtils.join(List.of(a, b), " | ", UUID::toString));
    }

    @Test
    void join_uuidsWithNull_doesNotCallMapper_matchesLiteralNull() {
        UUID a = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Iterable<UUID> elements = Arrays.asList(a, null);
        assertEquals(a + ", null", StringUtils.join(elements, ", ", UUID::toString));
    }

    @Test
    void join_defaultToString_sameAsObjectsToString() {
        assertEquals("1, 2, 3", StringUtils.join(List.of(1, 2, 3), ", "));
    }

    @Test
    void join_emptyIterable() {
        assertEquals("", StringUtils.join(List.of(), ","));
    }

    @Test
    void join_singleElement_noSeparator() {
        assertEquals("only", StringUtils.join(List.of("only"), ","));
    }

    @Test
    void join_nullElement_usesNullLiteral() {
        assertEquals("a, null, b", StringUtils.join(Arrays.asList("a", null, "b"), ", "));
    }

    @Test
    void join_nullIterableThrows() {
        assertThrows(NullPointerException.class, () -> StringUtils.join(null, ","));
    }

    @Test
    void join_nullSeparatorThrows() {
        assertThrows(NullPointerException.class, () -> StringUtils.join(List.of(1), null));
    }

    @Test
    void removeLineBreaks_joinsWrappedLinesWithSpace() {
        assertEquals("hello world", StringUtils.removeLineBreaks("hello\nworld", false));
    }

    @Test
    void removeLineBreaks_stripsIndentationAndTrailingSpacesPerLine() {
        assertEquals("a b", StringUtils.removeLineBreaks("  a  \n   b  ", false));
    }

    @Test
    void removeLineBreaks_rejoinsHyphenatedLineBreak() {
        assertEquals("super-cal", StringUtils.removeLineBreaks("super-\ncal", false));
    }

    @Test
    void removeLineBreaks_doubleNewlineBetweenWords_unchangedMiddleBreak() {
        // (?<=.) and (?=.) do not treat another newline as a boundary character
        assertEquals("a\n\nb", StringUtils.removeLineBreaks("a\n\nb", false));
    }

    @Test
    void removeLineBreaks_whenTrue_stripsSoftHyphens() {
        assertEquals("ab", StringUtils.removeLineBreaks("a\u00ADb", true));
    }

    @Test
    void removeLineBreaks_whenFalse_preservesSoftHyphens() {
        assertEquals("a\u00ADb", StringUtils.removeLineBreaks("a\u00ADb", false));
    }

    @Test
    void removeLineBreaks_nullTextThrows() {
        assertThrows(NullPointerException.class, () -> StringUtils.removeLineBreaks(null, false));
    }

    @Test
    void splitAndTrim_commaSeparated_trimsAndDropsBlanks() {
        assertIterableEquals(
                List.of("a", "b", "c"),
                StringUtils.splitAndTrim(" a , b , , c ", ","));
    }

    @Test
    void splitAndTrim_adjacentSeparatorsCollapse() {
        assertIterableEquals(List.of("a", "c"), StringUtils.splitAndTrim("a,,,c", ","));
    }

    @Test
    void splitAndTrim_nullString_returnsEmptyList() {
        assertIterableEquals(List.of(), StringUtils.splitAndTrim(null, ","));
    }

    @Test
    void splitAndTrim_whitespaceRegex_trimsAndSplits() {
        assertIterableEquals(List.of("a", "b"), StringUtils.splitAndTrim("  a \t b  ", "\\s+"));
    }

    @Test
    void splitAndTrim_nullRegexThrows() {
        assertThrows(NullPointerException.class, () -> StringUtils.splitAndTrim("a b", null));
    }

    @Test
    void splitAndTrim_multiCharSep_anyCharSplits() {
        assertIterableEquals(List.of("a", "b"), StringUtils.splitAndTrim("a,;b", ",;"));
    }
}
