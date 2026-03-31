package io.github.connellite.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for WildKMP.
 *
 * @author Varun Shah
 */
public class WildKMPMatchingTest {

    @Test
    public void isMatchBasic() {
        assertTrue(WildKMPMatching.isMatch("ACGT", "ACGT"));
        assertTrue(WildKMPMatching.isMatch("ACGTACAT", "ACAT"));
        assertFalse(WildKMPMatching.isMatch("ACGT", "TGCA"));
    }

    @Test
    public void isMatchWildcardRuns() {
        assertTrue(WildKMPMatching.isMatch("HUUGE", "H**GE"));
        assertTrue(WildKMPMatching.isMatch("HUUUGEHUUGEE", "H**G**"));
        assertFalse(WildKMPMatching.isMatch("HINGE", "H**GE"));
    }

    @Test
    public void isMatchCharSequenceTypes() {
        StringBuilder text = new StringBuilder("AAACCBAAB");
        StringBuilder pattern = new StringBuilder("A**B");
        assertTrue(WildKMPMatching.isMatch(text, pattern));

        StringBuffer text2 = new StringBuffer("ACCCABABB");
        StringBuffer pattern2 = new StringBuffer("A**B");
        assertFalse(WildKMPMatching.isMatch(text2, pattern2));
    }

    @Test
    public void isMatchEmptyAndNull() {
        assertTrue(WildKMPMatching.isMatch("", ""));
        assertFalse(WildKMPMatching.isMatch("", "A"));
        assertThrows(NullPointerException.class, () -> WildKMPMatching.isMatch(null, "A"));
        assertThrows(NullPointerException.class, () -> WildKMPMatching.isMatch("A", null));
    }

    @Test
    public void testAll() {
        assertEquals(0, WildKMPMatching.search("ACGT", "ACGT"));
        assertEquals(4, WildKMPMatching.search("ACGTACAT", "ACAT"));
        assertEquals(0, WildKMPMatching.search("ACGTACAT", "ACGT"));
        assertEquals(2, WildKMPMatching.search("AAAAAT", "AAAT"));
        assertEquals(8, WildKMPMatching.search("ACAT ACGACACAGT", "ACACAGT"));
        assertEquals(0, WildKMPMatching.search("ACGTACAT", "AC*T"));
        assertEquals(4, WildKMPMatching.search("AUGEHIGE", "H*GE"));
        assertEquals(0, WildKMPMatching.search("AUGEHIGE", "*UGE"));
        assertEquals(4, WildKMPMatching.search("AUGEHIGE", "HIG*"));
        assertEquals(0, WildKMPMatching.search("HUGEHUGS", "HUG*"));
        assertEquals(-1, WildKMPMatching.search("HUGEHUGS", "HIG*"));
        assertEquals(6, WildKMPMatching.search("HUGEAFHUGEEE", "HU*E*E"));
        assertEquals(5, WildKMPMatching.search("HOOLIHUUGEE", "H**GE"));
        assertEquals(0, WildKMPMatching.search("HUUGE", "H**GE"));
        assertEquals(-1, WildKMPMatching.search("HINGE", "H**GE"));
        assertEquals(-1, WildKMPMatching.search("ACCCABABB", "A**B"));
        assertEquals(10, WildKMPMatching.search("HIIIHINGGEHUUUGE", "H***GE"));
        assertEquals(2, WildKMPMatching.search("AAACCBAAB", "A**B"));
        assertEquals(-1, WildKMPMatching.search("AAACCBAAB", "***B"));
        assertEquals(-1, WildKMPMatching.search("AAACCBAAB", "B***"));
        assertEquals(6, WildKMPMatching.search("AAAACCABBBB", "A***B"));
        assertEquals(0, WildKMPMatching.search("HUUGEE", "H**G**"));
        assertEquals(6, WildKMPMatching.search("HUUUGEHUUGEE", "H**G**"));
        assertEquals(1, WildKMPMatching.search("HUUUGEHUUGEE", "***"));
        assertEquals(5, WildKMPMatching.search("AAABBAAABBB", "AAA***"));
        assertEquals(-1, WildKMPMatching.search("HUUGGGCCB", "H**G**B"));
        assertEquals(3, WildKMPMatching.search("AAACCCBAAB", "***B**"));
        assertEquals(8, WildKMPMatching.search("AAACCCBABBBBCC", "***B**"));
        assertEquals(3, WildKMPMatching.search("AACAAT", "A*T"));
        assertEquals(3, WildKMPMatching.search("AAAAAT", "A*T"));
        assertEquals(9, WildKMPMatching.search("AAAAAACCDAAAAAAD", "A**A**D"));
        assertEquals(-1, WildKMPMatching.search("babcacabca", "aa*c"));
    }
}
