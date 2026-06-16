package io.github.connellite.jdbc.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("JavadocLinkAsPlainText")
public abstract class CachingSqlParser implements SqlParser {
    public static final int PARSED_SQL_CACHE_SIZE = 1_000;

    private final Map<String, ParsedSql> parsedSqlCache;

    public CachingSqlParser() {
        this(PARSED_SQL_CACHE_SIZE);
    }

    public CachingSqlParser(int cacheSize) {
        this.parsedSqlCache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ParsedSql> eldest) {
                return size() > cacheSize;
            }
        });
    }

    /**
     * Parses SQL with caching; equivalent to Jdbi's caching parser contract.
     *
     * @param sql SQL text to parse
     * @return parsed SQL structure
     * <p>
     * Jdbi reference:
     * https://github.com/jdbi/jdbi/blob/6e959ba70365fb0e15f19f39e8b2bf32d4998b6c/core/src/main/java/org/jdbi/v3/core/statement/CachingSqlParser.java
     */
    @Override
    public ParsedSql parse(String sql) {
        Objects.requireNonNull(sql, "sql");
        try {
            ParsedSql parsedSql = parsedSqlCache.get(sql);
            if (parsedSql == null) {
                parsedSql = internalParse(sql);
                parsedSqlCache.put(sql, parsedSql);
            }
            return parsedSql;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Exception parsing for named parameter replacement", e);
        }
    }

    abstract ParsedSql internalParse(String sql);
}
