import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

public class SoftHeapTests extends TestCase {
  private final Random random = new Random(5234);

  public void testSoftHeap() {
    for (int z = 1; z <= 1000; z++) {
      int n = random.nextInt(z);
      int[] elems = new int[n];
      for (int i = 0; i < n; i++) {
        elems[i] = random.nextInt(n * 2);
      }
      List<Integer> list = Ints.asList(elems);
      int n1 = list.size();
      int k = (n1 - 1) / 2;
      SoftHeap<Integer> heap = new SoftHeap<Integer>(Ordering.natural());
      for (int i : list) {
        heap.add(i);
      }
      if (!heap.isEmpty()) {
        int alpha = heap.extractMin();
        int greater = 0;
        List<?> elems1 = Arrays.asList(heap.toArray());
        for (Object i : elems1) {
          if ((Integer) i >= alpha) {
            greater++;
          }
        }
        assertTrue(greater >= k);
      }
    }
  }

  public void testSoftHeapAddAll() {
    for (int z = 1; z <= 1000; z++) {
      int n = random.nextInt(z);
      int[] elems = new int[n];
      for (int i = 0; i < n; i++) {
        elems[i] = random.nextInt(n * 2);
      }
      List<Integer> list = Ints.asList(elems);
      int n1 = list.size();
      int k = (n1 - 1) / 2;
      SoftHeap<Integer> heap = new SoftHeap<Integer>(Ordering.natural());
      heap.addAll(list);
      if (!heap.isEmpty()) {
        int alpha = heap.extractMin();
        int greater = 0;
        List<?> elems1 = Arrays.asList(heap.toArray());
        for (Object i : elems1) {
          if ((Integer) i >= alpha) {
            greater++;
          }
        }
        assertTrue(greater >= k);
      }
    }
  }
}
