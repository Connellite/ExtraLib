package io.github.connellite.match;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ant-style path matcher adapted from Spring Framework {@code AntPathMatcher}.
 * <p>Matching rules:</p>
 * <ul>
 *   <li>{@code ?} matches one character within a path segment</li>
 *   <li>{@code *} matches zero or more characters within a path segment</li>
 *   <li>{@code **} matches zero or more path segments</li>
 * </ul>
 * <p>Source: {@code org.springframework.util.AntPathMatcher}</p>
 */
public class AntPathMatcher {

    /** Default path separator ({@code "/"}). */
    public static final String DEFAULT_PATH_SEPARATOR = "/";

    private static final char[] WILDCARD_CHARS = {'*', '?'};

    private final String pathSeparator;
    private final boolean caseSensitive;

    /**
     * Creates a matcher with {@link #DEFAULT_PATH_SEPARATOR} and case-sensitive matching.
     */
    public AntPathMatcher() {
        this(DEFAULT_PATH_SEPARATOR, true);
    }

    /**
     * Creates a matcher with the given separator and case-sensitivity.
     *
     * @param pathSeparator separator between path segments; must not be {@code null}
     * @param caseSensitive whether segment matching is case-sensitive
     */
    public AntPathMatcher(String pathSeparator, boolean caseSensitive) {
        this.pathSeparator = pathSeparator;
        this.caseSensitive = caseSensitive;
    }

    /**
     * Returns whether {@code path} matches {@code pattern}.
     *
     * @param pattern ant-style path pattern
     * @param path    path to test
     * @return {@code true} when the whole path matches the pattern
     */
    public boolean match(String pattern, String path) {
        return doMatch(pattern, path, true);
    }

    private boolean doMatch(String pattern, String path, boolean fullMatch) {
        if (path == null || path.startsWith(pathSeparator) != pattern.startsWith(pathSeparator)) {
            return false;
        }

        String[] patternDirs = tokenizePath(pattern);
        if (fullMatch && caseSensitive && !isPotentialMatch(path, patternDirs)) {
            return false;
        }

        String[] pathDirs = tokenizePath(path);
        int patternStart = 0;
        int patternEnd = patternDirs.length - 1;
        int pathStart = 0;
        int pathEnd = pathDirs.length - 1;

        while (patternStart <= patternEnd && pathStart <= pathEnd) {
            String patternDir = patternDirs[patternStart];
            if ("**".equals(patternDir)) {
                break;
            }
            if (!matchSegment(patternDir, pathDirs[pathStart])) {
                return false;
            }
            patternStart++;
            pathStart++;
        }

        if (pathStart > pathEnd) {
            if (patternStart > patternEnd) {
                return pattern.endsWith(pathSeparator) == path.endsWith(pathSeparator);
            }
            if (!fullMatch) {
                return true;
            }
            if (patternStart == patternEnd
                    && "*".equals(patternDirs[patternStart])
                    && path.endsWith(pathSeparator)) {
                return true;
            }
            for (int i = patternStart; i <= patternEnd; i++) {
                if (!"**".equals(patternDirs[i])) {
                    return false;
                }
            }
            return true;
        }
        if (patternStart > patternEnd) {
            return false;
        }
        if (!fullMatch && "**".equals(patternDirs[patternStart])) {
            return true;
        }

        while (patternStart <= patternEnd && pathStart <= pathEnd) {
            String patternDir = patternDirs[patternEnd];
            if ("**".equals(patternDir)) {
                break;
            }
            if (!matchSegment(patternDir, pathDirs[pathEnd])) {
                return false;
            }
            if (patternEnd == patternDirs.length - 1
                    && pattern.endsWith(pathSeparator) != path.endsWith(pathSeparator)) {
                return false;
            }
            patternEnd--;
            pathEnd--;
        }
        if (pathStart > pathEnd) {
            for (int i = patternStart; i <= patternEnd; i++) {
                if (!"**".equals(patternDirs[i])) {
                    return false;
                }
            }
            return true;
        }

        while (patternStart != patternEnd && pathStart <= pathEnd) {
            int nextDoubleWildcard = -1;
            for (int i = patternStart + 1; i <= patternEnd; i++) {
                if ("**".equals(patternDirs[i])) {
                    nextDoubleWildcard = i;
                    break;
                }
            }
            if (nextDoubleWildcard == patternStart + 1) {
                patternStart++;
                continue;
            }

            int patternLength = nextDoubleWildcard - patternStart - 1;
            int pathLength = pathEnd - pathStart + 1;
            int foundIndex = -1;

            segmentSearch:
            for (int i = 0; i <= pathLength - patternLength; i++) {
                for (int j = 0; j < patternLength; j++) {
                    if (!matchSegment(patternDirs[patternStart + j + 1], pathDirs[pathStart + i + j])) {
                        continue segmentSearch;
                    }
                }
                foundIndex = pathStart + i;
                break;
            }

            if (foundIndex == -1) {
                return false;
            }

            patternStart = nextDoubleWildcard;
            pathStart = foundIndex + patternLength;
        }

        for (int i = patternStart; i <= patternEnd; i++) {
            if (!"**".equals(patternDirs[i])) {
                return false;
            }
        }

        return true;
    }

