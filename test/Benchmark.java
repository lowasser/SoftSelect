import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Benchmark {

  private static final class CountingComparator<E> extends Ordering<E> {
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

  private static int countComparisonsSoft(Iterator<? extends Comparable> iter,
      int k) {
    CountingComparator<Comparable> comparator = new CountingComparator<Comparable>(
        Ordering.natural());
    Select.greatestKSoft(comparator, iter, k);
    return comparator.comparisonsMade;
  }

  private static int countComparisonsSoft2(Iterator<? extends Comparable> iter,
      int k) {
    CountingComparator<Comparable> comparator = new CountingComparator<Comparable>(
        Ordering.natural());
    Select.greatestKSoft2(comparator, iter, k);
    return comparator.comparisonsMade;
  }

  private static int countComparisonsHeap(Iterator<? extends Comparable> iter,
      int k) {
    CountingComparator<Comparable> comparator = new CountingComparator<Comparable>(
        Ordering.natural());
    Select.greatestKHeap(comparator, iter, k);
    return comparator.comparisonsMade;
  }

  private static int countComparisonsQuick(Iterator<? extends Comparable> iter,
      int k) {
    CountingComparator<Comparable> comparator = new CountingComparator<Comparable>(
        Ordering.natural());
    Select.greatestKQuick(comparator, iter, k);
    return comparator.comparisonsMade;
  }

  private static <T> void mostlySort(List<T> elems,
      Comparator<? super T> comparator) {
    SoftHeap<T> heap = new SoftHeap<T>(comparator);
    for (T t : elems) {
      heap.add(t);
    }
    for (int i = 0; i < elems.size(); i++) {
      elems.set(i, heap.extractMin());
    }
  }

  public static void main(String[] args) throws IOException {
    List<Integer> words = Lists.newArrayList();
    int n = 1000000;

    Random random = new Random(234723);
    for (int i = 0; i < n; i++) {
      words.add(random.nextInt());
    }
    List<? extends Comparable> list = words;
    Comparator<Comparable> comparator = Ordering.natural();
    int k = 100;
    System.out.println("RANDOM");
    for (int z = 0; z < 10; z++) {
      Select.greatestKSoft2(comparator, list.iterator(), k);
      Select.greatestKSoft(comparator, list.iterator(), k);
      Select.greatestKHeap(comparator, list.iterator(), k);
      Select.greatestKQuick(comparator, list.iterator(), k);
    }
    benchmark(list, k);

    mostlySort(list, Ordering.natural());
    for (int z = 0; z < 10; z++) {
      Select.greatestKSoft2(comparator, list.iterator(), k);
      Select.greatestKSoft(comparator, list.iterator(), k);
      Select.greatestKHeap(comparator, list.iterator(), k);
      Select.greatestKQuick(comparator, list.iterator(), k);
    }
    System.out.println("MOSTLY SORTED");
    benchmark(list, k);

    Collections.sort(list, Ordering.natural());
    for (int z = 0; z < 10; z++) {
      Select.greatestKSoft2(comparator, list.iterator(), k);
      Select.greatestKSoft(comparator, list.iterator(), k);
      Select.greatestKHeap(comparator, list.iterator(), k);
      Select.greatestKQuick(comparator, list.iterator(), k);
    }
    System.out.println("SORTED");
    benchmark(list, k);

    Collections.reverse(list);
    for (int z = 0; z < 10; z++) {
      Select.greatestKSoft2(comparator, list.iterator(), k);
      Select.greatestKSoft(comparator, list.iterator(), k);
      Select.greatestKHeap(comparator, list.iterator(), k);
      Select.greatestKQuick(comparator, list.iterator(), k);
    }
    System.out.println("REVERSE SORTED");
    benchmark(list, k);
  }

  private static void benchmark(List<? extends Comparable> list, int k) {
    long startTime = System.currentTimeMillis();

    System.out.println("k = " + k + "; n = " + list.size());

    Comparator<Comparable> comparator = Ordering.natural();
    for (int z = 0; z < 10; z++) {
      Select.greatestKSoft(comparator, list.iterator(), k);
    }
    long softTime = System.currentTimeMillis() - startTime;
    System.out.println("Soft time: " + softTime);
    startTime = System.currentTimeMillis();
    for (int z = 0; z < 10; z++) {
      Select.greatestKSoft2(comparator, list.iterator(), k);
    }
    long soft2Time = System.currentTimeMillis() - startTime;
    System.out.println("Soft2 time: " + soft2Time);

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

    System.out.println("Soft compares:\t"
        + countComparisonsSoft(list.iterator(), k));
    System.out.println("Soft2 compares:\t"
        + countComparisonsSoft2(list.iterator(), k));
    System.out.println("Heap compares:\t"
        + countComparisonsHeap(list.iterator(), k));
    System.out.println("Quick compares:\t"
        + countComparisonsQuick(list.iterator(), k));
  }
}
