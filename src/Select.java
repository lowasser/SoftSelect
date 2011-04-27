import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ForwardingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

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

    /**
     * Kicks out the smallest element if it would make the queue bigger than k.
     */
    public boolean add(@Nullable E element) {
      return offer(element);
    }

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
    Ordering<? super E> ordering = Ordering.from(comparator);
    switch (k) {
      case 0:
        return ImmutableList.of();
      case 1:
        if (!iterator.hasNext())
          return ImmutableList.of();
        E max = iterator.next();
        while (iterator.hasNext()) {
          max = ordering.max(max, iterator.next());
        }
        return Collections.singletonList(max);
      default:
        Queue<E> heap = new BoundedPriorityQueue<E>(comparator, k);
        while (iterator.hasNext()) {
          heap.offer(iterator.next());
        }
        @SuppressWarnings("unchecked")
        E[] topK = (E[]) new Object[heap.size()];
        for (int i = topK.length - 1; !heap.isEmpty(); i--) {
          topK[i] = heap.remove();
        }
        return Collections.unmodifiableList(Arrays.asList(topK));
    }
  }

  public static <E> List<E> greatestKQuick(Comparator<? super E> comparator,
      final Iterator<E> iterator, int k) {
    return Ordering.from(comparator).greatestOf(new Iterable<E>() {
      @Override public Iterator<E> iterator() {
        return iterator;
      }
    }, k);
  }

  public static <E> List<E> greatestKSoft(Comparator<? super E> comparator,
      Iterator<E> iterator, int k) {
    checkNotNull(comparator);
    Ordering<? super E> ordering = Ordering.from(comparator);
    switch (k) {
      case 0:
        return ImmutableList.of();
      case 1:
        if (!iterator.hasNext())
          return ImmutableList.of();
        E max = iterator.next();
        while (iterator.hasNext()) {
          max = ordering.max(max, iterator.next());
        }
        return Collections.singletonList(max);
      default:
        SoftHeap<E> heap = new SoftHeap<E>(comparator);
        while (iterator.hasNext() && heap.size() < 2 * k) {
          heap.add(iterator.next());
        }
        int alphaCompares = 0;
        if (iterator.hasNext()) {
          while (iterator.hasNext()) {
            E elem = iterator.next();
            alphaCompares++;
            if (comparator.compare(heap.peekMin(), elem) < 0) {
              heap.extractMin();
              heap.add(elem);
            }
          }
        }
        Queue<E> topKHeap = new BoundedPriorityQueue<E>(comparator, k);
        heap.addAllTo(topKHeap);
        @SuppressWarnings("unchecked")
        E[] topK = (E[]) new Object[topKHeap.size()];
        for (int i = topK.length - 1; !topKHeap.isEmpty(); i--) {
          topK[i] = topKHeap.remove();
        }
        return Collections.unmodifiableList(Arrays.asList(topK));
    }
  }
}
