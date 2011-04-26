import com.google.common.collect.Ordering;

import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;

public class SoftHeapTests extends TestCase {
  public void testSoftHeap() {
    Random random = new Random(0);
    for (int z = 1; z <= 1000; z++) {
      int n = random.nextInt(z);
      double epsilon = random.nextDouble();
      int[] elems = new int[n];
      for (int i = 0; i < n; i++) {
        elems[i] = random.nextInt();
      }
      SoftHeap<Integer> heap = new SoftHeap<Integer>(Ordering.natural(),
          epsilon);
      for (int i : elems) {
        heap.add(i);
      }
      Arrays.sort(elems);
      int[] outputs = new int[n];
      int corrupt = 0;
      for (int i = 0; !heap.isEmpty(); i++) {
        assertEquals(n - i, heap.size());
        int curKey = heap.peekKey().get();
        int extract = heap.extractMin().get();
        assertTrue("curKey=" + curKey + " < extract=" + extract,
            curKey >= extract);
        outputs[i] = extract;
        if (curKey > extract) {
          corrupt++;
        }
      }
      assertTrue("corrupt=" + corrupt + " > epsilon=" + epsilon + " * n=" + n,
          corrupt <= epsilon * n);
    }
  }
}
