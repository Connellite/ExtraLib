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

    @Override
    public boolean add(E e) {
        if (e == null) {
            return false;
        }
        return super.add(e);
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            return false;
        }
        return super.offer(e);
    }

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
