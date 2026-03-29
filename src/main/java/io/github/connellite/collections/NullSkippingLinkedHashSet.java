package io.github.connellite.collections;

import java.io.Serial;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * {@link LinkedHashSet} that ignores {@code null} on {@code add} / {@code addAll}.
 */
public class NullSkippingLinkedHashSet<E> extends LinkedHashSet<E> {

    @Serial
    private static final long serialVersionUID = 1L;

    public NullSkippingLinkedHashSet() {
    }

    public NullSkippingLinkedHashSet(int initialCapacity) {
        super(initialCapacity);
    }

    public NullSkippingLinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public NullSkippingLinkedHashSet(Collection<? extends E> c) {
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
