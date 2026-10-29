/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Nguyen Van Thang (SE201550), Person 1 - Core Model and Searching
 */
package model;

/** Mutable document backed by a gap buffer for repeated edits near one cursor. */
public class DocumentBuffer {

    private static final int INITIAL_GAP_SIZE = 16;

    private char[] data;
    private int gapStart;
    private int gapEnd;

    public DocumentBuffer() {
        data = new char[INITIAL_GAP_SIZE];
        gapStart = 0;
        gapEnd = data.length;
    }

    public DocumentBuffer(String initialText) {
        if (initialText == null) {
            throw new IllegalArgumentException("initialText must not be null");
        }
        long capacity = (long) initialText.length() + INITIAL_GAP_SIZE;
        if (capacity > Integer.MAX_VALUE - 8L) {
            throw new OutOfMemoryError("Document is too large");
        }
        data = new char[(int) capacity];
        initialText.getChars(0, initialText.length(), data, 0);
        gapStart = initialText.length();
        gapEnd = data.length;
    }

    public void insert(int position, String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        checkPosition(position);
        if (text.isEmpty()) {
            return;
        }

        ensureGapCapacity(text.length());
        moveGap(position);
        text.getChars(0, text.length(), data, gapStart);
        gapStart += text.length();
    }

    public void delete(int position, int length) {
        checkPosition(position);
        checkLength(length);
        checkRangeEnd(position, length);
        if (length == 0) {
            return;
        }

        moveGap(position);
        gapEnd += length;
    }

    public String substring(int start, int end) {
        checkPosition(start);
        if (end < start || end > length()) {
            throw new IndexOutOfBoundsException(
                    "Range [" + start + ", " + end + ") exceeds buffer length " + length());
        }
        char[] result = new char[end - start];
        copyRange(start, end, result);
        return new String(result);
    }

    public boolean regionMatches(int position, int length, String text) {
        if (text == null || length < 0 || text.length() != length
                || position < 0 || position > length()
                || length > length() - position) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            int index = position + i;
            char actual = index < gapStart ? data[index] : data[index + gapEnd - gapStart];
            if (actual != text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public void replace(int position, int length, String newText) {
        if (newText == null) {
            throw new IllegalArgumentException("newText must not be null");
        }
        checkPosition(position);
        checkLength(length);
        checkRangeEnd(position, length);

        if (newText.length() > length) {
            ensureGapCapacity(newText.length() - length);
        }
        delete(position, length);
        insert(position, newText);
    }

    public void apply(Action action) {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        switch (action.getType()) {
            case INSERT:
                insert(action.getPosition(), action.getNewText());
                break;
            case DELETE:
                checkOldTextMatches(action);
                delete(action.getPosition(), action.getOldText().length());
                break;
            case REPLACE:
                checkOldTextMatches(action);
                replace(action.getPosition(), action.getOldText().length(), action.getNewText());
                break;
            default:
                throw new IllegalStateException("Unknown action type: " + action.getType());
        }
    }

    private void checkOldTextMatches(Action action) {
        checkPosition(action.getPosition());
        checkRangeEnd(action.getPosition(), action.getOldText().length());
        String actual = substring(action.getPosition(), action.getPosition() + action.getOldText().length());
        if (!actual.equals(action.getOldText())) {
            throw new IllegalArgumentException("Action does not match the document at position "
                    + action.getPosition() + ": expected \"" + action.getOldText() + "\" but found \"" + actual + "\"");
        }
    }

    public String getText() {
        char[] result = new char[length()];
        copyRange(0, result.length, result);
        return new String(result);
    }

    public int length() {
        return data.length - (gapEnd - gapStart);
    }

    private void copyRange(int start, int end, char[] result) {
        int beforeGap = Math.max(0, Math.min(end, gapStart) - start);
        if (beforeGap > 0) {
            System.arraycopy(data, start, result, 0, beforeGap);
        }
        int suffixStart = Math.max(start, gapStart);
        int afterGap = end - suffixStart;
        if (afterGap > 0) {
            System.arraycopy(data, suffixStart + gapEnd - gapStart, result, beforeGap, afterGap);
        }
    }

    private void moveGap(int position) {
        if (position < gapStart) {
            int moved = gapStart - position;
            System.arraycopy(data, position, data, gapEnd - moved, moved);
            gapStart = position;
            gapEnd -= moved;
        } else if (position > gapStart) {
            int moved = position - gapStart;
            System.arraycopy(data, gapEnd, data, gapStart, moved);
            gapStart += moved;
            gapEnd += moved;
        }
    }

    private void ensureGapCapacity(int minimum) {
        if (gapEnd - gapStart >= minimum) {
            return;
        }

        long required = (long) length() + minimum + INITIAL_GAP_SIZE;
        long capacity = Math.max(required, (long) data.length * 2);
        if (capacity > Integer.MAX_VALUE - 8L) {
            throw new OutOfMemoryError("Document is too large");
        }

        char[] expanded = new char[(int) capacity];
        int suffixLength = data.length - gapEnd;
        int expandedGapEnd = expanded.length - suffixLength;
        System.arraycopy(data, 0, expanded, 0, gapStart);
        System.arraycopy(data, gapEnd, expanded, expandedGapEnd, suffixLength);
        data = expanded;
        gapEnd = expandedGapEnd;
    }

    private void checkPosition(int position) {
        if (position < 0 || position > length()) {
            throw new IndexOutOfBoundsException(
                    "Position " + position + " out of bounds for buffer length " + length());
        }
    }

    private void checkLength(int length) {
        if (length < 0) {
            throw new IndexOutOfBoundsException("Length " + length + " must not be negative");
        }
    }

    private void checkRangeEnd(int position, int length) {
        if (length > this.length() - position) {
            throw new IndexOutOfBoundsException(
                    "Range [" + position + ", " + position + "+" + length
                    + ") exceeds buffer length " + this.length());
        }
    }
}
