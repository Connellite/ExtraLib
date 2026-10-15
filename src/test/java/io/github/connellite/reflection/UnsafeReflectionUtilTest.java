package io.github.connellite.reflection;

import io.github.connellite.reflection.internal.ForeignMemory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnsafeReflectionUtilTest {

    @Test
    void loadsWithoutThrowing() {
        assertTrue(UnsafeReflectionUtil.isAvailable());
        if (Runtime.version().feature() < 22) {
            assertFalse(ForeignMemory.isAvailable());
        }
    }

    @Test
    void readsAndWritesPrivateField() throws Exception {
        Box box = new Box();
        Field name = Box.class.getDeclaredField("name");
        long offset = UnsafeReflectionUtil.objectFieldOffset(name);

        assertEquals("old", UnsafeReflectionUtil.getObject(box, offset));
        UnsafeReflectionUtil.putObject(box, offset, "next");
        assertEquals("next", UnsafeReflectionUtil.getObject(box, offset));
    }

    @Test
    void replacesFinalFieldThroughVolatilePut() throws Exception {
        Locked locked = new Locked("old");
        Field name = Locked.class.getDeclaredField("name");
        long offset = UnsafeReflectionUtil.objectFieldOffset(name);

        UnsafeReflectionUtil.putObjectVolatile(locked, offset, "new");
        assertEquals("new", UnsafeReflectionUtil.getObject(locked, offset));
    }

    @Test
    void directBufferAddressMatchesAddressField() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        buffer.position(3);
        Field address = Buffer.class.getDeclaredField("address");
        long offset = UnsafeReflectionUtil.objectFieldOffset(address);

        assertEquals(UnsafeReflectionUtil.getLong(buffer, offset), UnsafeReflectionUtil.directBufferAddress(buffer));
    }

    @Test
    void heapBufferIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> UnsafeReflectionUtil.directBufferAddress(ByteBuffer.allocate(8)));
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_22)
    void directBufferAddressMatchesMemorySegment() throws Throwable {
        assertTrue(ForeignMemory.isAvailable());
        ByteBuffer buffer = ByteBuffer.allocateDirect(16);
        buffer.position(4);

        long expected = memorySegmentAddress(buffer) - buffer.position();
        assertEquals(expected, UnsafeReflectionUtil.directBufferAddress(buffer));
    }

    private static long memorySegmentAddress(ByteBuffer buffer) throws Throwable {
        Class<?> memorySegment = Class.forName("java.lang.foreign.MemorySegment");
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodHandle ofBuffer = lookup.findStatic(
                memorySegment, "ofBuffer", MethodType.methodType(memorySegment, Buffer.class));
        MethodHandle address = lookup.findVirtual(memorySegment, "address", MethodType.methodType(long.class));
        MethodHandle addressOfBuffer = MethodHandles.filterArguments(address, 0, ofBuffer);
        return (long) addressOfBuffer.invokeExact((Buffer) buffer);
    }

    private static final class Box {
        private String name = "old";
    }

    private static final class Locked {
        private final String name;

        private Locked(String name) {
            this.name = name;
        }
    }
}
