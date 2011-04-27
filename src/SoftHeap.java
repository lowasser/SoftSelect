import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.collect.Ordering;

import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

public final class SoftHeap<E> {
  private static final class ElemLinkedListNode<E> implements Linked<E> {
    @Nullable private final E elem;
    private ElemLinkedListNode<E> next;

    private ElemLinkedListNode(@Nullable E elem, ElemLinkedListNode<E> next) {
      this.elem = elem;
      this.next = next;
    }

    @Override public E get() {
      return elem;
    }

    @Override public ElemLinkedListNode<E> next() {
      return next;
    }
  }

  private static final class ElemList<E> extends AbstractCollection<E> {
    /**
     * Points to the first node of the linked list, or null if the list is
     * empty.
     */
    @Nullable private ElemLinkedListNode<E> head = null;
    private int size = 0;
    /**
     * Points to the last node of the linked list, or null if the list is empty.
     */
    @Nullable private ElemLinkedListNode<E> tail = null;

    /**
     * Constructs an empty ElemList.
     */
    private ElemList() {
    }

    /**
     * Constructs an ElemList with a single element.
     */
    private ElemList(@Nullable E elem) {
      this.head = this.tail = new ElemLinkedListNode<E>(elem, null);
      this.size = 1;
    }

    public void addAll(ElemList<E> list) {
      if (isEmpty()) {
        this.head = list.head;
      } else {
        this.tail.next = list.head;
      }
      this.size += list.size;
      this.tail = list.tail;
    }

    public void clear() {
      this.head = this.tail = null;
      this.size = 0;
    }

    public boolean isEmpty() {
      return head == null;
    }

    @Override public Iterator<E> iterator() {
      return linkedIterator(head);
    }

    /**
     * Deletes and returns an arbitrary element from the linked list. Throws a
     * {@link NoSuchElementException} if the list is empty.
     */
    public E pick() {
      E elem = head.elem;
      head = head.next;
      size--;
      if (head == null) {
        tail = null;
      }
      return elem;
    }

    @Override public int size() {
      return size;
    }
  }

  private static interface Linked<T> {
    public T get();

    public Linked<T> next();
  }

  private final class Node {
    @Nullable private E ckey = null;
    @Nullable private Node left = null;
    @Nullable private Node right = null;
    private final ElemList<E> list;
    private final int rank;

    /**
     * Constructs a singleton tree.
     */
    private Node(@Nullable E elem) {
      this.ckey = elem;
      this.list = new ElemList<E>(elem);
      this.rank = 0;
    }

    /**
     * Constructs a tree of rank k+1 from two trees of rank k.
     */
    private Node(Node left, Node right) {
      assert left.rank == right.rank;
      this.rank = left.rank + 1;
      this.left = left;
      this.right = right;
      this.list = new ElemList<E>();
      sift();
    }

    public int echoTo(Object[] buffer, int i) {
      for (E e : list) {
        buffer[i++] = e;
      }
      if (hasLeft()) {
        i = left.echoTo(buffer, i);
      }
      if (hasRight()) {
        i = right.echoTo(buffer, i);
      }
      return i;
    }

    boolean hasLeft() {
      return left != null;
    }

    boolean hasRight() {
      return right != null;
    }

    void sift() {
      while (list.size() < targetSize() && !isLeaf()) {
        if (!hasLeft() || (hasRight() && compare(left.ckey, right.ckey) > 0)) {
          Node tmp = left;
          left = right;
          right = tmp;
        }
        list.addAll(left.list);
        ckey = left.ckey;
        if (left.isLeaf()) {
          left = null;
        } else {
          left.list.clear();
          left.sift();
        }
      }
    }

    private boolean isLeaf() {
      return !(hasLeft() || hasRight());
    }

    private int targetSize() {
      return SIZE_TABLE[rank];
    }
  }

  private final class Tree implements Linked<Node>, Comparable<Tree> {
    @Nullable private Tree next = null;
    @Nullable private Tree prev = null;
    private Node root;
    private Tree suffixMin = this;

    private Tree(@Nullable E elem) {
      this.root = new Node(elem);
    }

    @Override public int compareTo(Tree t) {
      return compare(root.ckey, t.root.ckey);
    }

    @Override public Node get() {
      return root;
    }

    public boolean hasNext() {
      return next != null;
    }

    public boolean hasPrev() {
      return prev != null;
    }

