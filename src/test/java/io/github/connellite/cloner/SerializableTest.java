package io.github.connellite.cloner;

import io.github.connellite.exception.CloningException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializableTest {

    @Test
    void serialize_deserialize_null_roundTrip() throws Exception {
        byte[] bytes = Serializable.serialize(null);
        assertTrue(bytes.length > 0);
        assertNull(Serializable.deserialize(bytes));
    }

    @Test
    void serialize_deserialize_payload_roundTrip() throws Exception {
        Payload p = new Payload(7, "x");
        byte[] bytes = Serializable.serialize(p);
        assertTrue(bytes.length > 0);

        Object read = Serializable.deserialize(bytes);
        assertTrue(read instanceof Payload);
        Payload copy = (Payload) read;
        assertNotSame(p, copy);
        assertEquals(p.value, copy.value);
        assertEquals(p.label, copy.label);
    }

    @Test
    void serialize_nonSerializable_throwsIOException() {
        assertThrows(IOException.class, () -> Serializable.serialize(new Object()));
    }

    @Test
    void deserialize_invalidBytes_throwsIOException() {
        byte[] garbage = {0, 1, 2, 3};
        assertThrows(IOException.class, () -> Serializable.deserialize(garbage));
    }

    @Test
    void writeObjectToFile_readObjectFromFile_roundTrip(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("obj.bin");
        Payload p = new Payload(42, "file");

        Serializable.writeObjectToFile(p, file);
        assertTrue(Files.exists(file));
        assertTrue(Files.size(file) > 0);

        Object read = Serializable.readObjectFromFile(file);
        assertTrue(read instanceof Payload);
        Payload copy = (Payload) read;
        assertNotSame(p, copy);
        assertEquals(p.value, copy.value);
        assertEquals(p.label, copy.label);
    }

    @Test
    void readObjectFromFile_missingFile_throwsIOException(@TempDir Path dir) {
        Path missing = dir.resolve("missing.bin");
        assertThrows(IOException.class, () -> Serializable.readObjectFromFile(missing));
    }

    @Test
    void clone_null_returnsNull() {
        assertNull(Serializable.clone(null));
    }

    @Test
    void clone_serializablePojo_roundTrip() {
        Payload p = new Payload(10, "hi");
        Payload copy = Serializable.clone(p);

        assertNotSame(p, copy);
        assertEquals(p.value, copy.value);
        assertEquals(p.label, copy.label);
    }

    @Test
    void clone_nonSerializable_throwsCloningException() {
        Object o = new Object();
        assertThrows(CloningException.class, () -> Serializable.clone(o));
    }

    record Payload(int value, String label) implements java.io.Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