    private boolean isPotentialMatch(String path, String[] patternDirs) {
        int pos = 0;
        for (String patternDir : patternDirs) {
            pos += skipSeparator(path, pos);
            int skipped = skipSegment(path, pos, patternDir);
            if (skipped < patternDir.length()) {
                return skipped > 0 || (!patternDir.isEmpty() && isWildcardChar(patternDir.charAt(0)));
            }
            pos += skipped;
        }
        return true;
    }

    private int skipSeparator(String path, int pos) {
        int skipped = 0;
        while (path.startsWith(pathSeparator, pos + skipped)) {
            skipped += pathSeparator.length();
        }
        return skipped;
    }

    private int skipSegment(String path, int pos, String prefix) {
        int skipped = 0;
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (isWildcardChar(c)) {
                return skipped;
            }
            int currentPos = pos + skipped;
            if (currentPos >= path.length()) {
                return 0;
            }
            if (c == path.charAt(currentPos)) {
                skipped++;
            }
        }
        return skipped;
    }

    private boolean isWildcardChar(char c) {
        for (char candidate : WILDCARD_CHARS) {
            if (c == candidate) {
                return true;
            }
        }
        return false;
    }

    private String[] tokenizePath(String path) {
        if (path == null || path.isEmpty()) {
            return new String[0];
        }

        List<String> tokens = new ArrayList<>();
        int start = 0;
        int length = path.length();
        int separatorLength = pathSeparator.length();

        while (start <= length) {
            int end = path.indexOf(pathSeparator, start);
            if (end == -1) {
                end = length;
            }
            if (end > start) {
                tokens.add(path.substring(start, end));
            }
            start = end + separatorLength;
            if (end == length) {
                break;
            }
        }

        return tokens.toArray(String[]::new);
    }

    private boolean matchSegment(String pattern, String value) {
        return SegmentMatcher.match(pattern, value, caseSensitive);
    }

    /**
     * Matches a single path segment against a pattern segment.
     * Adapted from Spring {@code AntPathMatcher.AntPathStringMatcher}.
     */
    private static final class SegmentMatcher {

        private static final Pattern GLOB_PATTERN = Pattern.compile("[?*]");

        private final String rawPattern;
        private final boolean caseSensitive;
        private final boolean exactMatch;
        private final Pattern compiledPattern;

        private SegmentMatcher(String pattern, boolean caseSensitive) {
            this.rawPattern = pattern;
            this.caseSensitive = caseSensitive;

            StringBuilder patternBuilder = new StringBuilder();
            Matcher matcher = GLOB_PATTERN.matcher(pattern);
            int end = 0;
            while (matcher.find()) {
                patternBuilder.append(Pattern.quote(pattern.substring(end, matcher.start())));
                String match = matcher.group();
                if ("?".equals(match)) {
                    patternBuilder.append('.');
                } else if ("*".equals(match)) {
                    patternBuilder.append(".*");
                }
                end = matcher.end();
            }

            if (end == 0) {
                this.exactMatch = true;
                this.compiledPattern = null;
            } else {
                this.exactMatch = false;
                patternBuilder.append(Pattern.quote(pattern.substring(end)));
                this.compiledPattern = Pattern.compile(
                        patternBuilder.toString(),
                        Pattern.DOTALL | (caseSensitive ? 0 : Pattern.CASE_INSENSITIVE)
                );
            }
        }

        static boolean match(String pattern, String value, boolean caseSensitive) {
            return new SegmentMatcher(pattern, caseSensitive).matches(value);
        }

        private boolean matches(String value) {
            if (exactMatch) {
                return caseSensitive ? rawPattern.equals(value) : rawPattern.equalsIgnoreCase(value);
            }
            return compiledPattern != null && compiledPattern.matcher(value).matches();
        }
    }
}
