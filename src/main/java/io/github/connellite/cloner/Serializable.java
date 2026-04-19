package io.github.connellite.cloner;

import io.github.connellite.exception.CloningException;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helpers around Java object serialization: in-memory bytes, file I/O, and deep copy via a memory round-trip.
 * <p>
 * Callers are responsible for choosing serializable types and stable {@code serialVersionUID} where needed.
 */
@UtilityClass
public class Serializable {

    private static final int BYTE_BUFFER_INITIAL_CAPACITY = 5120;

    /**
     * Serializes {@code obj} with {@link ObjectOutputStream#writeObject(Object)}.
     *
     * @param obj object to serialize; may be {@code null} (stored as a null marker in the stream)
     * @return non-null byte array containing the serialized form
     * @throws IOException if serialization fails
     */
    public static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream(BYTE_BUFFER_INITIAL_CAPACITY);
             ObjectOutputStream o = new ObjectOutputStream(b)) {
            o.writeObject(obj);
            return b.toByteArray();
        }
    }

    /**
     * Deserializes bytes produced by {@link #serialize(Object)} (or any compatible Java serialization stream).
     *
     * @param bytes serialized data; must not be {@code null}
     * @return deserialized object, which may be {@code null} if a null marker was written
     * @throws IOException            if the stream is corrupt or cannot be read
     * @throws ClassNotFoundException if a class referenced in the stream is not found
     */
    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream b = new ByteArrayInputStream(bytes);
             ObjectInputStream o = new ObjectInputStream(b)) {
            return o.readObject();
        }
    }

    /**
     * Writes {@code obj} to {@code pathOutFile} using object serialization. The file is created or truncated.
     *
     * @param obj          object to persist
     * @param pathOutFile  destination path
     * @throws IOException if the file cannot be opened or serialization fails
     */
    public static void writeObjectToFile(Object obj, Path pathOutFile) throws IOException {
        try (OutputStream os = Files.newOutputStream(pathOutFile);
             ObjectOutputStream oos = new ObjectOutputStream(os)) {
            oos.writeObject(obj);
        }
    }

    /**
     * Reads an object from a file previously written with {@link #writeObjectToFile(Object, Path)}.
     *
     * @param pathInputFile file to read
     * @return deserialized object, which may be {@code null}
     * @throws IOException            if the file cannot be read or data is invalid
     * @throws ClassNotFoundException if a class referenced in the stream is not found
     */
    public static Object readObjectFromFile(Path pathInputFile) throws IOException, ClassNotFoundException {
        try (InputStream fs = Files.newInputStream(pathInputFile);
             ObjectInputStream oin = new ObjectInputStream(fs)) {
            return oin.readObject();
        }
    }

    /**
     * Returns a deep copy of {@code original} by serializing to a byte array and deserializing again.
     * The object graph must be serializable (see {@link java.io.Serializable} and custom hooks such as
     * {@code writeObject}/{@code readObject} where applicable).
     *
     * @param original root of the graph to copy; may be {@code null}
     * @param <T>      compile-time type of {@code original}
     * @return a new graph, or {@code null} if {@code original} was {@code null}
     * @throws CloningException if serialization or deserialization fails
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(T original) {
        if (original == null) {
            return null;
        }

        try {
            byte[] bytes = serialize(original);
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (T) in.readObject();
            }
        } catch (Exception e) {
            throw new CloningException(e.getMessage(), e);
        }
    }
}
