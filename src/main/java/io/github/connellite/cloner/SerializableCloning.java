package io.github.connellite.cloner;

import io.github.connellite.exception.CloningException;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Cloning by Java serialization: the type must implement {@link Serializable} (directly or via supertypes)
 * and follow serialization semantics (e.g. {@code serialVersionUID}, {@code transient}, custom
 * {@code writeObject}/{@code readObject} where used).
 */
@UtilityClass
public class SerializableCloning {

    /**
     * Returns a deep copy of {@code original} by writing it to a byte array and reading it back.
     * Fails at runtime if the object graph is not serializable.
     *
     * @param original object to clone; may be {@code null}
     * @param <T>      compile-time type of {@code original}
     * @return a new instance graph, or {@code null} if {@code original} was {@code null}
     * @throws CloningException if serialization or deserialization fails
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(T original) {
        if (original == null) {
            return null;
        }

        try {
            byte[] bytes;
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream(5120);
                 ObjectOutputStream out = new ObjectOutputStream(bos)) {
                out.writeObject(original);
                out.flush();
                bytes = bos.toByteArray();
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (T) in.readObject();
            }
        } catch (Exception e) {
            throw new CloningException(e.getMessage(), e);
        }
    }
}
