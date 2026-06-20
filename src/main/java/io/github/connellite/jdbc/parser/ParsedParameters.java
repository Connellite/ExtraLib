package io.github.connellite.jdbc.parser;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("JavadocLinkAsPlainText")
public record ParsedParameters(boolean positional, List<String> parameterNames) {
    public static final ParsedParameters NONE = new ParsedParameters(true, Collections.emptyList());

    public ParsedParameters(boolean positional, List<String> parameterNames) {
        this.positional = positional;
        this.parameterNames = List.copyOf(parameterNames);
    }

    public int getParameterCount() {
        return parameterNames.size();
    }

    /**
     * Static factory of named parsed parameters.
     * Input names must be bare names without SQL prefix characters.
     *
     * @param names parameter names from SQL
     * @return new named parsed parameters
     * @throws IllegalArgumentException when names contain positional marker
     *                                  <p>
     *                                  Jdbi reference:
     *                                  https://github.com/jdbi/jdbi/blob/6e959ba70365fb0e15f19f39e8b2bf32d4998b6c/core/src/main/java/org/jdbi/v3/core/statement/ParsedParameters.java
     */
    public static ParsedParameters named(List<String> names) {
        if (names.contains(ParsedSql.POSITIONAL_PARAM)) {
            throw new IllegalArgumentException("Named parameters list must not contain positional parameter \""
                    + ParsedSql.POSITIONAL_PARAM + "\"");
        }
        return new ParsedParameters(false, names);
    }

    /**
     * Static factory of positional parsed parameters.
     *
     * @param count number of positional parameters in SQL
     * @return new positional parsed parameters
     * <p>
     * Jdbi reference:
     * https://github.com/jdbi/jdbi/blob/6e959ba70365fb0e15f19f39e8b2bf32d4998b6c/core/src/main/java/org/jdbi/v3/core/statement/ParsedParameters.java
     */
    public static ParsedParameters positional(int count) {
        return new ParsedParameters(true, Collections.nCopies(count, ParsedSql.POSITIONAL_PARAM));
    }
}
