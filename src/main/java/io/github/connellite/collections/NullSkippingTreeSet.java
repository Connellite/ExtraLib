package io.github.connellite.collections;

import java.io.Serial;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * {@link TreeSet} that ignores {@code null} on {@code add} / {@code addAll} (safe copy from arbitrary collections).
 */
public class NullSkippingTreeSet<E> extends TreeSet<E> {

    @Serial
    private static final long serialVersionUID = 1L;

    public NullSkippingTreeSet() {
    }

    public NullSkippingTreeSet(Comparator<? super E> comparator) {
        super(comparator);
    }

    public NullSkippingTreeSet(Collection<? extends E> c) {
        super();
        addAll(c);
    }

    public NullSkippingTreeSet(SortedSet<E> s) {
        super();
        addAll(s);
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
