import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

public class SelectTests extends TestCase {
  public void testSelectSoft() {
    Random random = new Random(0);
    for (int z = 1; z <= 1000; z++) {
      int n = random.nextInt(z);
      int k = random.nextInt(n + 1);
      int[] elems = new int[n];
      for (int i = 0; i < n; i++) {
        elems[i] = random.nextInt(n);
      }
      List<Integer> list = Ints.asList(elems);
      List<Integer> expected = Ordering.natural().reverse().sortedCopy(list)
        .subList(0, k);
      assertEquals(expected,
          Select.greatestKSoft(Ordering.natural(), list.iterator(), k));
    }
  }

  public void testSelectQuick() {
    Random random = new Random(0);
    for (int z = 1; z <= 1000; z++) {
      int n = random.nextInt(z);
      int k = random.nextInt(n + 1);
      int[] elems = new int[n];
      for (int i = 0; i < n; i++) {
        elems[i] = random.nextInt(n);
      }
      List<Integer> list = Ints.asList(elems);
      List<Integer> expected = Ordering.natural().reverse().sortedCopy(list)
        .subList(0, k);
      assertEquals(expected,
          Select.greatestKQuick(Ordering.natural(), list.iterator(), k));
    }
  }

  public void testSelectSoft2() {
    Random random = new Random(0);
    for (int z = 1; z <= 1000; z++) {
      int n = random.nextInt(z);
      int k = random.nextInt(n + 1);
      int[] elems = new int[n];
      for (int i = 0; i < n; i++) {
        elems[i] = random.nextInt(n);
      }
      List<Integer> list = Ints.asList(elems);
      List<Integer> expected = Ordering.natural().reverse().sortedCopy(list)
        .subList(0, k);
      assertEquals(expected,
          Select.greatestKSoft2(Ordering.natural(), list.iterator(), k));
    }
  }

  public void testSelectHeap() {
    Random random = new Random(0);
    for (int z = 1; z <= 1000; z++) {
      int n = random.nextInt(z);
      int k = random.nextInt(n + 1);
      int[] elems = new int[n];
      for (int i = 0; i < n; i++) {
        elems[i] = random.nextInt(n);
      }
      List<Integer> list = Ints.asList(elems);
      List<Integer> expected = Ordering.natural().reverse().sortedCopy(list)
        .subList(0, k);
      assertEquals(expected,
          Select.greatestKHeap(Ordering.natural(), list.iterator(), k));
    }
  }
}
