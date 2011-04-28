import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Ordering;

import java.util.AbstractCollection;
import java.util.Arrays;
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
    private final byte rank;

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
      this.rank = (byte) (left.rank + 1);
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
        if (hasRight() && compare(left.ckey, right.ckey) > 0) {
          Node tmp = left;
          left = right;
          right = tmp;
        }
        list.addAll(left.list);
        ckey = left.ckey;
        if (left.isLeaf()) {
          left = right;
          right = null;
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

    public byte rank() {
      return root.rank;
    }

    private Tree updateSuffixMin() {
      return suffixMin = hasNext() ? Ordering.natural().min(this,
          next.getSuffixMin()) : this;
    }

    private Tree getSuffixMin() {
      return (suffixMin.root == null) ? updateSuffixMin() : suffixMin;
    }
  }

  private static final double EPSILON = 0.5;
  private static final int R = 4;
  /**
   * NB: The bound in the paper specifies R=6 for epsilon=0.5. However, an exact
   * numerical analysis for this particular case proves that R=5 is guaranteed
   * to work -- and whenever N <= 2^20, R=4 also works. R=4 reduces the number
   * of comparisons by 15% compared to R=5 and 25% compared to R=6, so I think
   * that's reasonable, given that this is used for softselect, in which k is
   * typically not that big.
   */

  private static final int[] SIZE_TABLE = new int[31];

  static {
    Arrays.fill(SIZE_TABLE, 0, R + 1, 1);
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
  private byte rank = 0;
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
      Tree lastChanged = t1;
      do {
        t2 = t1.next;
        if (t1.rank() == t2.rank()) {
          if (!t2.hasNext() || t1.rank() != t2.next.rank()) {
            E t2Root = t2.root.ckey;
            t1.root = new Node(t1.root, t2.root);
            if (t1.root.ckey == t2Root) {
              t1.suffixMin = t2.suffixMin;
            } else {
              lastChanged = t1;
            }
            t1.next = t2.next;
            t2.root = null;
            if (t2.hasNext()) {
              t2.next.prev = t1;
            } else {
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
      updateSuffixMin(lastChanged);
      size++;
    }
    return true;
  }

  public E peekMin() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return first.getSuffixMin().root.list.head.elem;
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

  public int size() {
    return size;
  }

  public Object[] toArray() {
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
    if (t.hasPrev()) {
      t.prev.next = t.next;
    } else {
      first = t.next;
    }
    if (t.hasNext()) {
      t.next.prev = t.prev;
    }
    t.root = null;
  }

  private void updateSuffixMin(Tree t) {
    for (; t != null; t = t.prev)
      t.updateSuffixMin();
  }
}
