package io.github.connellite.reflection;

import net.openhft.compiler.CachedCompiler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstanceFactoryTest {

    @Test
    void newInstanceWithoutNoArgConstructorUsesObjenesis() throws Exception {
        assertThrows(NoSuchMethodException.class, () -> ReflectionUtil.getInstance(NoDefaultConstructor.class));

        NoDefaultConstructor created = InstanceFactory.newInstance(NoDefaultConstructor.class);

        assertNotNull(created);
        assertEquals(0, created.value);
    }

    @Test
    void newInstanceUsesTheClassFromEachClassLoader() throws Exception {
        String source = """
                package isolate;
                public class SameName {
                    public SameName(int value) {
                        throw new IllegalStateException("constructor");
                    }
                }
                """;
        ClassLoader parent = InstanceFactoryTest.class.getClassLoader();
        ClassLoader firstLoader = new ClassLoader(parent) {
        };
        ClassLoader secondLoader = new ClassLoader(parent) {
        };
        try (CachedCompiler compiler = new CachedCompiler(null, null)) {
            Class<?> first = compiler.loadFromJava(firstLoader, "isolate.SameName", source);
            Class<?> second = compiler.loadFromJava(secondLoader, "isolate.SameName", source);

            assertEquals(first.getName(), second.getName());
            assertNotSame(first, second);
            assertSame(firstLoader, first.getClassLoader());
            assertSame(secondLoader, second.getClassLoader());
            assertSame(first, InstanceFactory.newInstance(first).getClass());
            assertSame(second, InstanceFactory.newInstance(second).getClass());
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    static final class NoDefaultConstructor {
        final int value;

        NoDefaultConstructor(int value) {
            this.value = value;
        }
    }
}
