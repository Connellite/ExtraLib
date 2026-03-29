package io.github.connellite.collections;

import java.io.Serial;
import java.util.Collection;
import java.util.LinkedList;

/**
 * {@link LinkedList} that ignores {@code null} on add / offer / push / deque head-tail inserts.
 */
public class NullSkippingLinkedList<E> extends LinkedList<E> {

    @Serial
    private static final long serialVersionUID = 1L;

    public NullSkippingLinkedList() {
    }

    public NullSkippingLinkedList(Collection<? extends E> c) {
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
    public boolean offer(E e) {
        if (e == null) {
            return false;
        }
        return super.offer(e);
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
