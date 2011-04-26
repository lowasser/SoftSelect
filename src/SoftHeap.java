import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

public class SoftHeap<E> extends AbstractCollection<E> {
  private static final class ElemList<E> extends AbstractCollection<E> {
    private static final class ElemLinkedListNode<E> {
      private E elem;
      private ElemLinkedListNode<E> next;

      private ElemLinkedListNode(E elem, ElemLinkedListNode<E> next) {
        this.elem = checkNotNull(elem);
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
      checkNotNull(elem);
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
      assert invariant();
      assert list.invariant();
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
      assert invariant();
      return elem;
    }

    @Override public int size() {
      return size;
    }

    private boolean invariant() {
      int n = 0;
      for (E e : this) {
        checkNotNull(e);
        n++;
      }
      assert n == size;
      assert (size == 0) == (head == null);
      assert (size == 0) == (tail == null);
      assert (tail == null || tail.next == null);
      return true;
    }
  }

  private final class Heap extends AbstractCollection<E> {
    Optional<Tree> first = Optional.absent();
    int rank = 0;
    int size = 0;

    public Heap() {
    }

    public Heap(E elem) {
      checkNotNull(elem);
      this.first = Optional.of(new Tree(elem));
      this.size = 1;
    }

    @Override public boolean isEmpty() {
      return !first.isPresent();
    }

