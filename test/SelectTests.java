import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

public class SelectTests extends TestCase {
  public void testSelect() {
    Random random = new Random(0);
    for (int z = 1; z <= 1000; z++) {
      int n = random.nextInt(100000);
      int k = random.nextInt(1000);
      int[] elems = new int[n];
      for (int i = 0; i < n; i++) {
        elems[i] = random.nextInt();
      }
      List<Integer> selectHeap = Select.greatestKHeap(Ordering.natural(), Ints
        .asList(elems).iterator(), k);
      List<Integer> selectSoft = Select.greatestKSoft(Ordering.natural(), Ints
        .asList(elems).iterator(), k);
      assertEquals(selectHeap, selectSoft);
    }
  }
}
