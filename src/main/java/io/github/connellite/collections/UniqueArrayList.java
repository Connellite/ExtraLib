package io.github.connellite.collections;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;

/**
 * {@link ArrayList} that allows each distinct element (by {@link Object#equals(Object) equals}) at most once.
 * {@code add} / {@code addAll} ignore duplicates; order follows successful insertions.
 *
 * <p>{@link #contains(Object)} and {@link #containsAll(Collection)} use the internal set (same as {@link HashSet}
 * semantics, including at most one {@code null}).
 *
 * <p>{@link #subList(int, int)} returns an unmodifiable view of the corresponding range.
 */
public class UniqueArrayList<E> extends ArrayList<E> {

    @Serial
    private static final long serialVersionUID = 1L;

    private HashSet<E> uniques;

    public UniqueArrayList() {
        this.uniques = new HashSet<>();
    }

    public UniqueArrayList(int initialCapacity) {
        super(initialCapacity);
        this.uniques = new HashSet<>();
    }

    public UniqueArrayList(Collection<? extends E> c) {
        this(Math.max(c.size(), 16));
        addAll(c);
    }

    @Override
    public boolean add(E object) {
        int sizeBefore = size();
        add(size(), object);
        return sizeBefore != size();
    }

    @Override
    public void add(int index, E object) {
        if (uniques.add(object)) {
            super.add(index, object);
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> coll) {
        return addAll(size(), coll);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> coll) {
        ArrayList<E> temp = new ArrayList<>();
        for (E e : coll) {
            if (uniques.add(e)) {
                temp.add(e);
            }
        }
        return super.addAll(index, temp);
    }

    @Override
    public void clear() {
        super.clear();
        uniques.clear();
    }

    @Override
    public boolean contains(Object object) {
        return uniques.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> coll) {
        return uniques.containsAll(coll);
    }

    @Override
    public Iterator<E> iterator() {
        return new UniqueIterator(super.iterator());
    }

    @Override
    public ListIterator<E> listIterator() {
        return new UniqueListIterator(super.listIterator());
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new UniqueListIterator(super.listIterator(index));
    }

    @Override
    public E remove(int index) {
        E result = super.remove(index);
        uniques.remove(result);
        return result;
    }

    @Override
    public boolean remove(Object object) {
        boolean result = uniques.remove(object);
        if (result) {
            super.remove(object);
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> coll) {
        boolean changed = false;
        for (Object name : coll) {
            changed |= remove(name);
        }
        return changed;
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        boolean result = super.removeIf(filter);
        uniques.removeIf(filter);
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> coll) {
        boolean result = uniques.retainAll(coll);
        if (!result) {
            return false;
        }
        if (uniques.isEmpty()) {
            super.clear();
        } else {
            super.retainAll(uniques);
        }
        return true;
    }

    @Override
    public E set(int index, E object) {
        int pos = indexOf(object);
        E removed = super.set(index, object);
        if (pos != -1 && pos != index) {
            super.remove(pos);
        }
        uniques.remove(removed);
        uniques.add(object);
        return removed;
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return Collections.unmodifiableList(super.subList(fromIndex, toIndex));
    }

    @Override
    public Object clone() {
        @SuppressWarnings("unchecked")
        UniqueArrayList<E> copy = (UniqueArrayList<E>) super.clone();
        copy.uniques = new HashSet<>(this.uniques);
        return copy;
    }

    private final class UniqueIterator implements Iterator<E> {
        private final Iterator<E> delegate;
        private E last;

        UniqueIterator(Iterator<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public E next() {
            last = delegate.next();
            return last;
        }

        @Override
        public void remove() {
            delegate.remove();
            uniques.remove(last);
            last = null;
        }
    }

    private final class UniqueListIterator implements ListIterator<E> {
        private final ListIterator<E> delegate;
        private E last;

        UniqueListIterator(ListIterator<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public E next() {
            last = delegate.next();
            return last;
        }

        @Override
        public boolean hasPrevious() {
            return delegate.hasPrevious();
        }

        @Override
        public E previous() {
            last = delegate.previous();
            return last;
        }

        @Override
        public int nextIndex() {
            return delegate.nextIndex();
        }

        @Override
        public int previousIndex() {
            return delegate.previousIndex();
        }

        @Override
        public void remove() {
            delegate.remove();
            uniques.remove(last);
            last = null;
        }

        @Override
        public void set(E object) {
            throw new UnsupportedOperationException("ListIterator does not support set");
        }

        @Override
        public void add(E object) {
            if (uniques.add(object)) {
                delegate.add(object);
            }
        }
    }
}
