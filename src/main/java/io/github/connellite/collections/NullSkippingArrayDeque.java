package io.github.connellite.collections;

import java.io.Serial;
import java.util.ArrayDeque;
import java.util.Collection;

/**
 * {@link ArrayDeque} that ignores {@code null} on insert operations (standard deque rejects {@code null}).
 */
public class NullSkippingArrayDeque<E> extends ArrayDeque<E> {

    @Serial
    private static final long serialVersionUID = 1L;

    public NullSkippingArrayDeque() {
    }

    public NullSkippingArrayDeque(int numElements) {
        super(numElements);
    }

    public NullSkippingArrayDeque(Collection<? extends E> c) {
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
    public void addFirst(E e) {
        if (e == null) {
            return;
        }
        super.addFirst(e);
    }

    @Override
    public void addLast(E e) {
        if (e == null) {
            return;
        }
        super.addLast(e);
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
    public boolean offerFirst(E e) {
        if (e == null) {
            return false;
        }
        return super.offerFirst(e);
    }

    @Override
    public boolean offerLast(E e) {
        if (e == null) {
            return false;
        }
        return super.offerLast(e);
    }

    @Override
    public void push(E e) {
        if (e == null) {
            return;
        }
        super.push(e);
    }
}
