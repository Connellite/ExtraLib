package io.github.connellite.jdbc.internal;

import io.github.connellite.jdbc.TypeConverter;
import io.github.connellite.jdbc.annotation.Bind;
import io.github.connellite.reflection.ReflectionUtil;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

@UtilityClass
public final class BeanBindSource {

    private static final Map<Class<?>, List<MemberBinding>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    public static List<BindValue> read(Object bean, Set<String> parameterNames) throws SQLException {
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(parameterNames, "parameterNames");
        List<MemberBinding> members = metadata(bean.getClass());
        if (members.isEmpty()) {
            throw new IllegalArgumentException("No @Bind members on " + bean.getClass().getName());
        }
        List<BindValue> matched = new ArrayList<>();
        for (MemberBinding member : members) {
            if (!parameterNames.contains(member.name())) {
                continue;
            }
            matched.add(new BindValue(member.name(), convert(readRaw(bean, member), member)));
        }
        if (matched.isEmpty()) {
            throw new IllegalArgumentException("No @Bind values match named parameters on " + bean.getClass().getName());
        }
        return matched;
    }

    private static List<MemberBinding> metadata(Class<?> type) throws SQLException {
        List<MemberBinding> existing = CACHE.get(type);
        if (existing != null) {
            return existing;
        }
        synchronized (CACHE) {
            existing = CACHE.get(type);
            if (existing != null) {
                return existing;
            }
            List<MemberBinding> created = buildMetadata(type);
            CACHE.put(type, created);
            return created;
        }
    }

    private static List<MemberBinding> buildMetadata(Class<?> type) throws SQLException {
        List<MemberBinding> members = type.isRecord() ? recordMembers(type) : classMembers(type);
        return List.copyOf(members);
    }

    private static List<MemberBinding> classMembers(Class<?> type) throws SQLException {
        List<MemberBinding> members = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Field field : ReflectionUtil.getAllDeclaredFields(type)) {
            Bind bind = field.getAnnotation(Bind.class);
            if (bind == null) {
                continue;
            }
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException("@Bind is not allowed on static or synthetic field "
                        + field.getDeclaringClass().getName() + "#" + field.getName());
            }
            String name = parameterName(bind, field.getName());
            if (!names.add(name)) {
                throw new IllegalArgumentException("Duplicate @Bind parameter '" + name + "' on " + type.getName());
            }
            ReflectionUtil.open(field);
            members.add(new MemberBinding(name, field, null, converter(bind)));
        }
        return members;
    }

    private static List<MemberBinding> recordMembers(Class<?> type) throws SQLException {
        List<MemberBinding> members = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (RecordComponent component : type.getRecordComponents()) {
            Bind bind = component.getAnnotation(Bind.class);
            if (bind == null) {
                continue;
            }
            String name = parameterName(bind, component.getName());
            if (!names.add(name)) {
                throw new IllegalArgumentException("Duplicate @Bind parameter '" + name + "' on " + type.getName());
            }
            Method accessor = component.getAccessor();
            ReflectionUtil.open(accessor);
            members.add(new MemberBinding(name, null, accessor, converter(bind)));
        }
        return members;
    }

    private static String parameterName(Bind bind, String memberName) {
        return bind.value().isBlank() ? memberName : bind.value();
    }

    private static TypeConverter<?> converter(Bind bind) throws SQLException {
        if (bind.converter() == TypeConverter.DefaultConverter.class) {
            return null;
        }
        try {
            return ReflectionUtil.getInstance(bind.converter());
        } catch (ReflectiveOperationException e) {
            throw new SQLException("Cannot instantiate converter " + bind.converter().getName(), e);
        }
    }

    private static Object readRaw(Object bean, MemberBinding member) {
        try {
            if (member.accessor() != null) {
                return member.accessor().invoke(bean);
            }
            return ReflectionUtil.getValueField(bean, member.field());
        } catch (IllegalAccessException e) {
            throw cannotRead(member, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw cannotRead(member, cause);
        }
    }

    private static Object convert(Object raw, MemberBinding member) throws SQLException {
        TypeConverter<?> converter = member.converter();
        if (converter == null) {
            return raw;
        }
        return converter.convert(raw);
    }

    private static IllegalStateException cannotRead(MemberBinding member, Throwable cause) {
        String owner = member.accessor() != null
                ? member.accessor().getDeclaringClass().getName() + "#" + member.accessor().getName()
                : member.field().getDeclaringClass().getName() + "#" + member.field().getName();
        return new IllegalStateException("Cannot read " + owner, cause);
    }

    public record BindValue(String name, Object value) {
    }

    private record MemberBinding(String name, Field field, Method accessor, TypeConverter<?> converter) {
    }
}
