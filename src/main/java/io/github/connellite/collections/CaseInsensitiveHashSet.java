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
 * {@code remove}. Iteration and the single stored spelling per logical element follow {@link CaseInsensitiveHashMap}
 * (last successful {@code add} wins for that logical string).
 *
 * <p>Allows {@code null} like {@link java.util.HashSet}. Default case folding uses {@link Locale#ROOT}; pass a
 * {@link Locale} constructor to customize (see {@link CaseInsensitiveHashMap}).
 */
public class CaseInsensitiveHashSet extends AbstractSet<String> implements Set<String>, Cloneable, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Object PRESENT = new Object();

    private CaseInsensitiveHashMap<Object> map;

    public CaseInsensitiveHashSet() {
        this.map = new CaseInsensitiveHashMap<>();
    }

    public CaseInsensitiveHashSet(int initialCapacity) {
        this.map = new CaseInsensitiveHashMap<>(initialCapacity);
    }

    public CaseInsensitiveHashSet(int initialCapacity, float loadFactor) {
        this.map = new CaseInsensitiveHashMap<>(initialCapacity, loadFactor);
    }

    /**
     * @param locale locale for case folding; if {@code null}, {@link Locale#ROOT} is used
     */
    public CaseInsensitiveHashSet(Locale locale) {
        this.map = new CaseInsensitiveHashMap<>(locale);
    }

    public CaseInsensitiveHashSet(int initialCapacity, Locale locale) {
        this.map = new CaseInsensitiveHashMap<>(initialCapacity, locale);
    }

    public CaseInsensitiveHashSet(int initialCapacity, float loadFactor, Locale locale) {
        this.map = new CaseInsensitiveHashMap<>(initialCapacity, loadFactor, locale);
    }

    public CaseInsensitiveHashSet(Collection<? extends String> c) {
        this.map = new CaseInsensitiveHashMap<>(Math.max(16, c.size()));
        addAll(c);
    }

    public CaseInsensitiveHashSet(Collection<? extends String> c, Locale locale) {
        this.map = new CaseInsensitiveHashMap<>(Math.max(16, c.size()), locale);
        addAll(c);
    }

    /**
     * Locale used for case-insensitive comparison (same as the backing {@link CaseInsensitiveHashMap}).
     */
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
    public CaseInsensitiveHashSet clone() {
        try {
            CaseInsensitiveHashSet copy = (CaseInsensitiveHashSet) super.clone();
            copy.map = map.clone();
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
