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

    /**
     * Appends a non-null element to the tail.
     *
     * @param e element to append
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
     * Inserts a non-null element at the specified index.
     *
     * @param index insertion index
     * @param element element to insert
     */
    @Override
    public void add(int index, E element) {
        if (element == null) {
            return;
        }
        super.add(index, element);
    }

    /**
     * Appends all non-null elements from the source collection.
     *
     * @param c source collection
     * @return {@code true} if at least one element was appended
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

    /**
     * Inserts all non-null elements from the source collection starting at index.
     *
     * @param index insertion start index
     * @param c source collection
     * @return {@code true} if at least one element was inserted
     */
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

    /**
     * Adds a non-null element to deque head.
     *
     * @param e element to add
     */
    @Override
    public void addFirst(E e) {
        if (e == null) {
            return;
        }
        super.addFirst(e);
    }

    /**
     * Adds a non-null element to deque tail.
     *
     * @param e element to add
     */
    @Override
    public void addLast(E e) {
        if (e == null) {
            return;
        }
        super.addLast(e);
    }

    /**
     * Offers a non-null element to queue tail.
     *
     * @param e element to offer
     * @return {@code true} if accepted; {@code false} for {@code null}
     */
    @Override
    public boolean offer(E e) {
        if (e == null) {
            return false;
        }
        return super.offer(e);
    }

    /**
     * Offers a non-null element to deque head.
     *
     * @param e element to offer
     * @return {@code true} if accepted; {@code false} for {@code null}
     */
    @Override
    public boolean offerFirst(E e) {
        if (e == null) {
            return false;
        }
        return super.offerFirst(e);
    }

    /**
     * Offers a non-null element to deque tail.
     *
     * @param e element to offer
     * @return {@code true} if accepted; {@code false} for {@code null}
     */
    @Override
    public boolean offerLast(E e) {
        if (e == null) {
            return false;
        }
        return super.offerLast(e);
    }

    /**
     * Pushes a non-null element onto the stack view.
     *
     * @param e element to push
     */
    @Override
    public void push(E e) {
        if (e == null) {
            return;
        }
        super.push(e);
    }
}
