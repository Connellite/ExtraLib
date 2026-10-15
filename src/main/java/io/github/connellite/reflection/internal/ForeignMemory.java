package io.github.connellite.reflection.internal;

import lombok.experimental.UtilityClass;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * {@code java.lang.foreign.MemorySegment} bound by name so this library still compiles on Java 17.
 * The handle stays {@code null} below Java 22 and when {@code address()} cannot be called.
 */
@UtilityClass
public final class ForeignMemory {

    private static final MethodHandle ADDRESS_OF_BUFFER;

    static {
        MethodHandle handle = null;
        try {
            if (Runtime.version().feature() >= 22) {
                handle = bindAddressOfBuffer();
            }
        } catch (Throwable ignored) {
        }
        ADDRESS_OF_BUFFER = handle;
    }

    public static boolean isAvailable() {
        return ADDRESS_OF_BUFFER != null;
    }

    /**
     * @return {@code MemorySegment.ofBuffer(buffer).address()}, which includes {@code buffer.position()}
     */
    public static long addressOf(ByteBuffer buffer) throws Throwable {
        return (long) ADDRESS_OF_BUFFER.invokeExact((Buffer) buffer);
    }

    private static MethodHandle bindAddressOfBuffer() {
        try {
            Class<?> memorySegment = Class.forName("java.lang.foreign.MemorySegment");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodHandle ofBuffer = lookup.findStatic(
                    memorySegment, "ofBuffer", MethodType.methodType(memorySegment, Buffer.class));
            MethodHandle address = lookup.findVirtual(memorySegment, "address", MethodType.methodType(long.class));
            MethodHandle addressOfBuffer = MethodHandles.filterArguments(address, 0, ofBuffer);
            long ignoredAddress = (long) addressOfBuffer.invokeExact((Buffer) ByteBuffer.allocateDirect(1));
            if (ignoredAddress == 0L) {
                return null;
            }
            return addressOfBuffer;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
