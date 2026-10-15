package io.github.connellite.reflection;

import io.github.connellite.reflection.internal.ForeignMemory;
import io.github.connellite.reflection.internal.UnsafeReflectionUtil0;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Field access and the base address of a direct byte buffer.
 * <p>
 * Object fields go through {@code sun.misc.Unsafe} when that class can be used.
 * The address of a direct buffer prefers {@code java.lang.foreign.MemorySegment} on Java 22 and
 * later, and otherwise reads {@code java.nio.Buffer.address}. If neither path is available,
 * {@link #isAvailable()} is {@code false} and the other methods throw {@link IllegalStateException}.
 * Loading this class does not fail when {@code sun.misc.Unsafe} is absent.
 */
@UtilityClass
public class UnsafeReflectionUtil {

    private static final class Availability {
        private static final boolean UNSAFE;
        private static final boolean FOREIGN;
        private static final boolean ANY;

        static {
            boolean unsafeAvailable = false;
            try {
                unsafeAvailable = UnsafeReflectionUtil0.hasUnsafe();
            } catch (Throwable ignored) {
            }
            boolean foreignAvailable = false;
            try {
                foreignAvailable = ForeignMemory.isAvailable();
            } catch (Throwable ignored) {
            }
            UNSAFE = unsafeAvailable;
            FOREIGN = foreignAvailable;
            ANY = unsafeAvailable || foreignAvailable;
        }
    }

    /**
     * Returns whether field access or a direct-buffer address can be served.
     */
    public static boolean isAvailable() {
        return Availability.ANY;
    }

    /**
     * Returns the reference bits stored for {@code object}.
     * {@code null} is {@code 0}. The value is the slot contents of an {@code Object[]},
     * which is a narrow reference when the JVM compresses ordinary object pointers.
     *
     * @param object referenced object, or {@code null}
     * @return reference bits, or {@code 0} when {@code object} is {@code null}
     * @throws IllegalStateException when field access is unavailable
     */
    public static long referenceBitsOf(Object object) {
        if (object == null) {
            return 0L;
        }
        requireUnsafe();
        try {
            return UnsafeReflectionUtil0.referenceBitsOf(object);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /**
     * Returns the offset of an instance field.
     *
     * @param field instance field
     * @return byte offset of {@code field} within its declaring class
     * @throws IllegalStateException when field access is unavailable
     */
    public static long objectFieldOffset(Field field) {
        Objects.requireNonNull(field, "field");
        requireUnsafe();
        try {
            return UnsafeReflectionUtil0.objectFieldOffset(field);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /**
     * Returns the offset of a static field.
     *
     * @param field static field
     * @return byte offset of {@code field}
     * @throws IllegalStateException when field access is unavailable
     */
    public static long staticFieldOffset(Field field) {
        Objects.requireNonNull(field, "field");
        requireUnsafe();
        try {
            return UnsafeReflectionUtil0.staticFieldOffset(field);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /**
     * Returns the base object for a static field access.
     *
     * @param field static field
     * @return base to pass with {@link #staticFieldOffset(Field)}
     * @throws IllegalStateException when field access is unavailable
     */
    public static Object staticFieldBase(Field field) {
        Objects.requireNonNull(field, "field");
        requireUnsafe();
        try {
            return UnsafeReflectionUtil0.staticFieldBase(field);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /**
     * Reads a reference at {@code offset} within {@code base}.
     *
     * @throws IllegalStateException when field access is unavailable
     */
    public static Object getObject(Object base, long offset) {
        requireUnsafe();
        try {
            return UnsafeReflectionUtil0.getObject(base, offset);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /**
     * Writes a reference at {@code offset} within {@code base}.
     *
     * @throws IllegalStateException when field access is unavailable
     */
    public static void putObject(Object base, long offset, Object value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putObject(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /**
     * Writes a reference with {@code volatile} semantics.
     *
     * @throws IllegalStateException when field access is unavailable
     */
    public static void putObjectVolatile(Object base, long offset, Object value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putObjectVolatile(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static boolean getBoolean(Object base, long offset) {
        requireUnsafe();
        try {
            return UnsafeReflectionUtil0.getBoolean(base, offset);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putBoolean(Object base, long offset, boolean value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putBoolean(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putBooleanVolatile(Object base, long offset, boolean value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putBooleanVolatile(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static byte getByte(Object base, long offset) {
        requireUnsafe();
        try {
            return UnsafeReflectionUtil0.getByte(base, offset);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putByte(Object base, long offset, byte value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putByte(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putByteVolatile(Object base, long offset, byte value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putByteVolatile(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static char getChar(Object base, long offset) {
        requireUnsafe();
        try {
            return UnsafeReflectionUtil0.getChar(base, offset);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putChar(Object base, long offset, char value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putChar(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putCharVolatile(Object base, long offset, char value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putCharVolatile(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static short getShort(Object base, long offset) {
        requireUnsafe();
        try {
            return UnsafeReflectionUtil0.getShort(base, offset);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putShort(Object base, long offset, short value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putShort(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putShortVolatile(Object base, long offset, short value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putShortVolatile(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static int getInt(Object base, long offset) {
        requireUnsafe();
        try {
            return UnsafeReflectionUtil0.getInt(base, offset);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putInt(Object base, long offset, int value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putInt(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putIntVolatile(Object base, long offset, int value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putIntVolatile(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static long getLong(Object base, long offset) {
        requireUnsafe();
        try {
            return UnsafeReflectionUtil0.getLong(base, offset);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putLong(Object base, long offset, long value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putLong(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putLongVolatile(Object base, long offset, long value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putLongVolatile(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static float getFloat(Object base, long offset) {
        requireUnsafe();
        try {
            return UnsafeReflectionUtil0.getFloat(base, offset);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putFloat(Object base, long offset, float value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putFloat(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putFloatVolatile(Object base, long offset, float value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putFloatVolatile(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static double getDouble(Object base, long offset) {
        requireUnsafe();
        try {
            return UnsafeReflectionUtil0.getDouble(base, offset);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putDouble(Object base, long offset, double value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putDouble(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /** @throws IllegalStateException when field access is unavailable */
    public static void putDoubleVolatile(Object base, long offset, double value) {
        requireUnsafe();
        try {
            UnsafeReflectionUtil0.putDoubleVolatile(base, offset, value);
        } catch (LinkageError e) {
            throw unavailable(e);
        }
    }

    /**
     * Returns the address of index 0 in a direct buffer.
     * On Java 22 and later this is {@code MemorySegment.ofBuffer(buffer).address()} minus
     * {@link ByteBuffer#position()}. If that call fails, or the segment API is absent, the address
     * is the {@code java.nio.Buffer.address} field.
     *
     * @param buffer direct byte buffer
     * @return base address, not including {@code buffer.position()}
     * @throws IllegalArgumentException when {@code buffer} is not direct
     * @throws IllegalStateException    when neither address path is available
     */
    public static long directBufferAddress(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer");
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("buffer must be direct");
        }
        Throwable foreignFailure = null;
        if (Availability.FOREIGN) {
            try {
                return ForeignMemory.addressOf(buffer) - buffer.position();
            } catch (Throwable e) {
                foreignFailure = e;
            }
        }
        if (!Availability.UNSAFE) {
            throw unavailable(foreignFailure);
        }
        long offset = UnsafeReflectionUtil0.bufferAddressOffset();
        if (offset < 0L) {
            throw unavailable(foreignFailure);
        }
        try {
            return UnsafeReflectionUtil0.getLong(buffer, offset);
        } catch (LinkageError e) {
            if (foreignFailure != null) {
                e.addSuppressed(foreignFailure);
            }
            throw unavailable(e);
        }
    }

    private static void requireUnsafe() {
        if (!Availability.UNSAFE) {
            throw unavailable(null);
        }
    }

    private static IllegalStateException unavailable(Throwable cause) {
        String message = "sun.misc.Unsafe is unavailable";
        if (cause == null) {
            return new IllegalStateException(message);
        }
        return new IllegalStateException(message, cause);
    }
}
