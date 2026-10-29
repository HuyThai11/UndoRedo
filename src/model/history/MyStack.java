/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Vo Anh Kiet (SE200764), Person 2 - History Stacks and Sorting
 */
package model.history;


public class MyStack<T> implements HistoryStack<T> {
    private StackNode<T> top;
    private int size;

    public MyStack() {
        top = null;
        size = 0;
    }

    @Override
    public void push(T item) {
        StackNode<T> newNode = new StackNode<>(item);
        newNode.next = top;
        top = newNode;
        size++;
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

    /** Remove all items. O(1): the garbage collector frees the old nodes. */
    @Override
    public void clear() {
        top = null;
        size = 0;
    }
}
