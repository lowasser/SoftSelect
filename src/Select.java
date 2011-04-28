import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ForwardingQueue;
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
    checkArgument(k > 0);
    Queue<E> heap = new BoundedPriorityQueue<E>(comparator, k);
    int nSkipped = 0;
    while (iterator.hasNext()) {
      if (!heap.offer(iterator.next())) {
        nSkipped++;
      }
    }
    System.err.println("Completely skipped (H): " + nSkipped);
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

  public static <E> List<E> greatestKSoft(Comparator<? super E> comparator,
      Iterator<E> iterator, int k) {
    checkNotNull(comparator);
    checkArgument(k >= 0);
    SoftHeap<E> heap = new SoftHeap<E>(comparator);
    int n = 0;
    while (iterator.hasNext() && heap.size() < 2 * k) {
      heap.add(iterator.next());
      n++;
    }
    int alphaCompares = 0;
    int nSkipped = 0;
    if (iterator.hasNext()) {
      E alpha = heap.peekMin();
      while (iterator.hasNext()) {
        E elem = iterator.next();
        n++;
        alphaCompares++;
        if (comparator.compare(alpha, elem) < 0) {
          heap.extractMin();
          heap.add(elem);
          alpha = heap.peekMin();
        } else {
          nSkipped++;
        }
      }
    }
    System.err.println("Completely skipped (S): " + nSkipped);
    @SuppressWarnings("unchecked")
    E[] top2K = (E[]) heap.toArray();
    return greatestKQuick(comparator, Arrays.asList(top2K), k);
  }
}
