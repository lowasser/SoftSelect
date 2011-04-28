import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Ordering;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Benchmark {

  private static final class CountingComparator<E> implements Comparator<E> {
    private final Comparator<? super E> comparator;
    private int comparisonsMade = 0;

    private CountingComparator(Comparator<? super E> comparator) {
      this.comparator = comparator;
    }

    @Override public int compare(E o1, E o2) {
      comparisonsMade++;
      return comparator.compare(o1, o2);
    }

    public void reset() {
      comparisonsMade = 0;
    }
  }

  private static final class ByteArray implements Comparable<ByteArray> {
    private final byte[] array;

    private ByteArray(Random random) {
      this.array = new byte[random.nextInt(100)];
      random.nextBytes(array);
    }

    @Override public boolean equals(Object obj) {
      if (obj instanceof ByteArray) {
        return Arrays.equals(array, ((ByteArray) obj).array);
      }
      return false;
    }

    @Override public int compareTo(ByteArray o) {
      checkNotNull(o);
      int m = Math.min(array.length, o.array.length);
      for (int i = 0; i < m; i++) {
        int x = array[i] - o.array[i];
        if (x != 0) {
          return x;
        }
      }
      return array.length - o.array.length;
    }
  }

  public static void main(String[] args) {
    Random random = new Random(100);
    int n = 1000000;
    ByteArray[] elems = new ByteArray[n];
    for (int i = 0; i < n; i++) {
      elems[i] = new ByteArray(random);
    }
    int k = 100;
    List<ByteArray> list = Collections.unmodifiableList(Arrays.asList(elems));
    CountingComparator<Comparable> comparator = new CountingComparator<Comparable>(
        Ordering.natural());
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
