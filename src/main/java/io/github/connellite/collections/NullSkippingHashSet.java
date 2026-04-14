package io.github.connellite.collections;

import java.io.Serial;
import java.util.Collection;
import java.util.HashSet;

/**
 * {@link HashSet} that ignores {@code null} on {@code add} / {@code addAll}.
 */
public class NullSkippingHashSet<E> extends HashSet<E> {

    @Serial
    private static final long serialVersionUID = 1L;

    public NullSkippingHashSet() {
    }

    public NullSkippingHashSet(int initialCapacity) {
        super(initialCapacity);
    }

    public NullSkippingHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public NullSkippingHashSet(Collection<? extends E> c) {
        super();
        addAll(c);
    }

    /**
     * Adds a non-null element to the set.
     *
     * @param e element to add
     * @return {@code true} if element was added; {@code false} for {@code null}
     */
    @Override
    public boolean add(E e) {
        if (e == null) {
            return false;
        }
        return super.add(e);
    }

    /**
     * Adds all non-null elements from the source collection.
     *
     * @param c source collection
     * @return {@code true} if at least one element was added
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean changed = false;
        for (E e : c) {
            if (e != null && super.add(e)) {
                changed = true;
            }
        }
        return changed;
    }
}
