import com.google.common.collect.Ordering;

import java.util.Arrays;
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
  }

  private static int
      countComparisonsSoft(List<? extends Comparable> list, int k) {
    CountingComparator<Comparable> comparator = new CountingComparator<Comparable>(
        Ordering.natural());
    Select.greatestKSoft(comparator, list.iterator(), k);
    return comparator.comparisonsMade;
  }

  private static int
      countComparisonsHeap(List<? extends Comparable> list, int k) {
    CountingComparator<Comparable> comparator = new CountingComparator<Comparable>(
        Ordering.natural());
    Select.greatestKHeap(comparator, list.iterator(), k);
    return comparator.comparisonsMade;
  }

  private static int countComparisonsQuick(List<? extends Comparable> list,
      int k) {
    CountingComparator<Comparable> comparator = new CountingComparator<Comparable>(
        Ordering.natural());
    Select.greatestKQuick(comparator, list.iterator(), k);
    return comparator.comparisonsMade;
  }

  private static <T> void
      mostlySort(T[] elems, Comparator<? super T> comparator) {
    SoftHeap<T> heap = new SoftHeap<T>(comparator);
    for (T t : elems) {
      heap.add(t);
    }
    for (int i = 0; i < elems.length; i++) {
      elems[i] = heap.extractMin();
    }
  }

  public static void main(String[] args) {
    Random random = new Random(100);
    int n = 1000000;
    Integer[] elems = new Integer[n];
    for (int i = 0; i < n; i++) {
      elems[i] = random.nextInt();
    }
    List<? extends Comparable> list = Arrays.asList(elems);
    // Collections.reverse(list);
    for (int k = 100; k <= 1000; k += 100) {
      System.out.println("k = " + k + "; n = " + n);
      System.out.println("Soft compares:\t" + countComparisonsSoft(list, k));
      System.out.println("Heap compares:\t" + countComparisonsHeap(list, k));
      System.out.println("Quick compares:\t" + countComparisonsQuick(list, k));
    }
    Comparator<Comparable> comparator = Ordering.natural();
    int k = 100;
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
