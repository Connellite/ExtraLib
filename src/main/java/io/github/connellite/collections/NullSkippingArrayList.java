package io.github.connellite.collections;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;

/**
 * {@link ArrayList} that ignores {@code null} on add operations (no exception, no-op).
 */
public class NullSkippingArrayList<E> extends ArrayList<E> {

    @Serial
    private static final long serialVersionUID = 1L;

    public NullSkippingArrayList() {
    }

    public NullSkippingArrayList(int initialCapacity) {
        super(initialCapacity);
    }

    public NullSkippingArrayList(Collection<? extends E> c) {
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
    public void add(int index, E element) {
        if (element == null) {
            return;
        }
        super.add(index, element);
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

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        if (c.isEmpty()) {
            return false;
        }
        int i = index;
        boolean changed = false;
        for (E e : c) {
            if (e != null) {
                super.add(i++, e);
                changed = true;
            }
        }
        return changed;
    }
}
