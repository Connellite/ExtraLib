package io.github.connellite.collections;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
 * {@link LinkedHashMap} with {@link String} keys compared case-insensitively.
 * Preserves insertion order of canonical keys.
 *
 * @param <V> value type
 */
public class LinkedCaseInsensitiveMap<V> extends LinkedHashMap<String, V> implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;

    private HashMap<String, String> caseInsensitiveKeys;
    @Getter
    private final Locale locale;

    private transient volatile Set<String> keySetView;
    private transient volatile Collection<V> valuesView;
    private transient volatile Set<Map.Entry<String, V>> entrySetView;

    public LinkedCaseInsensitiveMap() {
        this(16, Locale.ROOT, false);
    }

    public LinkedCaseInsensitiveMap(int initialCapacity) {
        this(initialCapacity, Locale.ROOT, false);
    }

    public LinkedCaseInsensitiveMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, Locale.ROOT, false);
    }

    public LinkedCaseInsensitiveMap(Locale locale) {
        this(16, locale, false);
    }

    public LinkedCaseInsensitiveMap(int initialCapacity, Locale locale) {
        this(initialCapacity, 0.75f, locale, false);
    }

    public LinkedCaseInsensitiveMap(int initialCapacity, Locale locale, boolean accessOrder) {
        this(initialCapacity, 0.75f, locale, accessOrder);
    }

    public LinkedCaseInsensitiveMap(int initialCapacity, float loadFactor, Locale locale) {
        this(initialCapacity, loadFactor, locale, false);
    }

    public LinkedCaseInsensitiveMap(int initialCapacity, float loadFactor, Locale locale, boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
        this.caseInsensitiveKeys = new HashMap<>(initialCapacity, loadFactor);
        this.locale = locale != null ? locale : Locale.ROOT;
    }

    public LinkedCaseInsensitiveMap(Map<? extends String, ? extends V> m) {
        this(Math.max(16, m.size()), Locale.ROOT);
        putAll(m);
    }

    public LinkedCaseInsensitiveMap(Map<? extends String, ? extends V> m, Locale locale) {
        this(Math.max(16, m.size()), locale != null ? locale : Locale.ROOT);
        putAll(m);
    }

    protected String convertKey(String key) {
        return key.toLowerCase(locale);
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            return super.containsKey(null);
        }
        if (key instanceof String s) {
            String canonical = caseInsensitiveKeys.get(convertKey(s));
            return canonical != null && super.containsKey(canonical);
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
        String canonical = caseInsensitiveKeys.putIfAbsent(convertKey(key), key);
        if (canonical != null) {
            V existingValue = super.get(canonical);
            if (existingValue != null) {
                return existingValue;
            }
            key = canonical;
        }
        return super.putIfAbsent(key, value);
    }

    @Override
    public V computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        if (key == null) {
            return super.computeIfAbsent(null, mappingFunction);
        }
        String canonical = caseInsensitiveKeys.putIfAbsent(convertKey(key), key);
        if (canonical != null) {
            V existingValue = super.get(canonical);
            if (existingValue != null) {
                return existingValue;
            }
            key = canonical;
        }
        return super.computeIfAbsent(key, mappingFunction);
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
        String canonical = caseInsensitiveKeys.putIfAbsent(convertKey(key), key);
        if (canonical != null) {
            key = canonical;
        }
        return super.compute(key, (k, v) -> {
            V nv = remappingFunction.apply(k, v);
            if (nv == null) {
                if (v != null || super.containsKey(k)) {
                    caseInsensitiveKeys.remove(convertKey(k));
                }
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
        String canonical = caseInsensitiveKeys.putIfAbsent(convertKey(key), key);
        if (canonical != null) {
            key = canonical;
        }
        final String mergeKey = key;
        return super.merge(mergeKey, value, (oldVal, newVal) -> {
            V nv = remappingFunction.apply(oldVal, newVal);
            if (nv == null) {
                caseInsensitiveKeys.remove(convertKey(mergeKey));
            }
            return nv;
        });
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
    public LinkedCaseInsensitiveMap<V> clone() {
        LinkedCaseInsensitiveMap<V> copy = (LinkedCaseInsensitiveMap<V>) super.clone();
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
            if (!LinkedCaseInsensitiveMap.this.containsKey(o)) {
                return false;
            }
            LinkedCaseInsensitiveMap.this.remove(o);
            return true;
        }

        @Override
        public void clear() {
            LinkedCaseInsensitiveMap.this.clear();
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
            LinkedCaseInsensitiveMap.this.clear();
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
            if (!LinkedCaseInsensitiveMap.this.containsKey(k)) {
                return false;
            }
            V v = LinkedCaseInsensitiveMap.this.get(k);
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
            if (!LinkedCaseInsensitiveMap.this.containsKey(k)) {
                return false;
            }
            V v = LinkedCaseInsensitiveMap.this.get(k);
            if (!Objects.equals(v, e.getValue())) {
                return false;
            }
            LinkedCaseInsensitiveMap.this.remove(k);
            return true;
        }

        @Override
        public void clear() {
            LinkedCaseInsensitiveMap.this.clear();
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
            this.delegate = LinkedCaseInsensitiveMap.super.entrySet().iterator();
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
