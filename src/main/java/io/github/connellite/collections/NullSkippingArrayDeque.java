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

    /**
     * Appends a non-null element to the deque tail.
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
     * Inserts a non-null element at deque head.
     *
     * @param e element to insert
     */
    @Override
    public void addFirst(E e) {
        if (e == null) {
            return;
        }
        super.addFirst(e);
    }

    /**
     * Inserts a non-null element at deque tail.
     *
     * @param e element to insert
     */
    @Override
    public void addLast(E e) {
        if (e == null) {
            return;
        }
        super.addLast(e);
    }

    /**
     * Adds all non-null elements from the input collection.
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

    /**
     * Offers a non-null element at deque head.
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
     * Offers a non-null element at deque tail.
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
     * Pushes a non-null element onto the stack view of this deque.
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
