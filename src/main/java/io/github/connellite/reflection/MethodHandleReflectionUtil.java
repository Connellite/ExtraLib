package io.github.connellite.reflection;

import lombok.experimental.UtilityClass;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;

/**
 * Field access, method invocation, and construction through cached {@link MethodHandle}
 * and {@link VarHandle} instances. Resolve a handle once ({@link #methodHandle(Method)},
 * {@link #varHandle(Field)}, {@link #constructorHandle(Class, Class...)},
 * {@link #resolveRecordConstructorHandle(Class)}, etc.), then pass it as the first argument
 * to {@link #invoke(MethodHandle, Object...)}, {@link #get(VarHandle, Object)},
 * {@link #set(VarHandle, Object, Object)}, or {@link #getInstance(MethodHandle, Object...)}.
 * <p>
 * For {@link MethodHandle} invocation:
 * <ul>
 *     <li>{@link #invoke(MethodHandle, Object...)} — coercion path via {@link MethodHandle#invoke},
 *         similar to classic reflection.</li>
 *     <li>{@link #asType(MethodHandle, MethodType)} then {@link MethodHandle#invokeExact} at the call site —
 *         strict signatures and primitive returns (with a cast). A utility wrapper cannot preserve
 *         {@code invokeExact} types through {@link Object} varargs.</li>
 * </ul>
 * <p>
 * Classic reflection helpers ({@link ReflectionUtil#getMethodByName}, {@link ReflectionUtil#lookupClass},
 * enum maps, generic introspection, etc.) remain on {@link ReflectionUtil}.
 */
@UtilityClass
public class MethodHandleReflectionUtil {

    /**
     * Unreflects a declared method into a {@link MethodHandle}.
     */
    public static MethodHandle methodHandle(Method method) throws IllegalAccessException {
        return lookupFor(method.getDeclaringClass()).unreflect(method);
    }

    /**
     * Resolves a declared no-arg method by name and unreflects it.
     */
    public static MethodHandle methodHandle(Class<?> clazz, String methodName)
            throws NoSuchMethodException, IllegalAccessException {
        return methodHandle(clazz.getDeclaredMethod(methodName));
    }

    /**
     * Unreflects a declared field into a {@link VarHandle}.
     */
    public static VarHandle varHandle(Field field) throws IllegalAccessException {
        return lookupFor(field.getDeclaringClass()).unreflectVarHandle(field);
    }

    /**
     * Resolves a declared field by name and unreflects it.
     */
    public static VarHandle varHandle(Class<?> clazz, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        return varHandle(clazz.getDeclaredField(fieldName));
    }

    /**
     * No-argument declared constructor as a {@link MethodHandle}.
     */
    public static <T> MethodHandle constructorHandle(Class<T> type) throws NoSuchMethodException, IllegalAccessException {
        return constructorHandle(type.getDeclaredConstructor());
    }

    /**
     * Declared constructor with the given parameter types as a {@link MethodHandle}.
     */
    public static <T> MethodHandle constructorHandle(Class<T> type, Class<?>... parameterTypes)
            throws NoSuchMethodException, IllegalAccessException {
        return constructorHandle(type.getDeclaredConstructor(parameterTypes));
    }

    /**
     * Unreflects a declared constructor into a {@link MethodHandle}.
     */
    public static MethodHandle constructorHandle(Constructor<?> constructor) throws IllegalAccessException {
        return lookupFor(constructor.getDeclaringClass()).unreflectConstructor(constructor);
    }

