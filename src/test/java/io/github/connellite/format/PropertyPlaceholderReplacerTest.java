package io.github.connellite.format;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
