/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Vo Anh Kiet (SE200764), Person 2 - History Stacks and Sorting
 */
package model.history;


public interface HistoryStack<T> {

    /** Adds an item on top. A bounded stack that is full drops its oldest item. */
    void push(T item);

    /** Removes and returns the newest item. Throws StackEmptyException if empty. */
    T pop();

    /** Returns the newest item without removing it. Throws StackEmptyException if empty. */
    T top();

    /** Same as top(); the name used by java.util.Stack. Kept so the Lab 02 and Lab 03 code still compiles. */
    default T peek() {
        return top();
    }

    boolean isEmpty();

    int size();

    /** Removes all items. */
    void clear();
}