    @Override public Tree next() {
      return next;
    }

    public int rank() {
      return root.rank;
    }

    private Tree getSuffixMin() {
      Tree sufMin = suffixMin;
      if (sufMin.root == null) {
        sufMin = this;
        if (hasNext()) {
          Tree nSufMin = next().getSuffixMin();
          if (compareTo(nSufMin) > 0) {
            sufMin = nSufMin;
          }
        }
        suffixMin = sufMin;
      }
      return sufMin;
    }
  }

  private static final double EPSILON = 0.5;
  private static final int R = 6;

  private static final int[] SIZE_TABLE = new int[31];

  static {
    for (int i = 0; i <= R; i++) {
      SIZE_TABLE[i] = 1;
    }
    for (int i = R + 1; i < SIZE_TABLE.length; i++) {
      SIZE_TABLE[i] = (3 * SIZE_TABLE[i - 1] + 1) / 2;
    }
  }

  private static <T> Iterator<T>
      linkedIterator(final @Nullable Linked<T> linked) {
    return new Iterator<T>() {
      private Linked<T> current = linked;

      @Override public boolean hasNext() {
        return current != null;
      }

      @Override public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        T cur = current.get();
        current = current.next();
        return cur;
      }

      @Override public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Nullable private Tree first = null;
  private int rank = 0;
  private int size = 0;
  private Comparator<? super E> comparator;

  SoftHeap(Comparator<? super E> comparator) {
    this.comparator = checkNotNull(comparator);
  }

  public boolean add(E elem) {
    if (isEmpty()) {
      first = new Tree(elem);
      rank = 0;
      size = 1;
    } else {
      Tree t2 = first;
      Tree t1 = first = new Tree(elem);
      t1.next = t2;
      t2.prev = t1;
      do {
        if (t1.rank() == t1.next.rank()) {
          if (!t1.next.hasNext() || t1.rank() != t1.next.next.rank()) {
            t1.root = new Node(t1.root, t1.next.root);
            removeTree(t1.next);
            if (!t1.hasNext()) {
              break;
            }
          }
        } else if (t1.rank() > 0) {
          break;
        }
        t1 = t1.next;
      } while (t1.hasNext());
      if (t1.rank() > rank) {
        this.rank = t1.rank();
      }
      updateSuffixMin(t1);
      size++;
    }
    return true;
  }

  public E extractMin() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    Tree t = first.getSuffixMin();
    Node x = t.root;
    E e = x.list.pick();
    if (x.list.size() * 2 <= x.targetSize()) {
      if (!x.isLeaf()) {
        x.sift();
        updateSuffixMin(t);
      } else if (x.list.isEmpty()) {
        removeTree(t);
      }
    }
    size--;
    return e;
  }

  public boolean isEmpty() {
    return first == null;
  }

  /**
   * Deletes an element from the heap. The element returned will be the element
   * with the smallest current key, but the current key of an element may be
   * greater than the element. If the heap is empty, throws a
   * {@link NoSuchElementException}.
   */

  public Optional<E> peekKey() {
    if (isEmpty()) {
      return Optional.absent();
    }
    return Optional.of(first.getSuffixMin().root.ckey);
  }

  public int size() {
    return size;
  }

  public Object[] toArray() {
    @SuppressWarnings("unchecked")
    Object[] elements = new Object[size()];
    Iterator<Node> rootIter = linkedIterator(first);
    int i = 0;
    while (rootIter.hasNext()) {
      i = rootIter.next().echoTo(elements, i);
    }
    assert i == elements.length;
    return elements;
  }

  private int compare(E a, E b) {
    return comparator.compare(a, b);
  }

  private void removeTree(Tree t) {
    if (!t.hasPrev()) {
      first = t.next;
    } else {
      t.prev.next = t.next;
    }
    if (t.hasNext()) {
      t.next.prev = t.prev;
    }
    t.root = null;
  }

  private void updateSuffixMin(Tree t) {
    Tree tmpmin = t;
    if (t.hasNext()) {
      tmpmin = Ordering.natural().min(t, t.next().getSuffixMin());
    }
    t.suffixMin = tmpmin;
    while (t.prev != null) {
      t = t.prev;
      if (t.compareTo(tmpmin) <= 0) {
        tmpmin = t;
      }
      t.suffixMin = tmpmin;
    }
  }
}
