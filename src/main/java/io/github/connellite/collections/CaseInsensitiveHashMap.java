package io.github.connellite.collections;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@link HashMap} with {@link String} keys compared case-insensitively. The key set and entries
 * expose the <em>currently stored</em> spelling (last successful {@code put} wins for that logical key).
 *
 * <p>Like {@link HashMap}, allows a single {@code null} key; it is not folded by case and is omitted
 * from the internal case-folding index. For speed and stable behavior, the default locale used to
 * fold case is {@link Locale#ROOT} (override via {@link #CaseInsensitiveHashMap(Locale)}).
 *
 * @param <V> value type
 */
public class CaseInsensitiveHashMap<V> extends HashMap<String, V> implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;

    private HashMap<String, String> caseInsensitiveKeys;
    @Getter
    private final Locale locale;

    private transient volatile Set<String> keySetView;
    private transient volatile Collection<V> valuesView;
    private transient volatile Set<Map.Entry<String, V>> entrySetView;

    public CaseInsensitiveHashMap() {
        this(16, Locale.ROOT);
    }

    public CaseInsensitiveHashMap(int initialCapacity) {
        this(initialCapacity, Locale.ROOT);
    }

    public CaseInsensitiveHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        this.caseInsensitiveKeys = new HashMap<>(initialCapacity, loadFactor);
        this.locale = Locale.ROOT;
    }

    /**
     * @param locale locale for {@link String#toLowerCase(Locale)}; if {@code null}, {@link Locale#ROOT} is used
     */
    public CaseInsensitiveHashMap(Locale locale) {
        this(16, locale);
    }

    public CaseInsensitiveHashMap(int initialCapacity, Locale locale) {
        super(initialCapacity);
        this.caseInsensitiveKeys = new HashMap<>(initialCapacity);
        this.locale = locale != null ? locale : Locale.ROOT;
    }

    public CaseInsensitiveHashMap(int initialCapacity, float loadFactor, Locale locale) {
        super(initialCapacity, loadFactor);
        this.caseInsensitiveKeys = new HashMap<>(initialCapacity, loadFactor);
        this.locale = locale != null ? locale : Locale.ROOT;
    }

    public CaseInsensitiveHashMap(Map<? extends String, ? extends V> m) {
        this(Math.max(16, m.size()), Locale.ROOT);
        putAll(m);
    }

    public CaseInsensitiveHashMap(Map<? extends String, ? extends V> m, Locale locale) {
        this(Math.max(16, m.size()), locale != null ? locale : Locale.ROOT);
        putAll(m);
    }

    /**
     * Maps a user key to the form stored in {@link #caseInsensitiveKeys} (default: lower case via this map's locale).
     */
    protected String convertKey(String key) {
        return key.toLowerCase(locale);
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            return super.containsKey(null);
        }
        if (key instanceof String s) {
            return caseInsensitiveKeys.containsKey(convertKey(s));
        }
        return false;
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            return super.get(null);
        }
        if (key instanceof String s) {
            String canonical = caseInsensitiveKeys.get(convertKey(s));
            if (canonical != null) {
                return super.get(canonical);
            }
        }
        return null;
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V value = get(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public V put(String key, V value) {
        if (key == null) {
            return super.put(null, value);
        }
        String oldCanonical = caseInsensitiveKeys.put(convertKey(key), key);
        V displaced = null;
        if (oldCanonical != null && !oldCanonical.equals(key)) {
            displaced = super.remove(oldCanonical);
        }
        V previous = super.put(key, value);
        return displaced != null ? displaced : previous;
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> m) {
        if (m.isEmpty()) {
            return;
        }
        m.forEach(this::put);
    }

    @Override
    public V putIfAbsent(String key, V value) {
        if (key == null) {
            return super.putIfAbsent(null, value);
        }
        String norm = convertKey(key);
        String canonical = caseInsensitiveKeys.get(norm);
        if (canonical != null) {
            return super.putIfAbsent(canonical, value);
        }
        V prev = super.putIfAbsent(key, value);
        if (prev == null && super.containsKey(key)) {
            caseInsensitiveKeys.put(norm, key);
        }
        return prev;
    }

    @Override
    public V computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        if (key == null) {
            return super.computeIfAbsent(null, mappingFunction);
        }
        String norm = convertKey(key);
        String existing = caseInsensitiveKeys.get(norm);
        if (existing != null) {
            V current = super.get(existing);
            if (current != null) {
                return current;
            }
            return super.computeIfAbsent(existing, mappingFunction);
        }
        return super.computeIfAbsent(key, k -> {
            V v = mappingFunction.apply(k);
            if (v != null) {
                caseInsensitiveKeys.put(norm, k);
            }
            return v;
        });
    }

    @Override
    public V computeIfPresent(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        if (key == null) {
            return super.computeIfPresent(null, remappingFunction);
        }
        String canonical = caseInsensitiveKeys.get(convertKey(key));
        if (canonical == null) {
            return null;
        }
        return super.computeIfPresent(canonical, (k, v) -> {
            V nv = remappingFunction.apply(k, v);
            if (nv == null) {
                caseInsensitiveKeys.remove(convertKey(k));
            }
            return nv;
        });
    }

    @Override
    public V compute(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        if (key == null) {
            return super.compute(null, remappingFunction);
        }
        String norm = convertKey(key);
        String canonical = caseInsensitiveKeys.get(norm);
        String opKey = canonical != null ? canonical : key;
        return super.compute(opKey, (k, v) -> {
            V nv = remappingFunction.apply(k, v);
            if (nv == null) {
                if (v != null) {
                    caseInsensitiveKeys.remove(convertKey(k));
                }
            } else if (v == null) {
                caseInsensitiveKeys.put(convertKey(k), k);
            }
            return nv;
        });
    }

    @Override
    public V merge(String key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(remappingFunction);
        if (key == null) {
            return super.merge(null, value, remappingFunction);
        }
        String norm = convertKey(key);
        String canonical = caseInsensitiveKeys.get(norm);
        String mergeKey = canonical != null ? canonical : key;
        boolean absent = canonical == null;
        V result = super.merge(mergeKey, value, (oldVal, newVal) -> {
            V nv = remappingFunction.apply(oldVal, newVal);
            if (nv == null) {
                caseInsensitiveKeys.remove(convertKey(mergeKey));
            }
            return nv;
        });
        if (absent && result != null) {
            caseInsensitiveKeys.put(norm, mergeKey);
        }
        return result;
    }

    @Override
    public V remove(Object key) {
        if (key == null) {
            return super.remove(null);
        }
        if (key instanceof String s) {
            String canonical = caseInsensitiveKeys.remove(convertKey(s));
            if (canonical != null) {
                return super.remove(canonical);
            }
        }
        return null;
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (key == null) {
            return super.remove(null, value);
        }
        if (!(key instanceof String s)) {
            return false;
        }
        String canonical = caseInsensitiveKeys.get(convertKey(s));
        if (canonical == null) {
            return false;
        }
        if (!super.remove(canonical, value)) {
            return false;
        }
        caseInsensitiveKeys.remove(convertKey(canonical));
        return true;
    }

    @Override
    public V replace(String key, V value) {
        if (key == null) {
            return super.replace(null, value);
        }
        String canonical = caseInsensitiveKeys.get(convertKey(key));
        if (canonical == null) {
            return null;
        }
        return super.replace(canonical, value);
    }

    @Override
    public boolean replace(String key, V oldValue, V newValue) {
        if (key == null) {
            return super.replace(null, oldValue, newValue);
        }
        String canonical = caseInsensitiveKeys.get(convertKey(key));
        if (canonical == null) {
            return false;
        }
        return super.replace(canonical, oldValue, newValue);
    }

    @Override
    public void clear() {
        caseInsensitiveKeys.clear();
        super.clear();
    }

    @Override
    public Set<String> keySet() {
        Set<String> ks = keySetView;
        if (ks == null) {
            ks = new KeySet(super.keySet());
            keySetView = ks;
        }
        return ks;
    }

    @Override
    public Collection<V> values() {
        Collection<V> vs = valuesView;
        if (vs == null) {
            vs = new Values(super.values());
            valuesView = vs;
        }
        return vs;
    }

    @Override
    public Set<Map.Entry<String, V>> entrySet() {
        Set<Map.Entry<String, V>> es = entrySetView;
        if (es == null) {
            es = new EntrySet(super.entrySet());
            entrySetView = es;
        }
        return es;
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super V> action) {
        super.forEach(action);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CaseInsensitiveHashMap<V> clone() {
        CaseInsensitiveHashMap<V> copy = (CaseInsensitiveHashMap<V>) super.clone();
        copy.caseInsensitiveKeys = (HashMap<String, String>) this.caseInsensitiveKeys.clone();
        copy.keySetView = null;
        copy.valuesView = null;
        copy.entrySetView = null;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    private void removeCaseInsensitiveIndexIfNonNull(String key) {
        if (key != null) {
            caseInsensitiveKeys.remove(convertKey(key));
        }
    }

    private final class KeySet extends AbstractSet<String> {

        private final Set<String> delegate;

        KeySet(Set<String> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public Iterator<String> iterator() {
            return new KeySetIterator();
        }

        @Override
        public boolean remove(Object o) {
            if (!CaseInsensitiveHashMap.this.containsKey(o)) {
                return false;
            }
            CaseInsensitiveHashMap.this.remove(o);
            return true;
        }

        @Override
        public void clear() {
            CaseInsensitiveHashMap.this.clear();
        }

        @Override
        public Spliterator<String> spliterator() {
            return delegate.spliterator();
        }

        @Override
        public void forEach(Consumer<? super String> action) {
            delegate.forEach(action);
        }
    }

    private final class Values extends AbstractCollection<V> {

        private final Collection<V> delegate;

        Values(Collection<V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public Iterator<V> iterator() {
            return new ValuesIterator();
        }

        @Override
        public void clear() {
            CaseInsensitiveHashMap.this.clear();
        }

        @Override
        public Spliterator<V> spliterator() {
            return delegate.spliterator();
        }

        @Override
        public void forEach(Consumer<? super V> action) {
            delegate.forEach(action);
        }
    }

    private final class EntrySet extends AbstractSet<Map.Entry<String, V>> {

        private final Set<Map.Entry<String, V>> delegate;

        EntrySet(Set<Map.Entry<String, V>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) {
                return false;
            }
            Object k = e.getKey();
            if (!CaseInsensitiveHashMap.this.containsKey(k)) {
                return false;
            }
            V v = CaseInsensitiveHashMap.this.get(k);
            return Objects.equals(v, e.getValue());
        }

        @Override
        public Iterator<Map.Entry<String, V>> iterator() {
            return new EntrySetIterator();
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) {
                return false;
            }
            Object k = e.getKey();
            if (!CaseInsensitiveHashMap.this.containsKey(k)) {
                return false;
            }
            V v = CaseInsensitiveHashMap.this.get(k);
            if (!Objects.equals(v, e.getValue())) {
                return false;
            }
            CaseInsensitiveHashMap.this.remove(k);
            return true;
        }

        @Override
        public void clear() {
            CaseInsensitiveHashMap.this.clear();
        }

        @Override
        public Spliterator<Map.Entry<String, V>> spliterator() {
            return delegate.spliterator();
        }

        @Override
        public void forEach(Consumer<? super Map.Entry<String, V>> action) {
            delegate.forEach(action);
        }
    }

    private abstract class EntryIterator<T> implements Iterator<T> {

        private final Iterator<Map.Entry<String, V>> delegate;
        private Map.Entry<String, V> last;

        EntryIterator() {
            this.delegate = CaseInsensitiveHashMap.super.entrySet().iterator();
        }

        protected Map.Entry<String, V> nextEntry() {
            Map.Entry<String, V> e = delegate.next();
            last = e;
            return e;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public void remove() {
            delegate.remove();
            if (last != null) {
                removeCaseInsensitiveIndexIfNonNull(last.getKey());
                last = null;
            }
        }
    }

    private final class KeySetIterator extends EntryIterator<String> {

        @Override
        public String next() {
            return nextEntry().getKey();
        }
    }

    private final class ValuesIterator extends EntryIterator<V> {

        @Override
        public V next() {
            return nextEntry().getValue();
        }
    }

    private final class EntrySetIterator extends EntryIterator<Map.Entry<String, V>> {

        @Override
        public Map.Entry<String, V> next() {
            return nextEntry();
        }
    }
}
