/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Vo Anh Kiet (SE200764), Person 2 - History Stacks and Sorting
 */
package model.history;


public class StackEmptyException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public StackEmptyException() {
        super("Stack is empty");
    }
}
