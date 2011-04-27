import com.google.common.primitives.Ints;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Benchmark {

  private static final class CountingComparator implements Comparator<Integer> {
    private int comparisonsMade = 0;

    @Override public int compare(Integer o1, Integer o2) {
      comparisonsMade++;
      return o1 - o2;
    }

    public void reset() {
      comparisonsMade = 0;
    }
  }

  public static void main(String[] args) {
    Random random = new Random(0);
    int n = 1000000;
    int[] elems = new int[n];
    for (int i = 0; i < n; i++) {
      elems[i] = random.nextInt();
    }
    int k = 1000;
    List<Integer> list = Collections.unmodifiableList(Ints.asList(elems));
    CountingComparator comparator = new CountingComparator();
    Select.greatestKHeap(comparator, list.iterator(), k);
    int heapComps = comparator.comparisonsMade;
    comparator.reset();
    Select.greatestKSoft(comparator, list.iterator(), k);
    int softComps = comparator.comparisonsMade;
    comparator.reset();
    Select.greatestKQuick(comparator, list.iterator(), k);
    int quickComps = comparator.comparisonsMade;
    comparator.reset();
    System.out.println("Soft comparisons: " + softComps);
    System.out.println("Heap comparisons: " + heapComps);
    System.out.println("Quick comparisons: " + quickComps);
    long startTime = System.currentTimeMillis();
    for (int z = 0; z < 10; z++) {
      Select.greatestKSoft(comparator, list.iterator(), k);
    }
    long softTime = System.currentTimeMillis() - startTime;
    System.out.println("Soft time: " + softTime);
    startTime = System.currentTimeMillis();
    for (int z = 0; z < 10; z++) {
      Select.greatestKHeap(comparator, list.iterator(), k);
    }
    long heapTime = System.currentTimeMillis() - startTime;
    System.out.println("Heap time: " + heapTime);
    startTime = System.currentTimeMillis();
    for (int z = 0; z < 10; z++) {
      Select.greatestKQuick(comparator, list.iterator(), k);
    }
    long quickTime = System.currentTimeMillis() - startTime;
    System.out.println("Quick time: " + quickTime);
  }
}
