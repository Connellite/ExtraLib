package io.github.connellite.format;

import io.github.connellite.logger.Logger;
import io.github.connellite.logger.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Replaces placeholders such as {@code ${name}} with values from {@link Properties} or a resolver.
 * <p>
 * Port of Spring {@code org.springframework.util.PropertyPlaceholderHelper}: nested placeholders,
 * {@code ${key:default}} when a value separator is set, circular-reference detection, and optional
 * escape of the prefix or separator.
 */
public final class PropertyPlaceholderReplacer {

    private static final int PLACEHOLDER_CACHE_SIZE = 1_000;

    private static final Map<String, String> WELL_KNOWN_SIMPLE_PREFIXES = Map.of(
            "}", "{",
            "]", "[",
            ")", "("
    );

    private final String placeholderPrefix;
    private final String placeholderSuffix;
    private final String simplePrefix;
    private final String valueSeparator;
    private final Character escapeCharacter;
    private final boolean ignoreUnresolvablePlaceholders;
    private final Map<CacheKey, String> replacementCache;

    /**
     * Prefix/suffix only; unresolvable placeholders are left as-is.
     */
    public PropertyPlaceholderReplacer(String placeholderPrefix, String placeholderSuffix) {
        this(placeholderPrefix, placeholderSuffix, null, true);
    }

    /**
     * @param valueSeparator                 {@code key} / default separator, or {@code null} to disable defaults
     * @param ignoreUnresolvablePlaceholders {@code true} leaves unknown placeholders unchanged
     */
    public PropertyPlaceholderReplacer(
            String placeholderPrefix,
            String placeholderSuffix,
            String valueSeparator,
            boolean ignoreUnresolvablePlaceholders) {
        this(placeholderPrefix, placeholderSuffix, valueSeparator, null, ignoreUnresolvablePlaceholders);
    }

