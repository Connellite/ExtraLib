package io.github.connellite.reflection.internal;

import lombok.experimental.UtilityClass;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.Buffer;

@UtilityClass
public final class UnsafeReflectionUtil0 {

    public static boolean hasUnsafe() {
        return SunMisc.UNSAFE != null && SunMisc.UNAVAILABILITY_CAUSE == null;
    }

    public static long bufferAddressOffset() {
        return SunMisc.BUFFER_ADDRESS_OFFSET;
    }

    public static long objectFieldOffset(Field field) {
        return SunMisc.UNSAFE.objectFieldOffset(field);
    }

    public static long staticFieldOffset(Field field) {
        return SunMisc.UNSAFE.staticFieldOffset(field);
    }

    public static Object staticFieldBase(Field field) {
        return SunMisc.UNSAFE.staticFieldBase(field);
    }

    public static Object getObject(Object base, long offset) {
        return SunMisc.UNSAFE.getObject(base, offset);
    }

    public static void putObject(Object base, long offset, Object value) {
        SunMisc.UNSAFE.putObject(base, offset, value);
    }

    public static void putObjectVolatile(Object base, long offset, Object value) {
        SunMisc.UNSAFE.putObjectVolatile(base, offset, value);
    }

    public static boolean getBoolean(Object base, long offset) {
        return SunMisc.UNSAFE.getBoolean(base, offset);
    }

    public static void putBoolean(Object base, long offset, boolean value) {
        SunMisc.UNSAFE.putBoolean(base, offset, value);
    }

    public static void putBooleanVolatile(Object base, long offset, boolean value) {
        SunMisc.UNSAFE.putBooleanVolatile(base, offset, value);
    }

    public static byte getByte(Object base, long offset) {
        return SunMisc.UNSAFE.getByte(base, offset);
    }

    public static void putByte(Object base, long offset, byte value) {
        SunMisc.UNSAFE.putByte(base, offset, value);
    }

    public static void putByteVolatile(Object base, long offset, byte value) {
        SunMisc.UNSAFE.putByteVolatile(base, offset, value);
    }

    public static char getChar(Object base, long offset) {
        return SunMisc.UNSAFE.getChar(base, offset);
    }

    public static void putChar(Object base, long offset, char value) {
        SunMisc.UNSAFE.putChar(base, offset, value);
    }

    public static void putCharVolatile(Object base, long offset, char value) {
        SunMisc.UNSAFE.putCharVolatile(base, offset, value);
    }

    public static short getShort(Object base, long offset) {
        return SunMisc.UNSAFE.getShort(base, offset);
    }

    public static void putShort(Object base, long offset, short value) {
        SunMisc.UNSAFE.putShort(base, offset, value);
    }

    public static void putShortVolatile(Object base, long offset, short value) {
        SunMisc.UNSAFE.putShortVolatile(base, offset, value);
    }

    public static int getInt(Object base, long offset) {
        return SunMisc.UNSAFE.getInt(base, offset);
    }

    public static void putInt(Object base, long offset, int value) {
        SunMisc.UNSAFE.putInt(base, offset, value);
    }

    public static void putIntVolatile(Object base, long offset, int value) {
        SunMisc.UNSAFE.putIntVolatile(base, offset, value);
    }

    public static long getLong(Object base, long offset) {
        return SunMisc.UNSAFE.getLong(base, offset);
    }

    public static void putLong(Object base, long offset, long value) {
        SunMisc.UNSAFE.putLong(base, offset, value);
    }

    public static void putLongVolatile(Object base, long offset, long value) {
        SunMisc.UNSAFE.putLongVolatile(base, offset, value);
    }

    public static float getFloat(Object base, long offset) {
        return SunMisc.UNSAFE.getFloat(base, offset);
    }

    public static void putFloat(Object base, long offset, float value) {
        SunMisc.UNSAFE.putFloat(base, offset, value);
    }

    public static void putFloatVolatile(Object base, long offset, float value) {
        SunMisc.UNSAFE.putFloatVolatile(base, offset, value);
    }

    public static double getDouble(Object base, long offset) {
        return SunMisc.UNSAFE.getDouble(base, offset);
    }

    public static void putDouble(Object base, long offset, double value) {
        SunMisc.UNSAFE.putDouble(base, offset, value);
    }

    public static void putDoubleVolatile(Object base, long offset, double value) {
        SunMisc.UNSAFE.putDoubleVolatile(base, offset, value);
    }

