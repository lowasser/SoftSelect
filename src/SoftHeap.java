import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.collect.Iterators;

import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

public class SoftHeap<E> extends AbstractCollection<E> {
  private static final class ElemList<E> extends AbstractCollection<E> {
    private static final class ElemLinkedListNode<E> {
      @Nullable private final E elem;
      private ElemLinkedListNode<E> next;

      private ElemLinkedListNode(@Nullable E elem, ElemLinkedListNode<E> next) {
        this.elem = elem;
        this.next = next;
      }
    }

    /**
     * Points to the first node of the linked list, or null if the list is
     * empty.
     */
    private ElemLinkedListNode<E> head;
    private int size;
    /**
     * Points to the last node of the linked list, or null if the list is empty.
     */
    private ElemLinkedListNode<E> tail;

    /**
     * Constructs an empty ElemList.
     */
    ElemList() {
      this.head = null;
      this.tail = null;
      this.size = 0;
    }

    /**
     * Constructs an ElemList with a single element.
     */
    ElemList(@Nullable E elem) {
      ElemLinkedListNode<E> node = new ElemLinkedListNode<E>(elem, null);
      this.head = node;
      this.tail = node;
      this.size = 1;
    }

    public void clear() {
      this.head = null;
      this.tail = null;
      this.size = 0;
    }

    public void consume(ElemList<E> list) {
      checkNotNull(list);
      if (list.isEmpty()) {
        return;
      }
      if (isEmpty()) {
        this.head = list.head;
        this.tail = list.tail;
        this.size = list.size;
      } else {
        this.tail.next = list.head;
        this.size += list.size;
        this.tail = list.tail;
      }
      list.clear();
    }

    public boolean isEmpty() {
      return size == 0;
    }

    @Override public Iterator<E> iterator() {
      return new Iterator<E>() {
        private ElemLinkedListNode<E> cursor = head;

        @Override public boolean hasNext() {
          return cursor != null;
        }

        @Override public E next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          E elem = cursor.elem;
          cursor = cursor.next;
          return elem;
        }

        @Override public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    /**
     * Deletes and returns an arbitrary element from the linked list. Throws a
     * {@link NoSuchElementException} if the list is empty.
     */
    public E pick() {
      if (isEmpty()) {
        throw new NoSuchElementException();
      }
      E elem = head.elem;
      head = head.next;
      size--;
      if (size == 0) {
        head = null;
        tail = null;
      }
      return elem;
    }

    @Override public int size() {
      return size;
    }
  }

  private final class Heap extends AbstractCollection<E> {
    Optional<Tree> first = Optional.absent();
    int rank = 0;
    int size = 0;

    public Heap() {
    }

    public Heap(@Nullable E elem) {
      this.first = Optional.of(new Tree(elem));
      this.size = 1;
    }

    public E extractMin() {
      if (!first.isPresent()) {
        throw new NoSuchElementException();
      }
      Tree t = first.get().sufmin();
      Node x = t.root();
      E e = x.list.pick();
      if (x.list.size() * 2 <= x.targetSize()) {
        if (!x.isLeaf()) {
          // System.err.println("Sifting at " + t);
          x.sift();
          t.updateSuffixMin();
        } else if (x.list.isEmpty()) {
          // System.err.println("Removing " + t);
          removeTree(t);
          if (t.hasPrev())
            t.prev().updateSuffixMin();
        }
      }
      size--;
      return e;
    }

    public Heap insert(@Nullable E elem) {
      return meld(new Heap(elem));
    }

    @Override public boolean isEmpty() {
      return !first.isPresent();
    }

    @Override public Iterator<E> iterator() {
      Tree t = first.get(null);
      Iterator<E> iterator = Iterators.emptyIterator();
      while (t != null) {
        iterator = Iterators.concat(iterator, t.root.iterator());
        t = t.next.get(null);
      }
      return iterator;
    }

    public Heap meld(Heap q) {
      checkNotNull(q);
      Heap p = this;
      if (p.rank > q.rank) {
        Heap tmp = p;
        p = q;
        q = tmp;
      }
      p.mergeInto(q);
      q.repeatedCombine(p.rank);
      p.first = null;
      p.rank = 0;
      q.size += p.size;
      p.size = 0;
      return q;
    }

    public void mergeInto(Heap q) {
      assert rank <= q.rank;
      if (!first.isPresent()) {
        return;
      }
      Tree t1 = first.get();
      Tree t2 = q.first.get();
      while (true) {
        while (t1.rank() > t2.rank()) {
          t2 = t2.next();
        }
        Optional<Tree> t1Prime = t1.next;
        q.insertTree(t1, t2);
        if (t1Prime.isPresent()) {
          t1 = t1Prime.get();
        } else {
          break;
        }
      }
    }

    public void repeatedCombine(int k) {
      if (!first.isPresent()) {
        return;
      }
      Tree t = first.get();
      while (t.hasNext()) {
        if (t.rank() == t.next().rank()) {
          if (!t.next().hasNext() || t.rank() != t.next().next().rank()) {
            t.root = new Node(t.root, t.next().root);
            removeTree(t.next());
            if (!t.hasNext()) {
              break;
            }
          }
        } else if (t.rank() > k) {
          break;
        }
        t = t.next();
      }
      if (t.rank() > rank) {
        this.rank = t.rank();
      }
      t.updateSuffixMin();
    }

