package io.github.connellite.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntPathMatcherTest {

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Test
    void emptyPathMatchesEmptyPattern() {
        assertTrue(matcher.match("", ""));
    }

    @Test
    void exactPathMatch() {
        assertTrue(matcher.match("/foo/bar", "/foo/bar"));
        assertFalse(matcher.match("/foo/bar", "/foo/bar/baz"));
        assertFalse(matcher.match("/foo/bar", "/foo/baz"));
    }

    @Test
    void leadingSeparatorMustAgree() {
        assertFalse(matcher.match("foo", "/foo"));
        assertFalse(matcher.match("/foo", "foo"));
    }

    @Test
    void trailingSeparatorMustAgreeWhenSegmentsMatch() {
        assertTrue(matcher.match("/foo/", "/foo/"));
        assertFalse(matcher.match("/foo", "/foo/"));
        assertFalse(matcher.match("/foo/", "/foo"));
    }

    @Test
    void singleSegmentWildcard() {
        assertTrue(matcher.match("/foo/*", "/foo/bar"));
        assertTrue(matcher.match("/foo/*", "/foo/"));
        assertFalse(matcher.match("/foo/*", "/foo/bar/baz"));
        assertFalse(matcher.match("/foo/*", "/bar/foo"));
    }

    @Test
    void doubleSegmentWildcard() {
        assertTrue(matcher.match("/foo/**", "/foo/bar"));
        assertTrue(matcher.match("/foo/**", "/foo/bar/baz"));
        assertTrue(matcher.match("/**", "/foo/bar/baz"));
        assertTrue(matcher.match("/foo/**/baz", "/foo/bar/baz"));
        assertFalse(matcher.match("/foo/**/baz", "/foo/bar/qux"));
    }

    @Test
    void questionMarkMatchesSingleCharacterInSegment() {
        assertTrue(matcher.match("/foo/?ar", "/foo/bar"));
        assertTrue(matcher.match("/foo/f?o", "/foo/foo"));
        assertFalse(matcher.match("/foo/?ar", "/foo/bear"));
        assertFalse(matcher.match("/foo/?ar", "/foo/ar"));
    }

    @Test
    void caseSensitiveByDefault() {
        assertFalse(matcher.match("/foo/bar", "/FOO/BAR"));
        assertFalse(matcher.match("/Foo/*", "/foo/bar"));
    }

    @Test
    void caseInsensitiveMatcher() {
        AntPathMatcher insensitive = new AntPathMatcher(AntPathMatcher.DEFAULT_PATH_SEPARATOR, false);

        assertTrue(insensitive.match("/foo/bar", "/FOO/BAR"));
        assertTrue(insensitive.match("/Foo/*", "/foo/bar"));
        assertTrue(insensitive.match("/foo/?ar", "/FOO/BAR"));
    }

    @Test
    void customPathSeparator() {
        AntPathMatcher dotMatcher = new AntPathMatcher(".", true);

        assertTrue(dotMatcher.match("foo.bar", "foo.bar"));
        assertTrue(dotMatcher.match("foo.*", "foo.bar"));
        assertTrue(dotMatcher.match("foo.**", "foo.bar.baz"));
        assertFalse(dotMatcher.match("foo.bar", "foo/bar"));
    }

    @Test
    void nullPathDoesNotMatch() {
        assertFalse(matcher.match("/foo", null));
    }

    @Test
    void consecutiveDoubleWildcardsAreCollapsed() {
        assertTrue(matcher.match("/foo/**/**/baz", "/foo/bar/baz"));
        assertTrue(matcher.match("/foo/**/**", "/foo/bar/baz"));
    }

    @Test
    void patternWithOnlyDoubleWildcardMatchesAnyPath() {
        assertTrue(matcher.match("/**", "/"));
        assertTrue(matcher.match("/**", "/a"));
        assertTrue(matcher.match("/**", "/a/b/c"));
    }
}