    /**
     * Reference bits stored for {@code value} in an {@code Object[]} slot.
     * With compressed references this is the narrow value, not a native address.
     */
    public static long referenceBitsOf(Object value) {
        int scale = References.SCALE;
        if (scale != 4 && scale != 8) {
            throw new IllegalStateException("sun.misc.Unsafe is unavailable");
        }
        Object[] box = new Object[] {value};
        if (scale == 8) {
            return SunMisc.UNSAFE.getLong(box, References.BASE);
        }
        return Integer.toUnsignedLong(SunMisc.UNSAFE.getInt(box, References.BASE));
    }

    private static final class References {
        private static final long BASE;
        private static final int SCALE;

        static {
            long base = -1L;
            int scale = 0;
            if (SunMisc.UNSAFE != null) {
                try {
                    base = SunMisc.UNSAFE.arrayBaseOffset(Object[].class);
                    scale = SunMisc.UNSAFE.arrayIndexScale(Object[].class);
                } catch (Throwable ignored) {
                    base = -1L;
                    scale = 0;
                }
            }
            BASE = base;
            SCALE = scale;
        }
    }

    /**
     * Loaded on the first call into {@link UnsafeReflectionUtil0}.
     */
    private static final class SunMisc {

        private static final Unsafe UNSAFE;
        private static final Throwable UNAVAILABILITY_CAUSE;
        private static final long BUFFER_ADDRESS_OFFSET;

        private static final class Probe {
            @SuppressWarnings("unused")
            private int value;
        }

        static {
            Unsafe resolved = null;
            Throwable cause = null;
            long bufferAddressOffset = -1L;
            try {
                Object maybeUnsafe = readTheUnsafe();
                if (maybeUnsafe instanceof Throwable throwable) {
                    cause = throwable;
                } else {
                    Unsafe unsafe = (Unsafe) maybeUnsafe;
                    Throwable missing = missingMethods(unsafe);
                    if (missing != null) {
                        cause = missing;
                    } else {
                        try {
                            Field probe = Probe.class.getDeclaredField("value");
                            unsafe.objectFieldOffset(probe);
                            bufferAddressOffset = unsafe.objectFieldOffset(Buffer.class.getDeclaredField("address"));
                            resolved = unsafe;
                        } catch (UnsupportedOperationException | SecurityException | ReflectiveOperationException e) {
                            cause = e;
                        }
                    }
                }
            } catch (Throwable e) {
                cause = e;
            }
            UNSAFE = resolved;
            UNAVAILABILITY_CAUSE = cause;
            BUFFER_ADDRESS_OFFSET = bufferAddressOffset;
        }

        private SunMisc() {
        }

        private static Object readTheUnsafe() {
            try {
                Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                if (!unsafeField.trySetAccessible()) {
                    return new IllegalAccessException("sun.misc.Unsafe.theUnsafe is not accessible");
                }
                return unsafeField.get(null);
            } catch (NoSuchFieldException | IllegalAccessException | SecurityException | NoClassDefFoundError e) {
                return e;
            }
        }

        private static Throwable missingMethods(Unsafe unsafe) {
            try {
                Class<?> type = unsafe.getClass();
                type.getDeclaredMethod("objectFieldOffset", Field.class);
                type.getDeclaredMethod("staticFieldOffset", Field.class);
                type.getDeclaredMethod("staticFieldBase", Field.class);
                type.getDeclaredMethod("getObject", Object.class, long.class);
                type.getDeclaredMethod("putObject", Object.class, long.class, Object.class);
                type.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
                requirePrimitive(type, "Boolean", boolean.class);
                requirePrimitive(type, "Byte", byte.class);
                requirePrimitive(type, "Char", char.class);
                requirePrimitive(type, "Short", short.class);
                requirePrimitive(type, "Int", int.class);
                requirePrimitive(type, "Long", long.class);
                requirePrimitive(type, "Float", float.class);
                requirePrimitive(type, "Double", double.class);
                return null;
            } catch (NoSuchMethodException e) {
                return e;
            }
        }

        private static void requirePrimitive(Class<?> type, String name, Class<?> valueType) throws NoSuchMethodException {
            type.getDeclaredMethod("get" + name, Object.class, long.class);
            type.getDeclaredMethod("put" + name, Object.class, long.class, valueType);
            type.getDeclaredMethod("put" + name + "Volatile", Object.class, long.class, valueType);
        }
    }
}