    /**
     * Canonical record constructor as a {@link MethodHandle}.
     */
    public static <T> MethodHandle resolveRecordConstructorHandle(Class<T> recordClass)
            throws NoSuchMethodException, IllegalAccessException {
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException("Class " + recordClass.getName() + " is not a record.");
        }
        RecordComponent[] components = recordClass.getRecordComponents();
        Class<?>[] types = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) {
            types[i] = components[i].getType();
        }
        return constructorHandle(recordClass, types);
    }

    /**
     * Adapts {@code handle} to {@code type} via {@link MethodHandle#asType(MethodType)}.
     */
    public static MethodHandle asType(MethodHandle handle, MethodType type) {
        return handle.asType(type);
    }

    /**
     * Invokes with {@link MethodHandle#invoke(Object...)} — arguments may be converted
     * (boxing, widening, reference casts). Convenience path; not signature-strict.
     * <p>
     * For {@link MethodHandle#invokeExact}, adapt with {@link #asType(MethodHandle, MethodType)}
     * and invoke on the handle at the use site.
     */
    public static Object invoke(MethodHandle handle, Object... args)
            throws InvocationTargetException, IllegalAccessException {
        return invokeWithCoercion(handle, args);
    }

    /**
     * Reads a static field through a {@link VarHandle}.
     */
    public static Object get(VarHandle handle) {
        return handle.get();
    }

    /**
     * Reads an instance field through a {@link VarHandle}.
     */
    public static Object get(VarHandle handle, Object receiver) {
        return handle.get(receiver);
    }

    /**
     * Reads a field and casts it to {@code type}.
     *
     * @param receiver instance for an instance field, or {@code null} for a static field
     */
    public static <T> T get(VarHandle handle, Object receiver, Class<T> type) {
        Object value = receiver == null ? handle.get() : handle.get(receiver);
        return ReflectionUtil.castFieldValue(type, value);
    }

    /**
     * Writes a static field through a {@link VarHandle}.
     */
    public static void set(VarHandle handle, Object value) {
        handle.set(value);
    }

    /**
     * Writes an instance field through a {@link VarHandle}.
     */
    public static void set(VarHandle handle, Object receiver, Object value) {
        handle.set(receiver, value);
    }

    /**
     * Writes a field value and rejects {@code final} fields using {@code field} metadata.
     *
     * @param receiver instance for an instance field, or {@code null} for a static field
     * @throws IllegalStateException if {@code field} is {@code final}
     */
    public static void set(VarHandle handle, Field field, Object receiver, Object value) {
        if (Modifier.isFinal(field.getModifiers())) {
            throw new IllegalStateException("Field " + field.getName() + " is final.");
        }
        if (receiver == null) {
            handle.set(value);
            return;
        }
        handle.set(receiver, value);
    }

    /**
     * Invokes a no-arg constructor {@link MethodHandle}.
     */
    public static <T> T getInstance(MethodHandle constructorHandle)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return invokeConstructorWithCoercion(constructorHandle);
    }

    /**
     * Invokes a constructor {@link MethodHandle} with {@link MethodHandle#invoke(Object...)}.
     */
    public static <T> T getInstance(MethodHandle constructorHandle, Object... initargs)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return invokeConstructorWithCoercion(constructorHandle, initargs);
    }

    private static MethodHandles.Lookup lookupFor(Class<?> declaringClass) throws IllegalAccessException {
        return MethodHandles.privateLookupIn(declaringClass, MethodHandles.lookup());
    }

    private static Object invokeWithCoercion(MethodHandle handle, Object[] args)
            throws InvocationTargetException, IllegalAccessException {
        try {
            return switch (args.length) {
                case 0 -> handle.invoke();
                case 1 -> handle.invoke(args[0]);
                case 2 -> handle.invoke(args[0], args[1]);
                case 3 -> handle.invoke(args[0], args[1], args[2]);
                case 4 -> handle.invoke(args[0], args[1], args[2], args[3]);
                case 5 -> handle.invoke(args[0], args[1], args[2], args[3], args[4]);
                case 6 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5]);
                case 7 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
                case 8 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
                default -> handle.invokeWithArguments(args);
            };
        } catch (Throwable throwable) {
            throwInvocation(throwable);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeConstructorWithCoercion(MethodHandle constructorHandle, Object... initargs)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        try {
            return (T) invokeWithCoercion(constructorHandle, initargs);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw e;
        } catch (Throwable throwable) {
            throwConstruction(throwable);
            return null;
        }
    }

    private static void throwInvocation(Throwable throwable)
            throws IllegalAccessException, InvocationTargetException {
        if (throwable instanceof InvocationTargetException invocationTargetException) {
            throw invocationTargetException;
        }
        if (throwable instanceof IllegalAccessException illegalAccessException) {
            throw illegalAccessException;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new InvocationTargetException(throwable);
    }

    private static void throwConstruction(Throwable throwable)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        if (throwable instanceof InvocationTargetException invocationTargetException) {
            throw invocationTargetException;
        }
        if (throwable instanceof InstantiationException instantiationException) {
            throw instantiationException;
        }
        if (throwable instanceof IllegalAccessException illegalAccessException) {
            throw illegalAccessException;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new InvocationTargetException(throwable);
    }
}
