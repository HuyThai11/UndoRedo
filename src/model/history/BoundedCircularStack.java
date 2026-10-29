/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Vo Anh Kiet (SE200764), Person 2 - History Stacks and Sorting
 */
package model.history;


public class BoundedCircularStack<T> implements HistoryStack<T> {
    private final Object[] data;
    private int top;
    private int size;

    public BoundedCircularStack(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }

        data = new Object[capacity];
        top = -1;
        size = 0;
    }

    @Override
    public void push(T item) {
        if (size < data.length) {
            top = (top + 1) % data.length;
            data[top] = item;
            size++;
            return;
        }

        // Full: discard the oldest element (the current bottom).
        // Moving top forward overwrites that oldest slot.
        top = (top + 1) % data.length;
        data[top] = item;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) {
            throw new StackEmptyException();
        }

        T item = (T) data[top];
        data[top] = null;
        top = (top - 1 + data.length) % data.length;
        size--;

        if (size == 0) {
            top = -1;
        }

        return item;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T top() {
        if (isEmpty()) {
            throw new StackEmptyException();
        }

        return (T) data[top];
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
        return data.length;
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            data[(top - i + data.length) % data.length] = null;
        }
        top = -1;
        size = 0;
    }
}
