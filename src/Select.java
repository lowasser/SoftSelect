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
        SoftHeap<E> heap = new SoftHeap<E>(comparator, 0.5);
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
        PriorityQueue<E> topKHeap = new PriorityQueue<E>(k, comparator);
        while (!heap.isEmpty()) {
          E elem = heap.extractMin();
          if (topKHeap.size() < k
              || comparator.compare(elem, topKHeap.peek()) > 0) {
            topKHeap.remove();
            topKHeap.add(elem);
          }
        }
        @SuppressWarnings("unchecked")
        E[] topK = (E[]) new Object[topKHeap.size()];
        for (int i = 0; !topKHeap.isEmpty(); i++) {
          topK[i] = topKHeap.remove();
        }
        return Collections.unmodifiableList(Arrays.asList(topK));
    }
  }
}
