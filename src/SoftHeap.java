import static com.google.common.base.Preconditions.checkNotNull;

import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

public class SoftHeap<E> {
  private Comparator<? super E> comparator;

  private int compare(E a, E b) {
    return comparator.compare(a, b);
  }

  private class ElemList extends AbstractCollection<E> {
    private class ElemLinkedListNode {
      @Nullable private E elem;
      @Nullable private ElemLinkedListNode next;

      private ElemLinkedListNode(E elem, ElemLinkedListNode next) {
        this.elem = elem;
        this.next = next;
      }
    }

    /**
     * Points to the first node of the linked list, or null if the list is
     * empty.
     */
    private ElemLinkedListNode head;
    /**
     * Points to the last node of the linked list, or null if the list is empty.
     */
    private ElemLinkedListNode tail;
    private int size;

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
      ElemLinkedListNode node = new ElemLinkedListNode(elem, null);
      this.head = node;
      this.tail = node;
    }

    public boolean isEmpty() {
      return size == 0;
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

    public void clear() {
      this.head = null;
      this.tail = null;
      this.size = 0;
    }

    public void consume(ElemList list) {
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

    private boolean invariant() {
      int n = 0;
      for (E e : this) {
        n++;
      }
      boolean answer = n == size;
      answer = answer && (size == 0) == (head == null);
      answer = answer && (size == 0) == (tail == null);
      answer = answer && (tail == null || tail.next == null);
      return answer;
    }

    @Override public Iterator<E> iterator() {
      return new Iterator<E>() {
        private ElemLinkedListNode cursor = head;

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

    @Override public int size() {
      return size;
    }
  }
}
