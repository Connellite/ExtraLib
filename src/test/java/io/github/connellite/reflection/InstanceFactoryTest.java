package io.github.connellite.reflection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstanceFactoryTest {

    @Test
    void newInstanceWithoutNoArgConstructorUsesObjenesis() throws Exception {
        assertThrows(NoSuchMethodException.class, () -> ReflectionUtil.getInstance(NoDefaultConstructor.class));

        NoDefaultConstructor created = InstanceFactory.newInstance(NoDefaultConstructor.class);

        assertNotNull(created);
        assertEquals(0, created.value);
    }

    @SuppressWarnings("ClassCanBeRecord")
    static final class NoDefaultConstructor {
        final int value;

        NoDefaultConstructor(int value) {
            this.value = value;
        }
    }
}
