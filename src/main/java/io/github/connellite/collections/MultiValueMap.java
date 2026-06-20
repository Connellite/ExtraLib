package io.github.connellite.collections;

import java.util.List;
import java.util.Map;

/**
 * Extension of the {@code Map} interface that stores multiple values.
 *
 * @param <K> the key type
 * @param <V> the value element type
 */
public interface MultiValueMap<K, V> extends Map<K, List<V>> {

    /**
     * Return the first value for the given key.
     *
     * @param key the key
     * @return the first value for the specified key, or {@code null}
     */
    V getFirst(K key);

    /**
     * Add the given single value to the current list of values for the given key.
     *
     * @param key the key
     * @param value the value to be added
     */
    void add(K key, V value);

    /**
     * Add all the values of the given list to the current list of values for the given key.
     *
     * @param key they key
     * @param values the values to be added
     */
    void addAll(K key, List<? extends V> values);

    /**
     * Add all the values of the given {@code MultiValueMap} to the current values.
     *
     * @param values the values to be added
     */
    void addAll(MultiValueMap<K, V> values);

    /**
     * Set the given single value under the given key.
     *
     * @param key the key
     * @param value the value to set
     */
    void set(K key, V value);

    /**
     * Set the given values under their associated key.
     *
     * @param values the values
     */
    void setAll(Map<K, V> values);

    /**
     * Return a {@code Map} with the first values contained in this {@code MultiValueMap}.
     *
     * @return a single value representation of this map
     */
    Map<K, V> toSingleValueMap();

    /**
     * Return an unmodifiable view of this map with immutable value lists.
     * The returned map and its list values cannot be modified; changes to this
     * map are not reflected in the returned instance.
     *
     * @return unmodifiable map with copied value lists
     */
    Map<K, List<V>> toUnmodifiableMap();

    /**
     * Add the given value, only when the map does not contain the given key.
     *
     * @param key the key
     * @param value the value to be added
     */
    default void addIfAbsent(K key, V value) {
        if (!containsKey(key)) {
            add(key, value);
        }
    }
}