    public Optional<E> extractMin() {
      if (!first.isPresent()) {
        return Optional.absent();
      }
      assert invariant();
      Tree t = first.get().sufmin();
      Node x = t.root();
      E e = x.list.pick();
      if (x.list.size() * 2 <= x.targetSize) {
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
      assert invariant();
      return Optional.of(e);
    }

    public Heap insert(E elem) {
      return meld(new Heap(elem));
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
      assert q.invariant();
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

    private boolean suffixFollows(Tree t) {
      Tree t2 = t;
      while (t2 != t.sufmin) {
        if (!t2.hasNext()) {
          return false;
        }
        t2 = t2.next();
      }
      return true;
    }

    private boolean doesNotLoopForward(Tree t) {
      Tree t2 = t;
      do {
        t2 = t.next.get(null);
        assert (t != t2);
      } while (t2 != null);
      return true;
    }

    private boolean doesNotLoopBackward(Tree t) {
      Tree t2 = t;
      do {
        t2 = t.prev.get(null);
        assert (t != t2);
      } while (t2 != null);
      return true;
    }

    private boolean nextPrev(Tree t) {
      return (!t.hasNext() || t.next().prev() == t)
          && (!t.hasPrev() || t.prev().next() == t);
    }

    private boolean invariant() {
      Tree t = first.get(null);
      int n = 0;
      int nCorrupt = 0;
      for (E e : this) {
        n++;
      }
      assert n == size;
      while (t != null) {
        assert suffixFollows(t);
        //assert doesNotLoopForward(t);
        //assert doesNotLoopBackward(t);
        assert nextPrev(t);
        assert !t.root.list.isEmpty();
        nCorrupt += t.root.numCorrupt();
        t = t.next.get(null);
      }
      //System.err.println("nCorrupt = " + nCorrupt + "; n = " + n
      //  + "; n * epsilon = " + (n * epsilon));
      assert nCorrupt <= n * epsilon;
      return true;
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

    @Override public Iterator<E> iterator() {
      Tree t = first.get(null);
      Iterator<E> iterator = Iterators.emptyIterator();
      while (t != null) {
        iterator = Iterators.concat(iterator, t.root.iterator());
        t = t.next.get(null);
      }
      return iterator;
    }

    @Override public int size() {
      return size;
    }
  }

  private final class Node implements Iterable<E>, Comparable<Node> {
    E ckey = null;
    Optional<Node> left = Optional.absent();
    final ElemList<E> list;
    final int rank;
    Optional<Node> right = Optional.absent();
    final int targetSize;

    /**
     * Constructs a singleton tree.
     */
    Node(E elem) {
      this.ckey = checkNotNull(elem);
      this.list = new ElemList<E>(elem);
      this.targetSize = 1;
      this.rank = 0;
      assert invariant();
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
      if (this.rank <= r) {
        this.targetSize = 1;
      } else {
        this.targetSize = (3 * left.targetSize + 1) / 2;
      }
      sift();
      assert invariant();
    }

    @Override public int compareTo(Node t) {
      checkNotNull(t);
      return compare(ckey, t.ckey);
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

    int numCorrupt() {
      int numCorrupt = list.size() - 1;
      if (hasLeft()) {
        numCorrupt += left().numCorrupt();
      }
      if (hasRight()) {
        numCorrupt += right().numCorrupt();
      }
      return numCorrupt;
    }

    void sift() {
      while (list.size() < targetSize && !isLeaf()) {
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
      assert invariant();
    }

    private boolean invariant() {
      checkNotNull(ckey);
      if (hasLeft()) {
        assert !left().list.isEmpty();
      }
      if (hasRight()) {
        assert !right().list.isEmpty();
      }
      if (rank <= r) {
        assert list.size() == 1;
      } else if (!isLeaf()) {
        assert list.size() * 2 >= targetSize;
        assert list.size() < 3 * targetSize;
      }
      return true;
    }

    private boolean isLeaf() {
      return !(hasLeft() || hasRight());
    }

    @Override public String toString() {
      ToStringHelper helper = Objects.toStringHelper(this).add("ckey", ckey)
        .add("list", list);
      if (hasLeft()) {
        helper.add("left", left);
      }
      if (hasRight()) {
        helper.add("right", right);
      }
      return helper.toString();

    }

    @Override public Iterator<E> iterator() {
      Iterator<E> iterator = list.iterator();
      if (hasLeft()) {
        iterator = Iterators.concat(iterator, left().iterator());
      }
      if (hasRight()) {
        iterator = Iterators.concat(iterator, right().iterator());
      }
      return iterator;
    }
  }

  private final class Tree implements Comparable<Tree> {
    private Optional<Tree> next = Optional.absent();
    private Optional<Tree> prev = Optional.absent();
    private Node root;
    private Tree sufmin = this;

    public Tree(E elem) {
      checkNotNull(elem);
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

    public String toString() {
      ToStringHelper helper = Objects.toStringHelper(this).add("id",
          System.identityHashCode(this));
      helper = helper.add("root", root).add("sufmin",
          System.identityHashCode(sufmin));
      String ans = helper.toString();
      if (hasNext()) {
        return ans + " " + next().toString();
      } else {
        return ans;
      }
    }
  }

  private Comparator<? super E> comparator;

  private final double epsilon;
  final int r;
  private Heap heap = new Heap();

  SoftHeap(Comparator<? super E> comparator, double epsilon) {
    this.comparator = checkNotNull(comparator);
    this.epsilon = epsilon;
    checkArgument(epsilon > 0 && epsilon < 1);
    this.r = 5 + (int) Math.ceil(-Math.log(epsilon) / Math.log(2));
  }

  public String rankString() {
    List<Integer> ranks = Lists.newArrayList();
    Map<Tree, Integer> index = new IdentityHashMap<Tree, Integer>();
    Tree t = heap.first.get(null);
    int k = 0;
    while (t != null) {
      index.put(t, k++);
      ranks.add(t.rank());
      t = t.next.get(null);
    }
    List<Integer> sufmins = Lists.newArrayList();
    t = heap.first.get(null);
    while (t != null) {
      sufmins.add(index.get(t.sufmin));
      t = t.next.get(null);
    }
    return ranks.toString() + " " + sufmins.toString();
  }

  public boolean add(E elem) {
    heap = heap.insert(elem);
    return true;
  }

  public Optional<E> peekKey() {
    if (heap.isEmpty()) {
      return Optional.absent();
    }
    return Optional.of(heap.first.get().sufmin().root.ckey);
  }

  public Optional<E> extractMin() {
    return heap.extractMin();
  }

  public boolean isEmpty() {
    return heap.isEmpty();
  }

  public int size() {
    return heap.size();
  }

  private int compare(E a, E b) {
    return comparator.compare(a, b);
  }

  @Override public Iterator<E> iterator() {
    return heap.iterator();
  }

}
