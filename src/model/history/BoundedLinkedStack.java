/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Vo Anh Kiet (SE200764), Person 2 - History Stacks and Sorting
 */
package model.history;


public class BoundedLinkedStack<T> implements HistoryStack<T> {
    private final int capacity;
    private StackNode<T> top;
    private int size;

    public BoundedLinkedStack(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }

        this.capacity = capacity;
        top = null;
        size = 0;
    }

    @Override
    public void push(T item) {
        StackNode<T> newNode = new StackNode<>(item);
        newNode.next = top;
        top = newNode;
        size++;

        if (size > capacity) {
            removeBottom();
        }
    }

    // O(n): walk to the second-to-last node, then cut the tail.
    private void removeBottom() {
        if (size == 1) {
            top = null;
            size = 0;
            return;
        }

        StackNode<T> current = top;
        while (current.next.next != null) {
            current = current.next;
        }

        current.next = null;
        size--;
    }

    @Override
    public T pop() {
        if (isEmpty()) {
            throw new StackEmptyException();
        }

        T item = top.data;
        top = top.next;
        size--;
        return item;
    }

    @Override
    public T top() {
        if (isEmpty()) {
            throw new StackEmptyException();
        }

        return top.data;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    public int capacity() {
        return capacity;
    }

    /** Removes all items. O(1): the garbage collector frees the old nodes. */
    @Override
    public void clear() {
        top = null;
        size = 0;
    }
}
