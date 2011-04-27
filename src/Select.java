import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

public final class Select {
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
        PriorityQueue<E> heap = new PriorityQueue<E>(k, comparator);
        while (iterator.hasNext() && heap.size() < k) {
          heap.add(iterator.next());
        }
        while (iterator.hasNext()) {
          E elem = iterator.next();
          if (comparator.compare(heap.peek(), elem) < 0) {
            heap.remove();
            heap.add(elem);
          }
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
        while (iterator.hasNext() && heap.size() <= 2 * k) {
          heap.add(iterator.next());
        }
        if (iterator.hasNext()) {
          E alpha = heap.extractMin();
          while (iterator.hasNext()) {
            E elem = iterator.next();
            if (comparator.compare(alpha, elem) < 0) {
              heap.add(elem);
              alpha = ordering.max(alpha, heap.extractMin());
            }
          }
        }
        @SuppressWarnings("unchecked")
        E[] top2K = (E[]) heap.toArray();
        Arrays.sort(top2K, ordering.reverse());
        return k < top2K.length ? Arrays.asList(top2K).subList(0, k) : Arrays
          .asList(top2K);
    }
  }
}
