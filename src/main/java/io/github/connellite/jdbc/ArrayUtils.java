package io.github.connellite.jdbc;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

@UtilityClass
public class ArrayUtils {
    /**
     * Creates a JDBC {@link Array} from an object array.
     *
     * @param connection open JDBC connection used to create array instances
     * @param typeName database-specific SQL array element type name (for example, {@code varchar})
     * @param elements source elements; {@code null} is treated as an empty array
     * @return JDBC {@link Array} created by {@link Connection#createArrayOf(String, Object[])}
     */
    public static Array toArray(@NonNull Connection connection, @NonNull String typeName, Object[] elements) throws SQLException {
        if (elements == null) {
            elements = new Object[0];
        }
        return connection.createArrayOf(typeName, elements);
    }

    /**
     * Creates a JDBC {@link Array} from a {@link Collection}.
     *
     * @param connection open JDBC connection used to create array instances
     * @param typeName database-specific SQL array element type name (for example, {@code varchar})
     * @param elements source elements; {@code null} is treated as an empty collection
     * @return JDBC {@link Array} created by {@link Connection#createArrayOf(String, Object[])}
     */
    public static Array toArray(@NonNull Connection connection, @NonNull String typeName, Collection<?> elements) throws SQLException {
        if (elements == null) {
            elements = Collections.emptyList();
        }
        return connection.createArrayOf(typeName, elements.toArray());
    }

}
