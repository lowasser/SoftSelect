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

  private static class RandomIterator implements Iterator<Integer> {
    private final Random random;
    private long length;

    private RandomIterator(long seed, long length) {
      random = new Random(seed);
      this.length = length;
    }

    @Override public boolean hasNext() {
      return length >= 0;
    }

    @Override public Integer next() {
      length--;
      return random.nextInt();
    }

    @Override public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private static Iterator<Integer> newIterator(int n) {
    return new RandomIterator(598457293, n);
  }

  public static void main(String[] args) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader("dictionary.txt"));
    List<String> words = Lists.newArrayList();
    while (reader.ready()) {
      words.add(reader.readLine());
    }
    Random random = new Random(234723);
    int n = words.size();
    Collections.shuffle(words, random);
    mostlySort(words, Ordering.natural());
    List<? extends Comparable> list = Collections.unmodifiableList(words);
    // Collections.reverse(list);
    for (int k = 100; k <= 1000; k += 100) {
      System.out.println("k = " + k + "; n = " + n);
      System.out.println("Soft compares:\t"
          + countComparisonsSoft(list.iterator(), k));
      System.out.println("Heap compares:\t"
          + countComparisonsHeap(list.iterator(), k));
      System.out.println("Quick compares:\t"
          + countComparisonsQuick(list.iterator(), k));
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
