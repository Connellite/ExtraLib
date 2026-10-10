package io.github.connellite.format;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyPlaceholderReplacerTest {

    @Test
    void replacePlaceholders_fromProperties() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}");
        Properties properties = new Properties();
        properties.setProperty("name", "Ada");
        assertEquals("Hello Ada", format.replacePlaceholders("Hello ${name}", properties));
    }

    @Test
    void replacePlaceholders_defaultValue() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}", ":", true);
        assertEquals("Ada", format.replacePlaceholders("${name:Ada}", key -> null));
        assertEquals("Bob", format.replacePlaceholders("${name:Ada}", key -> "name".equals(key) ? "Bob" : null));
    }

    @Test
    void replacePlaceholders_nestedKeyAndValue() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}");
        assertEquals("ok", format.replacePlaceholders("${${which}}", key -> switch (key) {
            case "which" -> "inner";
            case "inner" -> "ok";
            default -> null;
        }));
        assertEquals("Zed", format.replacePlaceholders("${name}", key -> "name".equals(key) ? "${alias}" : "alias".equals(key) ? "Zed" : null));
    }

    @Test
    void replacePlaceholders_nestedBraces() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}", ":", true);
        assertEquals("v", format.replacePlaceholders("${outer:${inner}}", key -> "inner".equals(key) ? "v" : null));
    }

    @Test
    void replacePlaceholders_ignoreOrFailUnresolved() {
        PropertyPlaceholderReplacer ignore = new PropertyPlaceholderReplacer("${", "}");
        assertEquals("x ${missing} y", ignore.replacePlaceholders("x ${missing} y", key -> null));

        PropertyPlaceholderReplacer fail = new PropertyPlaceholderReplacer("${", "}", null, false);
        assertThrows(IllegalArgumentException.class, () -> fail.replacePlaceholders("${missing}", key -> null));
    }

    @Test
    void replacePlaceholders_circularThrows() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}", null, false);
        assertThrows(IllegalArgumentException.class, () -> format.replacePlaceholders("${a}", key -> "a".equals(key) ? "${b}" : "${a}"));
    }

    @Test
    void replacePlaceholders_escapePrefix() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}", null, '\\', true);
        Properties properties = new Properties();
        properties.setProperty("name", "Ada");
        assertEquals("Hello ${name} Ada", format.replacePlaceholders("Hello \\${name} ${name}", properties));
    }

    @Test
    void replacePlaceholders_requiresNonNull() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}");
        assertThrows(NullPointerException.class, () -> new PropertyPlaceholderReplacer(null, "}"));
        assertThrows(NullPointerException.class, () -> format.replacePlaceholders(null, new Properties()));
        assertThrows(NullPointerException.class, () -> format.replacePlaceholders("x", (Properties) null));
    }

    @Test
    void replacePlaceholdersCached_storesTheResultAndDoesNotGrowOnAHit() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}");
        assertTrue(format.replacementCache().isEmpty());

        Properties properties = new Properties();
        properties.setProperty("name", "Ada");
        String template = "Hello ${name}";
        String first = format.replacePlaceholdersCached(template, properties);

        PropertyPlaceholderReplacer.CacheKey adaKey = new PropertyPlaceholderReplacer.CacheKey(
                template, List.of(new PropertyPlaceholderReplacer.PropertyEntry("name", "Ada")), null);
        assertEquals(1, format.replacementCache().size());
        assertSame(first, format.replacementCache().get(adaKey));

        assertSame(first, format.replacePlaceholdersCached(template, properties));
        assertEquals(1, format.replacementCache().size());

        format.replacePlaceholders(template, properties);
        assertEquals(1, format.replacementCache().size());

        properties.setProperty("name", "Bob");
        String second = format.replacePlaceholdersCached(template, properties);
        PropertyPlaceholderReplacer.CacheKey bobKey = new PropertyPlaceholderReplacer.CacheKey(
                template, List.of(new PropertyPlaceholderReplacer.PropertyEntry("name", "Bob")), null);
        assertEquals("Hello Bob", second);
        assertEquals(2, format.replacementCache().size());
        assertSame(second, format.replacementCache().get(bobKey));
        assertSame(first, format.replacementCache().get(adaKey));
    }

    @Test
    void replacePlaceholdersCached_reusesOneQueryForSeveralTables() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}");
        String template = "SELECT * FROM ${schema}.${table}";
        String[][] tables = {
                {"sales", "orders"},
                {"sales", "customers"},
                {"archive", "orders"}
        };

        String[] cached = new String[tables.length];
        for (int i = 0; i < tables.length; i++) {
            cached[i] = format.replacePlaceholdersCached(template, tableProperties(tables[i][0], tables[i][1]));
        }
        assertEquals("SELECT * FROM sales.orders", cached[0]);
        assertEquals("SELECT * FROM sales.customers", cached[1]);
        assertEquals("SELECT * FROM archive.orders", cached[2]);
        assertEquals(tables.length, format.replacementCache().size());

        for (int i = 0; i < tables.length; i++) {
            assertSame(cached[i], format.replacePlaceholdersCached(template, tableProperties(tables[i][0], tables[i][1])));
        }
        assertEquals(tables.length, format.replacementCache().size());
    }

    private static Properties tableProperties(String schema, String table) {
        Properties properties = new Properties();
        properties.setProperty("schema", schema);
        properties.setProperty("table", table);
        return properties;
    }

    @Test
    void replacePlaceholdersCached_storesTemplateLongerThan256() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}");
        Properties properties = new Properties();
        properties.setProperty("name", "Ada");
        String template = "x".repeat(257) + "${name}";
        String first = format.replacePlaceholdersCached(template, properties);
        assertEquals("x".repeat(257) + "Ada", first);
        assertEquals(1, format.replacementCache().size());
        assertSame(first, format.replacementCache().values().iterator().next());
        assertSame(first, format.replacePlaceholdersCached(template, properties));
        assertEquals(1, format.replacementCache().size());
    }

    @Test
    void replacePlaceholdersCached_resolverHitSkipsLaterResolveCalls() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}");
        int[] calls = {0};
        PropertyPlaceholderReplacer.PlaceholderResolver resolver = key -> {
            calls[0]++;
            return "Ada";
        };
        String template = "Hello ${name}";
        String first = format.replacePlaceholdersCached(template, resolver);

        assertEquals("Hello Ada", first);
        assertEquals(1, calls[0]);
        assertEquals(1, format.replacementCache().size());
        assertSame(first, format.replacementCache().get(
                new PropertyPlaceholderReplacer.CacheKey(template, List.of(), resolver)));

        assertSame(first, format.replacePlaceholdersCached(template, resolver));
        assertEquals(1, calls[0]);
        assertEquals(1, format.replacementCache().size());

        format.replacePlaceholders(template, resolver);
        assertEquals(2, calls[0]);
        assertEquals(1, format.replacementCache().size());
    }

    @Test
    void replacePlaceholdersCached_differentResolverInstanceIsAnotherEntry() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}");
        String template = "Hello ${name}";
        PropertyPlaceholderReplacer.PlaceholderResolver ada = key -> "Ada";
        PropertyPlaceholderReplacer.PlaceholderResolver bob = key -> "Bob";

        String first = format.replacePlaceholdersCached(template, ada);
        String second = format.replacePlaceholdersCached(template, bob);

        assertEquals("Hello Ada", first);
        assertEquals("Hello Bob", second);
        assertEquals(2, format.replacementCache().size());
        assertSame(first, format.replacementCache().get(
                new PropertyPlaceholderReplacer.CacheKey(template, List.of(), ada)));
        assertSame(second, format.replacementCache().get(
                new PropertyPlaceholderReplacer.CacheKey(template, List.of(), bob)));
    }

    @Test
    void replacePlaceholdersCached_resolverFailureIsNotStored() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}", null, false);
        PropertyPlaceholderReplacer.PlaceholderResolver missing = key -> null;
        assertThrows(IllegalArgumentException.class, () -> format.replacePlaceholdersCached("${missing}", missing));
        assertTrue(format.replacementCache().isEmpty());

        PropertyPlaceholderReplacer.PlaceholderResolver circular = key -> "a".equals(key) ? "${b}" : "${a}";
        assertThrows(IllegalArgumentException.class, () -> format.replacePlaceholdersCached("${a}", circular));
        assertTrue(format.replacementCache().isEmpty());
    }

    @Test
    void replacePlaceholdersCached_ignoredUnresolvedResolverResultIsStored() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}");
        int[] calls = {0};
        PropertyPlaceholderReplacer.PlaceholderResolver resolver = key -> {
            calls[0]++;
            return null;
        };
        String first = format.replacePlaceholdersCached("x ${missing} y", resolver);
        assertEquals("x ${missing} y", first);
        assertEquals(1, calls[0]);

        assertSame(first, format.replacePlaceholdersCached("x ${missing} y", resolver));
        assertEquals(1, calls[0]);
        assertEquals(1, format.replacementCache().size());
    }

    @Test
    void replacePlaceholdersCached_nestedResolverResultIsStoredOnce() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}");
        int[] calls = {0};
        PropertyPlaceholderReplacer.PlaceholderResolver resolver = key -> {
            calls[0]++;
            return switch (key) {
                case "which" -> "inner";
                case "inner" -> "ok";
                default -> null;
            };
        };
        String first = format.replacePlaceholdersCached("${${which}}", resolver);
        assertEquals("ok", first);
        int resolvedCalls = calls[0];
        assertTrue(resolvedCalls > 1);

        assertSame(first, format.replacePlaceholdersCached("${${which}}", resolver));
        assertEquals(resolvedCalls, calls[0]);
        assertEquals(1, format.replacementCache().size());
    }

    @Test
    void replacePlaceholdersCached_defaultValueIsStored() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}", ":", true);
        int[] calls = {0};
        PropertyPlaceholderReplacer.PlaceholderResolver resolver = key -> {
            calls[0]++;
            return null;
        };
        String first = format.replacePlaceholdersCached("${name:Ada}", resolver);
        assertEquals("Ada", first);
        int resolvedCalls = calls[0];
        assertSame(first, format.replacePlaceholdersCached("${name:Ada}", resolver));
        assertEquals(resolvedCalls, calls[0]);
        assertEquals(1, format.replacementCache().size());
    }

    @Test
    void replacePlaceholdersCached_nullArgumentsLeaveTheCacheEmpty() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}");
        assertThrows(NullPointerException.class, () -> format.replacePlaceholdersCached(null, new Properties()));
        assertThrows(NullPointerException.class, () -> format.replacePlaceholdersCached("x", (Properties) null));
        assertThrows(NullPointerException.class,
                () -> format.replacePlaceholdersCached(null, (PropertyPlaceholderReplacer.PlaceholderResolver) key -> "v"));
        assertThrows(NullPointerException.class,
                () -> format.replacePlaceholdersCached("x", (PropertyPlaceholderReplacer.PlaceholderResolver) null));
        assertTrue(format.replacementCache().isEmpty());
    }

    @Test
    void replacePlaceholdersCached_propertiesAndResolverDoNotShareAnEntry() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}");
        Properties properties = new Properties();
        properties.setProperty("name", "Ada");
        PropertyPlaceholderReplacer.PlaceholderResolver resolver = key -> "Ada";
        String fromProperties = format.replacePlaceholdersCached("Hello ${name}", properties);
        String fromResolver = format.replacePlaceholdersCached("Hello ${name}", resolver);

        assertEquals(fromProperties, fromResolver);
        assertEquals(2, format.replacementCache().size());
    }

    @Test
    void replacePlaceholdersCached_dropsTheLeastRecentlyUsedEntry() {
        PropertyPlaceholderReplacer format = new PropertyPlaceholderReplacer("${", "}");
        PropertyPlaceholderReplacer.PlaceholderResolver resolver = key -> "v";
        String oldest = "t0 ${x}";
        format.replacePlaceholdersCached(oldest, resolver);
        int size = 1;
        int evictedAt = -1;
        for (int i = 1; i < 10_000; i++) {
            format.replacePlaceholdersCached(oldest, resolver);
            format.replacePlaceholdersCached("t" + i + " ${x}", resolver);
            int next = format.replacementCache().size();
            if (next == size) {
                evictedAt = i;
                break;
            }
            size = next;
        }
        assertTrue(evictedAt > 1);
        assertTrue(format.replacementCache().containsKey(
                new PropertyPlaceholderReplacer.CacheKey(oldest, List.of(), resolver)));
        assertFalse(format.replacementCache().containsKey(
                new PropertyPlaceholderReplacer.CacheKey("t1 ${x}", List.of(), resolver)));
        assertTrue(format.replacementCache().containsKey(
                new PropertyPlaceholderReplacer.CacheKey("t" + evictedAt + " ${x}", List.of(), resolver)));
    }
}
