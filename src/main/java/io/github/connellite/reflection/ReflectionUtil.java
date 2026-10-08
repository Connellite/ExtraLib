package io.github.connellite.reflection;

import lombok.experimental.UtilityClass;

import java.lang.invoke.MethodHandles;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Small helpers around {@link Field}, {@link Method}, and {@link Constructor}: accessibility,
 * lookup by name or annotation, and {@code getInstance} shortcuts. Prefer
 * {@link #getInstance(Class, Class[], Object...)} when the constructor takes {@code null} arguments
 * or parameter types must be wider than the runtime types of {@code initargs}.
 */
@UtilityClass
public class ReflectionUtil {

    /**
     * Class of the code that called this method — shorthand for
     * {@link MethodHandles#lookup()}{@code .lookupClass()} at the call site.
     */
    public static Class<?> lookupClass() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
    }

    /**
     * Invokes a static method with no parameters.
     *
     * @param clazz  class declaring the method
     * @param method method name
     * @return the invocation result (possibly {@code null})
     */
    public static Object invokeStatic(Class<?> clazz, String method)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = clazz.getDeclaredMethod(method);
        if (!m.canAccess(null)) {
            m.trySetAccessible();
        }
        return m.invoke(null);
    }

    /**
     * Invokes an instance method with no parameters on {@code o}'s runtime class.
     */
    public static Object invoke(Object o, String method)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = o.getClass().getDeclaredMethod(method);
        if (!m.canAccess(o)) {
            m.trySetAccessible();
        }
        return m.invoke(o);
    }

    /**
     * Invokes {@code method} on {@code target} after {@link Method#trySetAccessible()}.
     */
    public static Object invoke(Method method, Object target, Object... args)
            throws InvocationTargetException, IllegalAccessException {
        if (!method.canAccess(target)) {
            method.trySetAccessible();
        }
        return method.invoke(target, args == null ? new Object[0] : args);
    }

    /**
     * Invokes an interface {@code default} method on {@code proxy}.
     */
    public static Object invokeDefault(Object proxy, Method method, Object... args) throws Throwable {
        return MethodHandles.privateLookupIn(method.getDeclaringClass(), MethodHandles.lookup())
                .unreflectSpecial(method, method.getDeclaringClass())
                .bindTo(proxy)
                .invokeWithArguments(args == null ? new Object[0] : args);
    }

    /**
     * Reads a static field and casts it to {@code type}.
     */
    public static <T> T getStatic(Class<?> clazz, String f, Class<T> type)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(f);
        if (!field.canAccess(null)) {
            field.trySetAccessible();
        }
        return castFieldValue(type, field.get(null));
    }

    /**
     * Writes a static field.
     */
    public static void setStatic(Class<?> clazz, String f, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(f);
        if (!field.canAccess(null)) {
            field.trySetAccessible();
        }
        field.set(null, value);
    }

    /**
     * Reads a field declared on the direct superclass of {@code o}'s class.
     */
    public static <T> T getSuper(Object o, String f, Class<T> type)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = o.getClass().getSuperclass().getDeclaredField(f);
        if (!field.canAccess(o)) {
            field.trySetAccessible();
        }
        return castFieldValue(type, field.get(o));
    }

    /**
     * Reads an instance field declared on {@code clazz} (not a subclass), for example a field on a
     * superclass when {@code clazz} is that superclass.
     */
    public static <T> T get(Object instance, Class<?> clazz, String f, Class<T> type)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(f);
        if (!field.canAccess(instance)) {
            field.trySetAccessible();
        }
        return castFieldValue(type, field.get(instance));
    }

    /**
     * Reads a field declared on {@code o}'s runtime class.
     */
    public static <T> T get(Object o, String f, Class<T> type)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = o.getClass().getDeclaredField(f);
        if (!field.canAccess(o)) {
            field.trySetAccessible();
        }
        return castFieldValue(type, field.get(o));
    }

    /**
     * Reads a public field (including inherited) via {@link Class#getField(String)}.
     */
    public static <T> T getPublic(Object o, String f, Class<T> type)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = o.getClass().getField(f);
        if (!field.canAccess(o)) {
            field.trySetAccessible();
        }
        return castFieldValue(type, field.get(o));
    }

    /**
     * Writes a field declared on {@code o}'s runtime class.
     */
    public static void set(Object o, String f, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = o.getClass().getDeclaredField(f);
        if (!field.canAccess(o)) {
            field.trySetAccessible();
        }
        field.set(o, value);
    }

    /**
     * Reads a field value, calling {@link Field#trySetAccessible()} when {@link Field#canAccess(Object)}
     * is false (private, protected, package-private from another package, etc.).
     *
     * @param obj   instance for an instance field, or {@code null} for a static field
     * @param field field to read; not null
     */
    public static Object getValueField(Object obj, Field field) throws IllegalAccessException {
        if (!field.canAccess(obj)) {
            field.trySetAccessible();
        }
        return field.get(obj);
    }

    /**
     * Writes a field value, calling {@link Field#trySetAccessible()} when {@link Field#canAccess(Object)}
     * is false.
     *
     * @throws IllegalStateException if the field is {@code final}
     */
    public static void setValueField(Object obj, Field field, Object value) throws IllegalAccessException {
        int modifier = field.getModifiers();
        if (Modifier.isFinal(modifier)) {
            throw new IllegalStateException("Field " + field.getName() + " is final.");
        }
        if (!field.canAccess(obj)) {
            field.trySetAccessible();
        }
        field.set(obj, value);
    }

    /**
     * Declared fields from {@code type} and its superclasses, superclass first, excluding {@link Object}.
     * Does not change accessibility or filter static/synthetic fields.
     */
    public static List<Field> getAllDeclaredFields(Class<?> type) {
        List<Class<?>> hierarchy = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            hierarchy.add(current);
        }
        Collections.reverse(hierarchy);
        List<Field> fields = new ArrayList<>();
        for (Class<?> current : hierarchy) {
            Collections.addAll(fields, current.getDeclaredFields());
        }
        return Collections.unmodifiableList(fields);
    }

    /**
     * Field named {@code name} on {@code type} or a superclass, made accessible, or {@code null}.
     */
    public static Field findDeclaredField(Class<?> type, String name) {
        for (Field field : getAllDeclaredFields(type)) {
            if (field.getName().equals(name)) {
                field.trySetAccessible();
                return field;
            }
        }
        return null;
    }

    /**
     * Field named {@code name} with exact {@code fieldType} on {@code type} or a superclass, made accessible,
     * or {@code null}.
     */
    public static Field findDeclaredField(Class<?> type, String name, Class<?> fieldType) {
        for (Field field : getAllDeclaredFields(type)) {
            if (field.getName().equals(name) && field.getType() == fieldType) {
                field.trySetAccessible();
                return field;
            }
        }
        return null;
    }

    /**
     * Declared methods from {@code type} and its superclasses, superclass first, excluding {@link Object}.
     * Does not change accessibility or filter bridges/synthetics.
     */
    public static List<Method> getAllDeclaredMethods(Class<?> type) {
        List<Class<?>> hierarchy = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            hierarchy.add(current);
        }
        Collections.reverse(hierarchy);
        List<Method> methods = new ArrayList<>();
        for (Class<?> current : hierarchy) {
            Collections.addAll(methods, current.getDeclaredMethods());
        }
        return Collections.unmodifiableList(methods);
    }

    /**
     * Method named {@code name} with the given parameter types on {@code type}, a superclass, or an
     * implemented interface (most specific class first). Made accessible, or {@code null}.
     */
    public static Method findMethod(Class<?> type, String name, Class<?>... paramTypes) {
        Class<?>[] params = paramTypes == null ? new Class<?>[0] : paramTypes;
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            Method method = matchDeclaredMethod(current, name, params);
            if (method != null) {
                method.trySetAccessible();
                return method;
            }
        }
        for (Class<?> iface : getAllInterfaces(type)) {
            Method method = matchDeclaredMethod(iface, name, params);
            if (method != null) {
                method.trySetAccessible();
                return method;
            }
        }
        return null;
    }

    /**
     * Method named {@code name} with {@code paramCount} parameters on {@code type}, a superclass, or an
     * implemented interface (most specific class first). Parameter types are not compared, so a generic
     * method is found after erasure. A non-bridge, non-synthetic method is preferred; a bridge or synthetic
     * method is returned only when no other match exists. Made accessible, or {@code null}.
     */
    public static Method findMethod(Class<?> type, String name, int paramCount) {
        if (paramCount < 0) {
            throw new IllegalArgumentException("paramCount must not be negative");
        }
        Method fallback = null;
        List<Class<?>> types = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            types.add(current);
        }
        types.addAll(getAllInterfaces(type));
        for (Class<?> current : types) {
            for (Method method : current.getDeclaredMethods()) {
                if (!name.equals(method.getName()) || method.getParameterCount() != paramCount) {
                    continue;
                }
                if (!method.isBridge() && !method.isSynthetic()) {
                    method.trySetAccessible();
                    return method;
                }
                if (fallback == null) {
                    fallback = method;
                }
            }
        }
        if (fallback != null) {
            fallback.trySetAccessible();
        }
        return fallback;
    }

    /**
     * {@code true} if {@code method} is {@code equals(Object)}.
     */
    public static boolean isEqualsMethod(Method method) {
        return method != null
                && method.getParameterCount() == 1
                && "equals".equals(method.getName())
                && method.getParameterTypes()[0] == Object.class;
    }

    /**
     * {@code true} if {@code method} is {@code hashCode()} with no parameters.
     */
    public static boolean isHashCodeMethod(Method method) {
        return method != null && method.getParameterCount() == 0 && "hashCode".equals(method.getName());
    }

    /**
     * {@code true} if {@code method} is {@code toString()} with no parameters.
     */
    public static boolean isToStringMethod(Method method) {
        return method != null && method.getParameterCount() == 0 && "toString".equals(method.getName());
    }

    /**
     * {@code true} if {@code method} is declared on {@link Object} or is {@code equals}/{@code hashCode}/{@code toString}.
     */
    public static boolean isObjectMethod(Method method) {
        return method != null
                && (method.getDeclaringClass() == Object.class
                || isEqualsMethod(method)
                || isHashCodeMethod(method)
                || isToStringMethod(method));
    }

    /**
     * {@code true} if {@code method} declares {@code exceptionType} or a supertype of it.
     */
    public static boolean declaresException(Method method, Class<?> exceptionType) {
        for (Class<?> declared : method.getExceptionTypes()) {
            if (declared.isAssignableFrom(exceptionType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the first {@linkplain Class#getDeclaredMethods() declared} method whose name equals
     * {@code nameMethod}, or {@code null} if none match. If several overloads share the name, which
     * one is returned is unspecified.
     */
    public static Method getMethodByName(Class<?> entity, String nameMethod) {
        for (Method method : entity.getDeclaredMethods()) {
            if (method.getName().equals(nameMethod)) {
                method.trySetAccessible();
                return method;
            }
        }
        return null;
    }

    /**
     * Returns the first declared method annotated with {@code clazz}, or {@code null}.
     */
    public static Method getMethodByAnnotation(Class<?> entity, Class<? extends Annotation> clazz) {
        for (Method method : entity.getDeclaredMethods()) {
            if (method.isAnnotationPresent(clazz)) {
                method.trySetAccessible();
                return method;
            }
        }
        return null;
    }

    /**
     * Returns all declared methods that carry the given annotation (possibly empty).
     */
    public static List<Method> getAllMethodsByAnnotation(Class<?> entity, Class<? extends Annotation> clazz) {
        List<Method> list = new ArrayList<>();
        for (Method method : entity.getDeclaredMethods()) {
            if (method.isAnnotationPresent(clazz)) {
                method.trySetAccessible();
                list.add(method);
            }
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * {@code annotation} on {@code clazz} itself, or else on its {@link Package}
     * (typically from {@code package-info}). Superclasses and implemented interfaces are not searched.
     *
     * @param clazz      class to inspect
     * @param annotation annotation type; must have {@link java.lang.annotation.RetentionPolicy#RUNTIME}
     * @return the annotation, or {@code null} when it is absent. A class in the unnamed package has no package annotation
     */
    public static <A extends Annotation> A getClassOrPackageAnnotation(Class<?> clazz, Class<A> annotation) {
        A onClass = clazz.getAnnotation(annotation);
        if (onClass != null) {
            return onClass;
        }
        Package packageInfo = clazz.getPackage();
        return packageInfo == null ? null : packageInfo.getAnnotation(annotation);
    }

    /**
     * Whether {@link #getClassOrPackageAnnotation(Class, Class)} finds the annotation.
     */
    public static boolean hasClassOrPackageAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        return getClassOrPackageAnnotation(clazz, annotation) != null;
    }

    /**
     * {@code annotation} on {@code clazz} itself, or else on its {@link Module}
     * (typically from {@code module-info}). Superclasses and implemented interfaces are not searched.
     *
     * @param clazz      class to inspect
     * @param annotation annotation type; must have {@link java.lang.annotation.RetentionPolicy#RUNTIME}
     * @return the annotation, or {@code null} when it is absent
     */
    public static <A extends Annotation> A getClassOrModuleAnnotation(Class<?> clazz, Class<A> annotation) {
        A onClass = clazz.getAnnotation(annotation);
        if (onClass != null) {
            return onClass;
        }
        Module moduleInfo = clazz.getModule();
        return moduleInfo == null ? null : moduleInfo.getAnnotation(annotation);
    }

    /**
     * Whether {@link #getClassOrModuleAnnotation(Class, Class)} finds the annotation.
     */
    public static boolean hasClassOrModuleAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        return getClassOrModuleAnnotation(clazz, annotation) != null;
    }

    /**
     * User class behind a JDK, CGLIB, Byte Buddy, Hibernate, Mockito, or Javassist proxy.
     * A JDK proxy with several application interfaces is returned unchanged.
     *
     * @param type class to unwrap; must not be {@code null}
     * @return the underlying class, or {@code type} when it is not a recognized proxy
     */
    public static Class<?> unwrapProxy(Class<?> type) {
        Objects.requireNonNull(type, "type");
        Class<?> current = type;
        while (true) {
            Class<?> next = unwrapProxyOnce(current);
            if (next == current) {
                return current;
            }
            current = next;
        }
    }

    /**
     * {@code true} when {@code className} looks like a generated subclass proxy.
     */
    static boolean isGeneratedProxyName(String className) {
        return className.contains("$$")
                || className.contains("$ByteBuddy$")
                || className.contains("$HibernateProxy$")
                || className.contains("$MockitoMock$")
                || className.contains("_$$_");
    }

    private static Class<?> unwrapProxyOnce(Class<?> type) {
        if (Proxy.isProxyClass(type)) {
            return singleUserInterface(type);
        }
        if (isGeneratedProxyName(type.getName())) {
            Class<?> superclass = type.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return superclass;
            }
            return singleUserInterface(type);
        }
        return type;
    }

    private static Class<?> singleUserInterface(Class<?> type) {
        Class<?> found = null;
        for (Class<?> iface : type.getInterfaces()) {
            String name = iface.getName();
            if (name.startsWith("java.") || name.startsWith("jdk.")) {
                continue;
            }
            if (found != null) {
                return type;
            }
            found = iface;
        }
        return found == null ? type : found;
    }

    /**
     * No-argument declared constructor, made accessible.
     */
    public static <T> Constructor<T> getConstructor(Class<T> t) throws NoSuchMethodException {
        Constructor<T> constructor = t.getDeclaredConstructor();
        constructor.trySetAccessible();
        return constructor;
    }

    /**
     * Declared constructor with the given parameter types, made accessible.
     */
    public static <T> Constructor<T> getConstructor(Class<T> t, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Constructor<T> constructor = t.getDeclaredConstructor(parameterTypes);
        constructor.trySetAccessible();
        return constructor;
    }

    /**
     * Resolves canonical constructor of a record class and makes it accessible.
     * <p>
     * For record {@code MyRecord(int id, String name)} returns constructor with parameter types
     * {@code (int.class, String.class)}.
     *
     * @param recordClass record class token
     * @param <T> record type
     * @return canonical constructor for the record
     * @throws IllegalArgumentException if {@code recordClass} is not a record
     * @throws IllegalStateException if canonical constructor cannot be resolved
     */
    public static <T> Constructor<T> resolveRecordConstructor(Class<T> recordClass) {
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException("Class " + recordClass.getName() + " is not a record.");
        }
        try {
            RecordComponent[] components = recordClass.getRecordComponents();
            Class<?>[] types = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                types[i] = components[i].getType();
            }
            return getConstructor(recordClass, types);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Cannot resolve canonical constructor for record " + recordClass.getName(), e);
        }
    }

    /**
     * {@code getConstructor(type).newInstance()} — no-arg instance.
     */
    public static <T> T getInstance(Class<T> t)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return getConstructor(t).newInstance();
    }

    /**
     * Resolves a constructor from the runtime classes of {@code initargs} (with primitive wrapper
     * unboxing for matching {@code int}, {@code boolean}, etc.). Fails if any argument is
     * {@code null} or if inferred types do not match an accessible constructor — then use
     * {@link #getInstance(Class, Class[], Object[])}.
     */
    public static <T> T getInstance(Class<T> t, Object... initargs)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (initargs.length == 0) {
            return getInstance(t);
        }
        Class<?>[] types = new Class<?>[initargs.length];
        for (int i = 0; i < initargs.length; i++) {
            types[i] = reflectParameterType(initargs[i], i);
        }
        return getConstructor(t, types).newInstance(initargs);
    }

    /**
     * Invokes {@code t.getDeclaredConstructor(parameterTypes).newInstance(initargs)} with access opened.
     */
    public static <T> T getInstance(Class<T> t, Class<?>[] parameterTypes, Object... initargs)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return getConstructor(t, parameterTypes).newInstance(initargs);
    }

    /**
     * Collects every interface type implemented (directly or indirectly) by {@code cls}: interfaces
     * declared on {@code cls}, super-interfaces of those, and the same closure for each superclass
     * up to {@link Object}. Each interface appears at most once; order is not specified.
     *
     * @param cls the class; must not be {@code null}
     * @return an immutable list of distinct interface {@link Class} objects
     * @throws NullPointerException if {@code cls} is {@code null}
     */
    public static List<Class<?>> getAllInterfaces(Class<?> cls) {
        return Stream.concat(
                Arrays.stream(cls.getInterfaces()).flatMap(intf ->
                        Stream.concat(Stream.of(intf), getAllInterfaces(intf).stream())),
                cls.getSuperclass() == null ? Stream.empty() : getAllInterfaces(cls.getSuperclass()).stream()
        ).distinct().toList();
    }

    /**
     * Returns whether {@code cls} is a nested type with a lexically enclosing class, i.e.
     * {@link Class#getEnclosingClass()} is non-null (member classes, local classes, anonymous classes).
     *
     * @param cls the class to test, or {@code null}
     * @return {@code false} if {@code cls} is {@code null} or top-level; otherwise the result of
     *         {@code cls.getEnclosingClass() != null}
     */
    public static boolean isInnerClass(final Class<?> cls) {
        return cls != null && cls.getEnclosingClass() != null;
    }

    /**
     * Builds a map from each enum constant's {@linkplain Enum#name() name} to that constant.
     * <p>Equivalent to {@link #getEnumMap(Class, Function) getEnumMap(enumClass, Enum::name)}.</p>
     *
     * @param enumClass the enum class; must not be {@code null} and must be an enum type
     * @return a new map (typically a {@link java.util.HashMap}, per {@link Collectors#toMap})
     * @throws NullPointerException if {@code enumClass} or {@link Class#getEnumConstants()} is {@code null}
     */
    public static <E extends Enum<E>> Map<String, E> getEnumMap(final Class<E> enumClass) {
        return getEnumMap(enumClass, Enum::name);
    }

    /**
     * Builds a map from a derived key (per constant) to the enum constant, for example a numeric
     * {@code type} field exposed by a getter.
     * <pre>{@code
     * private static final Map<Integer, ValueType> BY_TYPE =
     *         getEnumMap(ValueType.class, ValueType::getType);
     * }</pre>
     * <p>Duplicate keys produce {@link IllegalStateException} from {@link Collectors#toMap}.</p>
     *
     * @param enumClass    the enum class; must not be {@code null} and must be an enum type
     * @param keyExtractor function producing the map key for each constant; must not be {@code null}
     * @param <E>          enum type
     * @param <K>          key type (e.g. {@link Integer} for an {@code int} code)
     * @return a new map from extracted key to constant (see {@link Collectors#toMap})
     * @throws NullPointerException  if {@code enumClass}, {@code keyExtractor}, or enum constants are {@code null}
     * @throws IllegalStateException if two constants map to the same key
     */
    public static <E extends Enum<E>, K> Map<K, E> getEnumMap(
            final Class<E> enumClass,
            final Function<? super E, ? extends K> keyExtractor) {
        return Arrays.stream(enumClass.getEnumConstants())
                .collect(Collectors.toMap(keyExtractor, Function.identity()));
    }

    /**
     * Casts a reflected field value to {@code type} with primitive-aware behavior.
     * <p>
     * Reflection APIs return boxed values for primitive fields; when {@code type} is a primitive
     * token (for example {@code int.class}), this method first maps it to the wrapper type
     * (for example {@code Integer.class}) before casting.
     *
     * @param type requested target type (primitive or reference)
     * @param value value to cast
     * @return cast value, or {@code null} if {@code value} is {@code null}
     * @throws ClassCastException if {@code value} is not assignable to the effective cast type
     */
    @SuppressWarnings("unchecked")
    public static <T> T castFieldValue(Class<T> type, Object value) {
        if (value == null) {
            return null;
        }
        Class<?> castType = type.isPrimitive() ? primitiveToWrapper(type) : type;
        return (T) castType.cast(value);
    }

    /**
     * Rethrows {@code t} as-is, including checked exceptions, without wrapping.
     * {@code null} becomes {@link NullPointerException}.
     */
    @SuppressWarnings("unchecked")
    public static <X extends Throwable> void propagate(Throwable t) throws X {
        throw (X) t;
    }

    /**
     * Maps a primitive type token to its wrapper type; non-primitive inputs are returned unchanged.
     *
     * @param primitive primitive class token (or any class)
     * @return wrapper class for primitives, {@code Void.class} for {@code void.class}, otherwise {@code primitive}
     */
    public static Class<?> primitiveToWrapper(Class<?> primitive) {
        if (primitive == int.class) {
            return Integer.class;
        }
        if (primitive == long.class) {
            return Long.class;
        }
        if (primitive == boolean.class) {
            return Boolean.class;
        }
        if (primitive == byte.class) {
            return Byte.class;
        }
        if (primitive == char.class) {
            return Character.class;
        }
        if (primitive == short.class) {
            return Short.class;
        }
        if (primitive == float.class) {
            return Float.class;
        }
        if (primitive == double.class) {
            return Double.class;
        }
        if (primitive == void.class) {
            return Void.class;
        }
        return primitive;
    }

    /**
     * Extracts all generic argument classes from a {@link Type}.
     * <p>
     * Examples:
     * <ul>
     *     <li>{@code List<Integer>} -&gt; {@code [Integer.class]}</li>
     *     <li>{@code Map<Integer, String>} -&gt; {@code [Integer.class, String.class]}</li>
     * </ul>
     * Nested generic declarations are traversed recursively.
     *
     * @param genericType generic type token, usually from {@link Field#getGenericType()} or
     *                    {@link Method#getGenericReturnType()}
     * @return list of classes found inside generic arguments; empty if none
     */
    public static List<Class<?>> getAllGenericParameterClasses(Type genericType) {
        List<Class<?>> classes = new ArrayList<>();
        collectGenericParameterClasses(genericType, classes);
        return Collections.unmodifiableList(classes);
    }

    /**
     * Shortcut overload for {@link #getAllGenericParameterClasses(Type)} using a reflected field.
     *
     * @param field reflected field; must not be {@code null}
     * @return list of classes found inside the field generic arguments; empty if none
     */
    public static List<Class<?>> getAllGenericParameterClasses(Field field) {
        return getAllGenericParameterClasses(field.getGenericType());
    }

    /**
     * Generic arguments of the first {@code rawInterface} parameterization found on {@code type}
     * or its superclasses. Empty if none.
     */
    public static List<Class<?>> getGenericInterfaceParameterClasses(Class<?> type, Class<?> rawInterface) {
        return genericInterfaceParameterClasses(type, rawInterface);
    }

    /**
     * Resolves type argument {@code index} of {@code target} as implemented by {@code type},
     * substituting interface type variables along the way. {@code null} if not found.
     */
    public static Type resolveTypeArgument(Class<?> type, Class<?> target, int index) {
        return resolveTypeArgument(type, target, index, Map.of());
    }

    private static List<Class<?>> genericInterfaceParameterClasses(Type type, Class<?> rawInterface) {
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() == rawInterface) {
            return getAllGenericParameterClasses(parameterized);
        }
        if (type instanceof Class<?> clazz) {
            for (Type genericInterface : clazz.getGenericInterfaces()) {
                List<Class<?>> classes = genericInterfaceParameterClasses(genericInterface, rawInterface);
                if (!classes.isEmpty()) {
                    return classes;
                }
            }
            Class<?> superClass = clazz.getSuperclass();
            return superClass == null || superClass == Object.class
                    ? List.of()
                    : genericInterfaceParameterClasses(superClass, rawInterface);
        }
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> rawClass) {
            return genericInterfaceParameterClasses(rawClass, rawInterface);
        }
        return List.of();
    }

    private static Type resolveTypeArgument(Type type, Class<?> target, int index, Map<TypeVariable<?>, Type> variables) {
        if (type instanceof Class<?> clazz) {
            for (Type genericInterface : clazz.getGenericInterfaces()) {
                Type resolved = resolveTypeArgument(genericInterface, target, index, variables);
                if (resolved != null) {
                    return resolved;
                }
            }
            Type genericSuperclass = clazz.getGenericSuperclass();
            if (genericSuperclass != null && genericSuperclass != Object.class) {
                return resolveTypeArgument(genericSuperclass, target, index, variables);
            }
            return null;
        }
        if (!(type instanceof ParameterizedType parameterizedType)) {
            return null;
        }
        if (!(parameterizedType.getRawType() instanceof Class<?> rawType)) {
            return null;
        }
        Map<TypeVariable<?>, Type> nextVariables = new HashMap<>(variables);
        TypeVariable<?>[] parameters = rawType.getTypeParameters();
        Type[] arguments = parameterizedType.getActualTypeArguments();
        for (int i = 0; i < parameters.length && i < arguments.length; i++) {
            nextVariables.put(parameters[i], resolveCapturedType(arguments[i], variables));
        }
        if (rawType == target) {
            if (index < 0 || index >= arguments.length) {
                return null;
            }
            Type argument = resolveCapturedType(arguments[index], nextVariables);
            if (argument instanceof Class<?>) {
                return argument;
            }
            List<Class<?>> classes = getAllGenericParameterClasses(argument);
            return classes.isEmpty() ? argument : classes.get(0);
        }
        return resolveTypeArgument(rawType, target, index, nextVariables);
    }

    private static Type resolveCapturedType(Type type, Map<TypeVariable<?>, Type> variables) {
        while (type instanceof TypeVariable<?> variable && variables.containsKey(variable)) {
            type = variables.get(variable);
        }
        return type;
    }

    private static Method matchDeclaredMethod(Class<?> type, String name, Class<?>[] paramTypes) {
        for (Method method : type.getDeclaredMethods()) {
            if (name.equals(method.getName()) && Arrays.equals(paramTypes, method.getParameterTypes())) {
                return method;
            }
        }
        return null;
    }

    private static void collectGenericParameterClasses(Type type, List<Class<?>> target) {
        if (!(type instanceof ParameterizedType parameterizedType)) {
            return;
        }
        for (Type actualTypeArgument : parameterizedType.getActualTypeArguments()) {
            if (actualTypeArgument instanceof Class<?> clazz) {
                target.add(clazz);
                continue;
            }
            if (actualTypeArgument instanceof ParameterizedType nestedParameterizedType) {
                Type rawType = nestedParameterizedType.getRawType();
                if (rawType instanceof Class<?> rawClass) {
                    target.add(rawClass);
                }
                collectGenericParameterClasses(nestedParameterizedType, target);
            }
        }
    }

    private static Class<?> reflectParameterType(Object arg, int index) {
        if (arg == null) {
            throw new IllegalArgumentException(
                    "Cannot infer parameter type for null at index " + index + "; use getInstance(Class, Class[], Object...).");
        }
        Class<?> c = arg.getClass();
        if (c == Integer.class) {
            return int.class;
        }
        if (c == Long.class) {
            return long.class;
        }
        if (c == Short.class) {
            return short.class;
        }
        if (c == Byte.class) {
            return byte.class;
        }
        if (c == Character.class) {
            return char.class;
        }
        if (c == Boolean.class) {
            return boolean.class;
        }
        if (c == Float.class) {
            return float.class;
        }
        if (c == Double.class) {
            return double.class;
        }
        return c;
    }
}
