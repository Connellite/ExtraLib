package io.github.connellite.collections;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple implementation of {@link MultiValueMap} that wraps a {@link LinkedHashMap},
 * storing multiple values in an {@link ArrayList}.
 *
 * <p>This Map implementation is generally not thread-safe. It is primarily designed
 * for data structures exposed from request objects, for use in a single thread only.
 *
 * @param <K> the key type
 * @param <V> the value element type
 */
public class LinkedMultiValueHashMap<K, V> extends LinkedHashMap<K, List<V>>
        implements MultiValueMap<K, V>, Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Create a new {@code LinkedMultiValueHashMap} that wraps a {@link LinkedHashMap}.
     */
    public LinkedMultiValueHashMap() {
    }

    /**
     * Create a new {@code LinkedMultiValueHashMap} that wraps a {@link LinkedHashMap}
     * with the given initial capacity.
     *
     * @param expectedSize the expected number of entries
     */
    public LinkedMultiValueHashMap(int expectedSize) {
        super(expectedSize);
    }

    /**
     * Copy constructor: Create a new {@code LinkedMultiValueHashMap} with the same mappings as
     * the specified Map.
     *
     * @param otherMap the Map whose mappings are to be placed in this Map
     */
    public LinkedMultiValueHashMap(Map<K, List<V>> otherMap) {
        super(otherMap);
    }

    /**
     * Copy constructor: Create a new {@code LinkedMultiValueHashMap} with the same mappings as
     * the specified {@code MultiValueMap}.
     *
     * @param otherMap the Map whose mappings are to be placed in this Map
     */
    public LinkedMultiValueHashMap(MultiValueMap<K, V> otherMap) {
        super(otherMap);
    }

    @Override
    public V getFirst(K key) {
        List<V> values = get(key);
        return (values != null && !values.isEmpty() ? values.get(0) : null);
    }

    @Override
    public void add(K key, V value) {
        computeIfAbsent(key, k -> new ArrayList<>(1)).add(value);
    }

    @Override
    public void addAll(K key, List<? extends V> values) {
        computeIfAbsent(key, k -> new ArrayList<>(values.size())).addAll(values);
    }

    @Override
    public void addAll(MultiValueMap<K, V> values) {
        values.forEach(this::addAll);
    }

    @Override
    public void set(K key, V value) {
        List<V> values = new ArrayList<>(1);
        values.add(value);
        put(key, values);
    }

    @Override
    public void setAll(Map<K, V> values) {
        values.forEach(this::set);
    }

    @Override
    public Map<K, V> toSingleValueMap() {
        Map<K, V> singleValueMap = new LinkedHashMap<>(size());
        forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                singleValueMap.put(key, values.get(0));
            }
        });
        return singleValueMap;
    }

    @SuppressWarnings("Java9CollectionFactory")
    @Override
    public Map<K, List<V>> toUnmodifiableMap() {
        Map<K, List<V>> immutableMap = new LinkedHashMap<>(size());
        forEach((key, values) -> immutableMap.put(key, Collections.unmodifiableList(new ArrayList<>(values))));
        return Collections.unmodifiableMap(immutableMap);
    }

    /**
     * Create a deep copy of this Map.
     *
     * @return a copy of this Map, including a copy of each value-holding List entry
     */
    public LinkedMultiValueHashMap<K, V> deepCopy() {
        LinkedMultiValueHashMap<K, V> copy = new LinkedMultiValueHashMap<>(size());
        forEach((key, values) -> copy.put(key, new ArrayList<>(values)));
        return copy;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public LinkedMultiValueHashMap<K, V> clone() {
        return new LinkedMultiValueHashMap<>(this);
    }
}