    @Override public int size() {
      return size;
    }

    private void insertTree(Tree t1, Tree t2) {
      checkNotNull(t1);
      checkNotNull(t2);
      Optional<Tree> t2Present = Optional.of(t2);
      Optional<Tree> t1Present = Optional.of(t1);
      if (t2.hasPrev()) {
        t2.prev().next = t1Present;
        t1.prev = t2.prev;
      } else {
        first = t1Present;
      }
      t1.next = t2Present;
      t2.prev = t1Present;
    }

    private void removeTree(Tree t) {
      checkNotNull(t);
      if (!t.hasPrev()) {
        first = t.next;
      } else {
        t.prev().next = t.next;
      }
      if (t.hasNext()) {
        t.next().prev = t.prev;
      }
    }
  }

  private final class Node implements Iterable<E>, Comparable<Node> {
    @Nullable private E ckey = null;
    private Optional<Node> left = Optional.absent();
    private final ElemList<E> list;
    private final int rank;
    private Optional<Node> right = Optional.absent();

    /**
     * Constructs a singleton tree.
     */
    Node(@Nullable E elem) {
      this.ckey = elem;
      this.list = new ElemList<E>(elem);
      this.rank = 0;
    }

    /**
     * Constructs a tree of rank k+1 from two trees of rank k.
     */
    Node(Node left, Node right) {
      assert left.rank == right.rank;
      this.rank = left.rank + 1;
      this.left = Optional.of(left);
      this.right = Optional.of(right);
      this.list = new ElemList<E>();
      sift();
    }

    private int targetSize() {
      return sizeTable[rank];
    }

    @Override public int compareTo(Node t) {
      checkNotNull(t);
      return compare(ckey, t.ckey);
    }

    @Override public Iterator<E> iterator() {
      if (hasLeft()) {
        if (hasRight()) {
          return Iterators.concat(list.iterator(), left().iterator(), right()
            .iterator());
        } else {
          return Iterators.concat(list.iterator(), left().iterator());
        }
      } else if (hasRight()) {
        return Iterators.concat(list.iterator(), right().iterator());
      } else {
        return list.iterator();
      }
    }

    boolean hasLeft() {
      return left.isPresent();
    }

    boolean hasRight() {
      return right.isPresent();
    }

    Node left() {
      return left.get();
    }

    Node right() {
      return right.get();
    }

    void sift() {
      while (list.size() < targetSize() && !isLeaf()) {
        if (!left.isPresent()
            || (right.isPresent() && left.get().compareTo(right.get()) > 0)) {
          Optional<Node> tmp = left;
          left = right;
          right = tmp;
        }
        list.consume(left().list);
        ckey = left().ckey;
        if (left().isLeaf()) {
          left = Optional.absent();
        } else {
          left().sift();
        }
      }
    }

    private boolean isLeaf() {
      return !(hasLeft() || hasRight());
    }
  }

  private final class Tree implements Comparable<Tree> {
    private Optional<Tree> next = Optional.absent();
    private Optional<Tree> prev = Optional.absent();
    private Node root;
    private Tree sufmin = this;

    public Tree(@Nullable E elem) {
      this.root = new Node(elem);
    }

    @Override public int compareTo(Tree t) {
      return root.compareTo(t.root);
    }

    public boolean hasNext() {
      return next.isPresent();
    }

    public boolean hasPrev() {
      return prev.isPresent();
    }

    public Tree next() {
      return next.get();
    }

    public Tree prev() {
      return prev.get();
    }

    public int rank() {
      return root.rank;
    }

    public Node root() {
      return checkNotNull(root);
    }

    public Tree sufmin() {
      return checkNotNull(sufmin);
    }

    public void updateSuffixMin() {
      if (!hasNext() || compareTo(next().sufmin) <= 0) {
        sufmin = this;
      } else {
        sufmin = next().sufmin;
      }
      if (hasPrev()) {
        prev().updateSuffixMin();
      }
    }
  }

  private final int[] sizeTable = new int[31];
  private Comparator<? super E> comparator;
  private Heap heap = new Heap();

  SoftHeap(Comparator<? super E> comparator, double epsilon) {
    this.comparator = checkNotNull(comparator);
    checkArgument(epsilon > 0 && epsilon < 1);
    int r = 5 + (int) Math.ceil(-Math.log(epsilon) / Math.log(2));
    for (int i = 0; i <= r; i++) {
      sizeTable[i] = 1;
    }
    for (int i = r + 1; i < sizeTable.length; i++) {
      sizeTable[i] = (3 * sizeTable[i - 1] + 1) / 2;
    }
  }

  public boolean add(E elem) {
    heap = heap.insert(elem);
    return true;
  }

  /**
   * Deletes an element from the heap. The element returned will be the element
   * with the smallest current key, but the current key of an element may be
   * greater than the element. If the heap is empty, throws a
   * {@link NoSuchElementException}.
   */
  public E extractMin() {
    return heap.extractMin();
  }

  public boolean isEmpty() {
    return heap.isEmpty();
  }

  @Override public Iterator<E> iterator() {
    return heap.iterator();
  }

  public Optional<E> peekKey() {
    if (heap.isEmpty()) {
      return Optional.absent();
    }
    return Optional.of(heap.first.get().sufmin().root.ckey);
  }

  public int size() {
    return heap.size();
  }

  private int compare(E a, E b) {
    return comparator.compare(a, b);
  }
}
