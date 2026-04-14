package io.github.connellite.collections;

import java.io.Serial;
import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * {@link PriorityQueue} that ignores {@code null} on {@code add} / {@code offer} / {@code addAll} (standard queue rejects {@code null}).
 */
public class NullSkippingPriorityQueue<E> extends PriorityQueue<E> {

    @Serial
    private static final long serialVersionUID = 1L;

    public NullSkippingPriorityQueue() {
    }

    public NullSkippingPriorityQueue(int initialCapacity) {
        super(initialCapacity);
    }

    public NullSkippingPriorityQueue(Comparator<? super E> comparator) {
        super(comparator);
    }

    public NullSkippingPriorityQueue(int initialCapacity, Comparator<? super E> comparator) {
        super(initialCapacity, comparator);
    }

    public NullSkippingPriorityQueue(Collection<? extends E> c) {
        super();
        addAll(c);
    }

    /**
     * Adds a non-null element to the queue.
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
     * Offers a non-null element to the queue.
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
     * Adds all non-null elements from the source collection.
     *
     * @param c source collection
     * @return {@code true} if at least one element was added
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean changed = false;
        for (E e : c) {
            if (e != null && super.offer(e)) {
                changed = true;
            }
        }
        return changed;
    }
}
