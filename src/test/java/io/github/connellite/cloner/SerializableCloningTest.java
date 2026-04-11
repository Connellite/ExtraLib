package io.github.connellite.cloner;

import io.github.connellite.exception.CloningException;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SerializableCloningTest {

    @Test
    void clone_null_returnsNull() {
        assertNull(SerializableCloning.clone(null));
    }

    @Test
    void clone_serializablePojo_roundTrip() {
        Payload p = new Payload(10, "hi");
        Payload copy = SerializableCloning.clone(p);

        assertNotSame(p, copy);
        assertEquals(p.value, copy.value);
        assertEquals(p.label, copy.label);
    }

    @Test
    void clone_nonSerializable_throwsCloningException() {
        Object o = new Object();
        assertThrows(CloningException.class, () -> SerializableCloning.clone(o));
    }

    record Payload(int value, String label) implements Serializable {
            @Serial
            private static final long serialVersionUID = 1L;
    }
}
