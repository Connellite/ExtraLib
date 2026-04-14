package io.github.connellite.collections;

import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * {@link LinkedHashMap} that ignores {@code put} / {@code putAll} / {@code putIfAbsent} / {@code merge} when the <em>key</em> is {@code null} (no-op; {@code null} values are still stored).
 */
public class NullSkippingLinkedHashMap<K, V> extends LinkedHashMap<K, V> {

    @Serial
    private static final long serialVersionUID = 1L;

    public NullSkippingLinkedHashMap() {
    }

    public NullSkippingLinkedHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public NullSkippingLinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public NullSkippingLinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
    }

    public NullSkippingLinkedHashMap(Map<? extends K, ? extends V> m) {
        super();
        putAll(m);
    }

    /**
     * Associates value with a non-null key.
     *
     * @param key map key
     * @param value map value
     * @return previous value, or current {@code null}-key value when key is {@code null}
     */
    @Override
    public V put(K key, V value) {
        if (key == null) {
            return get(key);
        }
        return super.put(key, value);
    }

    /**
     * Copies all entries with non-null keys from the input map.
     *
     * @param m source map
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            K k = e.getKey();
            if (k != null) {
                super.put(k, e.getValue());
            }
        }
    }

    /**
     * Stores value for a non-null key only when absent.
     *
     * @param key map key
     * @param value map value
     * @return existing value or {@code null}
     */
    @Override
    public V putIfAbsent(K key, V value) {
        if (key == null) {
            return get(key);
        }
        return super.putIfAbsent(key, value);
    }

    /**
     * Merges value for a non-null key.
     *
     * @param key map key
     * @param value value to merge
     * @param remappingFunction merge callback
     * @return merged value, or current {@code null}-key value when key is {@code null}
     */
    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        if (key == null) {
            return get(key);
        }
        return super.merge(key, value, remappingFunction);
    }
}
