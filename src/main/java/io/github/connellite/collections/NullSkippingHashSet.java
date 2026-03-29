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

    @Override
    public boolean add(E e) {
        if (e == null) {
            return false;
        }
        return super.add(e);
    }

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
