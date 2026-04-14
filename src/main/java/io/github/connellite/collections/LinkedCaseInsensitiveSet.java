package io.github.connellite.collections;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * {@link Set} of {@link String} elements compared case-insensitively for {@code contains} / {@code add} /
 * {@code remove}, backed by {@link LinkedCaseInsensitiveMap}. Iteration order follows insertion order
 * of canonical keys.
 *
 * <p>Allows {@code null} like {@link java.util.LinkedHashSet}. Default case folding uses {@link Locale#ROOT}.
 */
public class LinkedCaseInsensitiveSet extends AbstractSet<String> implements Set<String>, Cloneable, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Object PRESENT = new Object();

    private LinkedCaseInsensitiveMap<Object> map;

    public LinkedCaseInsensitiveSet() {
        this.map = new LinkedCaseInsensitiveMap<>();
    }

    public LinkedCaseInsensitiveSet(int initialCapacity) {
        this.map = new LinkedCaseInsensitiveMap<>(initialCapacity);
    }

    public LinkedCaseInsensitiveSet(int initialCapacity, float loadFactor) {
        this.map = new LinkedCaseInsensitiveMap<>(initialCapacity, loadFactor, Locale.ROOT);
    }

    public LinkedCaseInsensitiveSet(Locale locale) {
        this.map = new LinkedCaseInsensitiveMap<>(locale);
    }

    public LinkedCaseInsensitiveSet(int initialCapacity, Locale locale) {
        this.map = new LinkedCaseInsensitiveMap<>(initialCapacity, locale);
    }

    public LinkedCaseInsensitiveSet(int initialCapacity, float loadFactor, Locale locale) {
        this.map = new LinkedCaseInsensitiveMap<>(initialCapacity, loadFactor, locale);
    }

    public LinkedCaseInsensitiveSet(Collection<? extends String> c) {
        this.map = new LinkedCaseInsensitiveMap<>(Math.max(16, c.size()));
        addAll(c);
    }

    public LinkedCaseInsensitiveSet(Collection<? extends String> c, Locale locale) {
        this.map = new LinkedCaseInsensitiveMap<>(Math.max(16, c.size()), locale);
        addAll(c);
    }

    public Locale getLocale() {
        return map.getLocale();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean add(String e) {
        return map.put(e, PRESENT) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Iterator<String> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public LinkedCaseInsensitiveSet clone() {
        try {
            LinkedCaseInsensitiveSet copy = (LinkedCaseInsensitiveSet) super.clone();
            copy.map = map.clone();
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
