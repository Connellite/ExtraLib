package io.github.connellite.jdbc.internal;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"JavadocLinkAsPlainText", "UnusedReturnValue"})
public record ParsedSql(String sql, ParsedParameters parameters) {
        static final String POSITIONAL_PARAM = "?";

        /**
         * Static factory of parsed SQL.
         *
         * @param sql SQL text containing positional placeholders
         * @param parameters ordered parameter metadata
         * @return new parsed SQL instance
         * <p>
         * Jdbi reference:
         * https://github.com/jdbi/jdbi/blob/6e959ba70365fb0e15f19f39e8b2bf32d4998b6c/core/src/main/java/org/jdbi/v3/core/statement/ParsedSql.java
         */
        static ParsedSql of(String sql, ParsedParameters parameters) {
            return new ParsedSql(sql, parameters);
        }

        /**
         * Creates a new {@link Builder} for parsed SQL.
         *
         * @return builder instance
         * <p>
         * Jdbi reference:
         * https://github.com/jdbi/jdbi/blob/6e959ba70365fb0e15f19f39e8b2bf32d4998b6c/core/src/main/java/org/jdbi/v3/core/statement/ParsedSql.java
         */
        static Builder builder() {
            return new Builder();
        }

        static final class Builder {
            private final StringBuilder sql = new StringBuilder();
            private boolean positional = false;
            private boolean named = false;
            private final List<String> parameterNames = new ArrayList<>();

            /**
             * Appends an SQL fragment to the SQL text.
             *
             * @param sqlFragment SQL fragment
             * @return this builder
             * <p>
             * Jdbi reference:
             * https://github.com/jdbi/jdbi/blob/6e959ba70365fb0e15f19f39e8b2bf32d4998b6c/core/src/main/java/org/jdbi/v3/core/statement/ParsedSql.java
             */
            Builder append(String sqlFragment) {
                sql.append(sqlFragment);
                return this;
            }

            /**
             * Records a named parameter and appends positional {@code ?}.
             *
             * @param name named parameter
             * @return this builder
             * <p>
             * Jdbi reference:
             * https://github.com/jdbi/jdbi/blob/6e959ba70365fb0e15f19f39e8b2bf32d4998b6c/core/src/main/java/org/jdbi/v3/core/statement/ParsedSql.java
             */
            Builder appendNamedParameter(String name) {
                named = true;
                parameterNames.add(name);
                return append(POSITIONAL_PARAM);
            }

            /**
             * Records a positional parameter and appends positional {@code ?}.
             *
             * @return this builder
             * <p>
             * Jdbi reference:
             * https://github.com/jdbi/jdbi/blob/6e959ba70365fb0e15f19f39e8b2bf32d4998b6c/core/src/main/java/org/jdbi/v3/core/statement/ParsedSql.java
             */
            Builder appendPositionalParameter() {
                positional = true;
                parameterNames.add(POSITIONAL_PARAM);
                return append(POSITIONAL_PARAM);
            }

            /**
             * Finalizes parsed SQL and validates that named and positional parameters are not mixed.
             *
             * @return finalized parsed SQL
             * <p>
             * Jdbi reference:
             * https://github.com/jdbi/jdbi/blob/6e959ba70365fb0e15f19f39e8b2bf32d4998b6c/core/src/main/java/org/jdbi/v3/core/statement/ParsedSql.java
             */
            ParsedSql build() {
                if (positional && named) {
                    throw new IllegalArgumentException(
                            "Cannot mix named and positional parameters in a SQL statement: " + parameterNames);
                }

                ParsedParameters parameters = new ParsedParameters(positional, parameterNames);
                return new ParsedSql(sql.toString(), parameters);
            }
        }
    }