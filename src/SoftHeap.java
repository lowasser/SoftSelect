import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;

import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

public class SoftHeap<E> {
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
      boolean answer = n == size;
      answer = answer && (size == 0) == (head == null);
      answer = answer && (size == 0) == (tail == null);
      answer = answer && (tail == null || tail.next == null);
      return answer;
    }
  }

  private final class Tree implements Comparable<Tree> {
    E ckey;
    Optional<Tree> left = Optional.absent();
    final ElemList<E> list;
    final int rank;
    Optional<Tree> right = Optional.absent();
    final int targetSize;

    /**
     * Constructs a singleton tree.
     */
    Tree(E elem) {
      this.ckey = checkNotNull(elem);
      this.list = new ElemList<E>(elem);
      this.targetSize = 1;
      this.rank = 0;
      assert invariant();
    }

    /**
     * Constructs a tree of rank k+1 from two trees of rank k.
     */
    Tree(Tree left, Tree right) {
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

    @Override public int compareTo(Tree t) {
      checkNotNull(t);
      return compare(ckey, t.ckey);
    }

    boolean hasLeft() {
      return left.isPresent();
    }

    boolean hasRight() {
      return right.isPresent();
    }

    Tree left() {
      return left.get();
    }

    Tree right() {
      return right.get();
    }

    void sift() {
      while (list.size() < targetSize && !isLeaf()) {
        if (!left.isPresent()
            || (right.isPresent() && left.get().compareTo(right.get()) > 0)) {
          Optional<Tree> tmp = left;
          left = right;
          right = tmp;
        }
        list.consume(left().list);
        if (left().isLeaf()) {
          left = Optional.absent();
        } else {
          left().sift();
        }
      }
      assert invariant();
    }

    private boolean isLeaf() {
      return !(hasLeft() || hasRight());
    }

    private boolean invariant() {
      boolean good = true;
      if (hasLeft()) {
        good = good && !left().list.isEmpty();
      }
      if (hasRight()) {
        good = good && !right().list.isEmpty();
      }
      if (!isLeaf()) {
        good = good && !list.isEmpty();
      }
      return good;
    }
  }

  private Comparator<? super E> comparator;

  private final double epsilon;

  private final int r;

  SoftHeap(Comparator<? super E> comparator, double epsilon) {
    this.comparator = checkNotNull(comparator);
    this.epsilon = epsilon;
    checkArgument(epsilon > 0 && epsilon < 1);
    this.r = 5 + (int) Math.ceil(-Math.log(epsilon) / Math.log(2));
  }

  private int compare(E a, E b) {
    return comparator.compare(a, b);
  }

}
