import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ForwardingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;
import com.google.common.collect.PeekingIterator;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.annotation.Nullable;

public final class Select {

  private static final class BoundedPriorityQueue<E> extends ForwardingQueue<E> {
    private final PriorityQueue<E> queue;
    private final Comparator<? super E> comparator;
    private final int k;

    public BoundedPriorityQueue(Comparator<? super E> comparator, int k) {
      checkArgument(k > 0);
      this.comparator = checkNotNull(comparator);
      this.queue = new PriorityQueue<E>(k, comparator);
      this.k = k;
    }

    public boolean add(@Nullable E element) {
      return offer(element);
    }

    /**
     * Kicks out the smallest element if it would make the queue bigger than k.
     */
    @Override public boolean offer(@Nullable E element) {
      if (queue.size() < k) {
        return super.offer(element);
      } else if (comparator.compare(element, peek()) > 0) {
        remove();
        return super.offer(element);
      } else {
        return false;
      }
    }

    @Override protected Queue<E> delegate() {
      return queue;
    }
  }

  public static <E> List<E> greatestKHeap(Comparator<? super E> comparator,
      Iterator<E> iterator, int k) {
    checkNotNull(comparator);
    checkArgument(k >= 0);
    if (k == 0) {
      return ImmutableList.of();
    }
    Queue<E> heap = new BoundedPriorityQueue<E>(comparator, k);
    int nSkipped = 0;
    while (iterator.hasNext()) {
      if (!heap.offer(iterator.next())) {
        nSkipped++;
      }
    }
    @SuppressWarnings("unchecked")
    E[] topK = (E[]) new Object[heap.size()];
    for (int i = topK.length - 1; !heap.isEmpty(); i--) {
      topK[i] = heap.remove();
    }
    return Collections.unmodifiableList(Arrays.asList(topK));
  }

  private static <E> List<E> greatestKQuick(Comparator<? super E> comparator,
      Iterable<E> elements, int k) {
    return Ordering.from(comparator).greatestOf(elements, k);
  }

  public static <E> List<E> greatestKQuick(Comparator<? super E> comparator,
      final Iterator<E> iterator, int k) {
    return greatestKQuick(comparator, new Iterable<E>() {
      @Override public Iterator<E> iterator() {
        return iterator;
      }
    }, k);
  }

  private static final class BoundedDeque<E> extends ForwardingQueue<E> {
    private final int capacity;
    private final Queue<E> backing;

    private BoundedDeque(int capacity) {
      this.capacity = capacity;
      backing = new ArrayDeque<E>(capacity);
    }

    @Override protected Queue<E> delegate() {
      return backing;
    }

    @Override public boolean offer(E o) {
      if (size() == capacity) {
        poll();
      }
      return super.offer(o);
    }
  }

  public static <E> List<E> greatestKSoft(Comparator<? super E> comparator,
      Iterator<E> iterator, int k) {
    /*
     * TODO: optimize for increasing input.  Possibly specialize on increasing
     * runs a la timsort.
     */
    checkNotNull(comparator);
    checkArgument(k >= 0);
    if (k == 0)
      return ImmutableList.of();
    PeekingIterator<E> iter = Iterators.peekingIterator(iterator);
    SoftHeap<E> heap = new SoftHeap<E>(comparator);
    while (iter.hasNext() && heap.size() < 2 * k) {
      heap.add(iter.next());
    }

    if (iter.hasNext()) {
      BoundedDeque<E> run = new BoundedDeque<E>(k);

      runLoop : while (iter.hasNext()) {
        int count = 0;
        while (comparator.compare(heap.peekMin(), iter.peek()) > 0) {
          iter.next();
          if (!iter.hasNext()) {
            break runLoop;
          }
        }
        E current = iter.next();
        run.offer(current);
        count++;

        while (iter.hasNext()) {
          if (comparator.compare(current, iter.peek()) > 0) {
            if (comparator.compare(heap.peekMin(), iter.peek()) > 0) {
              iter.next();
              continue;
            } else {
              break;
            }
          }
          run.offer(current = iter.next());
          count++;
        }
        while (!run.isEmpty()) {
          heap.add(run.poll());
          heap.extractMin();
        }
      }
    }
    @SuppressWarnings("unchecked")
    E[] top2K = (E[]) heap.toArray();
    return greatestKQuick(comparator, Arrays.asList(top2K), k);
  }
}
