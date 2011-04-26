import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

public class SoftHeapTests extends TestCase {
  private final Random random = new Random(5234);

  private Iterator<List<Integer>> shrinks(final List<Integer> prevList) {
    final List<Integer> list = Collections.unmodifiableList(prevList);
    final int[] shuffle = new int[list.size()];
    for (int i = 0; i < shuffle.length; i++) {
      shuffle[i] = i;
    }
    for (int i = 0; i < shuffle.length; i++) {
      int j = random.nextInt(shuffle.length);
      int t = shuffle[i];
      shuffle[i] = shuffle[j];
      shuffle[j] = t;
    }
    return new Iterator<List<Integer>>() {
      private int i = 0;

      @Override public boolean hasNext() {
        return i < list.size();
      }

      @Override public List<Integer> next() {
        int[] array = new int[list.size() - 1];
        for (int j = 0; j < shuffle[i]; j++) {
          array[j] = list.get(j);
        }
        for (int j = shuffle[i] + 1; j < list.size(); j++) {
          array[j - 1] = list.get(j);
        }
        i++;
        return Collections.unmodifiableList(Ints.asList(array));
      }

      @Override public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  private List<Integer> failingList(List<Integer> init, double epsilon) {
    init = Collections.unmodifiableList(init);
    if (testList(init, epsilon)) {
      return null;
    } else {
      System.err.println("Shrinking...");
      List<Integer> shrunk = null;
      Iterator<List<Integer>> iter = shrinks(init);
      while (shrunk == null && iter.hasNext()) {
        shrunk = failingList(iter.next(), epsilon);
      }
      return (shrunk == null) ? init : shrunk;
    }
  }

  private boolean testList(List<Integer> list, double epsilon) {
    int n = list.size();
    int k = (n - 1) / 2;
    SoftHeap<Integer> heap = new SoftHeap<Integer>(Ordering.natural(), 0.5);
    for (int i : list) {
      heap.add(i);
    }
    if (heap.isEmpty())
      return true;
    int alpha = heap.extractMin().get();
    int greater = 0;
    for (Integer i : heap) {
      if (i > alpha) {
        greater++;
      }
    }
    return greater >= k;
  }

  public void testSoftHeap() {
    for (int z = 1; z <= 1000; z++) {
      int n = random.nextInt(200);
      double epsilon = random.nextDouble();
      int[] elems = new int[n];
      for (int i = 0; i < n; i++) {
        elems[i] = random.nextInt(n * 2);
      }
      List<Integer> shrunk = failingList(Ints.asList(elems), epsilon);
      if (shrunk != null) {
        SoftHeap<Integer> heap = new SoftHeap<Integer>(Ordering.natural(),
            epsilon);
        for (int i : shrunk) {
          heap.add(i);
        }
        while (!heap.isEmpty()) {
          heap.extractMin();
        }
        assertNull(shrunk.size() + " : " + shrunk.toString(), shrunk);
      }
    }
  }
}
