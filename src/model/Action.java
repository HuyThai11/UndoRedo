/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Nguyen Van Thang (SE201550), Person 1 - Core Model and Searching
 */
package model;

import java.util.Objects;

public class Action {

    public enum Type { INSERT, DELETE, REPLACE }

    private final Type type;
    private final int position;
    private final String oldText;
    private final String newText;

    public Action(Type type, int position, String oldText, String newText) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (oldText == null || newText == null) {
            throw new IllegalArgumentException("oldText and newText must not be null (use \"\" for empty)");
        }
        this.type = type;
        this.position = position;
        this.oldText = oldText;
        this.newText = newText;
    }

    // O(1) - only swaps fields, does not touch the real document
    public Action inverse() {
        switch (type) {
            case INSERT:
                return new Action(Type.DELETE, position, newText, "");
            case DELETE:
                return new Action(Type.INSERT, position, "", oldText);
            case REPLACE:
                return new Action(Type.REPLACE, position, newText, oldText);
            default:
                throw new IllegalStateException("Unknown action type: " + type);
        }
    }

    public Type getType() {
        return type;
    }

    public int getPosition() {
        return position;
    }

    public String getOldText() {
        return oldText;
    }

    public String getNewText() {
        return newText;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Action)) {
            return false;
        }
        Action action = (Action) other;
        return position == action.getPosition()
                && type == action.getType()
                && oldText.equals(action.getOldText())
                && newText.equals(action.getNewText());
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, position, oldText, newText);
    }

    @Override
    public String toString() {
        return String.format("Action[%s @%d, old=\"%s\", new=\"%s\"]",
                type, position, oldText, newText);
    }
}
