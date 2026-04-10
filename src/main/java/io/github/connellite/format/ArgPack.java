package io.github.connellite.format;

import io.github.connellite.exception.FormatException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class ArgPack {

    private final Object[] raw;
    private final Map<String, Object> named;

    ArgPack(Object[] raw, Map<String, Object> named) {
        this.raw = raw;
        this.named = named;
    }

    static ArgPack of(Object[] args) {
        if (args == null || args.length == 0) {
            return new ArgPack(args == null ? new Object[0] : args, Collections.emptyMap());
        }
        Map<String, Object> map = new HashMap<>();
        for (Object a : args) {
            if (a instanceof Named n) {
                map.put(n.name(), n.value());
            }
        }
        return new ArgPack(args, map);
    }

    Object resolve(ArgId id) {
        if (id instanceof AutoArgId) {
            return valueAtPositional(((AutoArgId) id).slot());
        }
        if (id instanceof IndexArgId) {
            return valueAtIndex(((IndexArgId) id).index());
        }
        if (id instanceof NameArgId) {
            String name = ((NameArgId) id).name();
            if (!named.containsKey(name)) {
                throw new FormatException("named argument not found: " + name);
            }
            return named.get(name);
        }
        throw new FormatException("internal error");
    }

    Object valueAtPositional(int posIndex) {
        int seen = 0;
        for (Object a : raw) {
            if (!(a instanceof Named)) {
                if (seen == posIndex) {
                    return unwrap(a);
                }
                seen++;
            }
        }
        throw new FormatException("argument not found: " + posIndex);
    }

    Object valueAtIndex(int arrayIndex) {
        if (arrayIndex < 0 || arrayIndex >= raw.length) {
            throw new FormatException("argument not found: " + arrayIndex);
        }
        return unwrap(raw[arrayIndex]);
    }

    private static Object unwrap(Object a) {
        return a instanceof Named ? ((Named) a).value() : a;
    }
}