    /**
     * @param escapeCharacter character immediately before a prefix or value separator to treat it as literal
     */
    public PropertyPlaceholderReplacer(
            String placeholderPrefix,
            String placeholderSuffix,
            String valueSeparator,
            Character escapeCharacter,
            boolean ignoreUnresolvablePlaceholders) {
        this.placeholderPrefix = Objects.requireNonNull(placeholderPrefix, "placeholderPrefix");
        this.placeholderSuffix = Objects.requireNonNull(placeholderSuffix, "placeholderSuffix");
        String simple = WELL_KNOWN_SIMPLE_PREFIXES.get(this.placeholderSuffix);
        this.simplePrefix = simple != null && this.placeholderPrefix.endsWith(simple) ? simple : this.placeholderPrefix;
        this.valueSeparator = valueSeparator;
        this.escapeCharacter = escapeCharacter;
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;

        this.replacementCache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, String> eldest) {
                return size() > PLACEHOLDER_CACHE_SIZE;
            }
        });
    }

    /**
     * Replaces placeholders using {@link Properties#getProperty(String)}.
     */
    public String replacePlaceholders(String value, Properties properties) {
        Objects.requireNonNull(properties, "properties");
        return replacePlaceholders(value, properties::getProperty);
    }

    /**
     * Same as {@link #replacePlaceholders(String, Properties)}, reusing a previous result
     * for the same template and property snapshot. There is no limit on template length.
     * The cache keeps at most {@link #PLACEHOLDER_CACHE_SIZE} entries and drops the least recently used.
     */
    public String replacePlaceholdersCached(String value, Properties properties) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(properties, "properties");
        CacheKey key = new CacheKey(value, propertySnapshot(properties), null);
        return replaceCached(key, () -> replacePlaceholders(value, properties));
    }

    /**
     * Same as {@link #replacePlaceholders(String, PlaceholderResolver)}, reusing a previous result
     * for the same template and the same resolver instance.
     */
    public String replacePlaceholdersCached(String value, PlaceholderResolver placeholderResolver) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(placeholderResolver, "placeholderResolver");
        CacheKey key = new CacheKey(value, List.of(), placeholderResolver);
        return replaceCached(key, () -> replacePlaceholders(value, placeholderResolver));
    }

    private String replaceCached(CacheKey key, Supplier<String> replacement) {
        String cached = replacementCache.get(key);
        if (cached != null) {
            return cached;
        }
        String replaced = replacement.get();
        replacementCache.put(key, replaced);
        return replaced;
    }

    /** Required for tests. */
    Map<CacheKey, String> replacementCache() {
        return replacementCache;
    }

    /**
     * Replaces placeholders using {@code placeholderResolver}.
     */
    public String replacePlaceholders(String value, PlaceholderResolver placeholderResolver) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(placeholderResolver, "placeholderResolver");
        return stripPrefixEscapes(parseStringValue(value, placeholderResolver, null));
    }

    private String parseStringValue(String value, PlaceholderResolver placeholderResolver, Set<String> visitedPlaceholders) {
        int startIndex = indexOfUnescaped(value, this.placeholderPrefix, 0);
        if (startIndex == -1) {
            return value;
        }

        StringBuilder result = new StringBuilder(value);
        while (startIndex != -1) {
            int endIndex = findPlaceholderEndIndex(result, startIndex);
            if (endIndex == -1) {
                break;
            }
            String placeholder = result.substring(startIndex + this.placeholderPrefix.length(), endIndex);
            String originalPlaceholder = placeholder;
            if (visitedPlaceholders == null) {
                visitedPlaceholders = new HashSet<>(4);
            }
            if (!visitedPlaceholders.add(originalPlaceholder)) {
                throw new IllegalArgumentException(
                        "Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
            }
            placeholder = parseStringValue(placeholder, placeholderResolver, visitedPlaceholders);
            String propVal = placeholderResolver.resolvePlaceholder(placeholder);
            if (propVal == null && this.valueSeparator != null) {
                int separatorIndex = indexOfUnescaped(placeholder, this.valueSeparator, 0);
                if (separatorIndex != -1) {
                    String actualPlaceholder = placeholder.substring(0, separatorIndex);
                    String defaultValue = placeholder.substring(separatorIndex + this.valueSeparator.length());
                    propVal = placeholderResolver.resolvePlaceholder(actualPlaceholder);
                    if (propVal == null) {
                        propVal = defaultValue;
                    }
                }
            }
            if (propVal != null) {
                propVal = parseStringValue(propVal, placeholderResolver, visitedPlaceholders);
                result.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
                String resolved = placeholder;
                LogHolder.logger.trace(() -> "Resolved placeholder '" + resolved + "'");
                startIndex = indexOfUnescaped(result, this.placeholderPrefix, startIndex + propVal.length());
            } else if (this.ignoreUnresolvablePlaceholders) {
                startIndex = indexOfUnescaped(result, this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
            } else {
                throw new IllegalArgumentException(
                        "Could not resolve placeholder '" + placeholder + "' in value \"" + value + "\"");
            }
            visitedPlaceholders.remove(originalPlaceholder);
        }
        return result.toString();
    }

    private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
        int index = startIndex + this.placeholderPrefix.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (substringMatch(buf, index, this.placeholderSuffix)) {
                if (withinNestedPlaceholder > 0) {
                    withinNestedPlaceholder--;
                    index += this.placeholderSuffix.length();
                } else {
                    return index;
                }
            } else if (substringMatch(buf, index, this.simplePrefix)) {
                withinNestedPlaceholder++;
                index += this.simplePrefix.length();
            } else {
                index++;
            }
        }
        return -1;
    }

    private int indexOfUnescaped(CharSequence value, String token, int from) {
        int index = indexOf(value, token, from);
        while (index != -1 && isEscaped(value, index)) {
            index = indexOf(value, token, index + token.length());
        }
        return index;
    }

    private boolean isEscaped(CharSequence value, int index) {
        return this.escapeCharacter != null && index > 0 && value.charAt(index - 1) == this.escapeCharacter;
    }

    private String stripPrefixEscapes(String value) {
        if (this.escapeCharacter == null) {
            return value;
        }
        String escapedPrefix = this.escapeCharacter + this.placeholderPrefix;
        int idx = value.indexOf(escapedPrefix);
        if (idx < 0) {
            return value;
        }
        return value.replace(escapedPrefix, this.placeholderPrefix);
    }

    private static int indexOf(CharSequence value, String token, int from) {
        if (value instanceof String s) {
            return s.indexOf(token, from);
        }
        return value.toString().indexOf(token, from);
    }

    private static boolean substringMatch(CharSequence buf, int index, String substring) {
        if (index + substring.length() > buf.length()) {
            return false;
        }
        for (int i = 0; i < substring.length(); i++) {
            if (buf.charAt(index + i) != substring.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static List<PropertyEntry> propertySnapshot(Properties properties) {
        List<String> names = new ArrayList<>(properties.stringPropertyNames());
        Collections.sort(names);
        List<PropertyEntry> entries = new ArrayList<>(names.size());
        for (String name : names) {
            entries.add(new PropertyEntry(name, properties.getProperty(name)));
        }
        return List.copyOf(entries);
    }

    record CacheKey(String value, List<PropertyEntry> properties, PlaceholderResolver resolver) {
    }

    record PropertyEntry(String key, String value) {
    }

    /**
     * Resolves a placeholder name to a replacement, or {@code null} to leave it unresolved.
     */
    @FunctionalInterface
    public interface PlaceholderResolver {
        String resolvePlaceholder(String placeholderName);
    }

    private static class LogHolder {
        private static final Logger logger = LoggerFactory.getLogger(PropertyPlaceholderReplacer.class);
    }
}
