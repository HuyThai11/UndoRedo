/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Huynh Huy Thai (SE203059), Person 3 - Controller
 */
package controller;

import model.ActionDiff;
import model.algorithm.LineSorter;
import model.algorithm.TextSearch;
import model.history.BoundedCircularStack;
import model.history.HistoryStack;
import model.history.MyStack;
import java.util.List;
import model.Action;
import model.DocumentBuffer;

/** Coordinates document edits and undo/redo history. */
public class Controller {

    private final DocumentBuffer buffer;
    private final HistoryStack<Action> undoStack;
    private final HistoryStack<Action> redoStack;
    private final int maxHistory; // -1 if unbounded

    /**
     * Creates a Controller with unbounded history (backwards compatible with Lab 02 and Lab 03).
     */
    public Controller() {
        this.buffer = new DocumentBuffer();
        this.undoStack = new MyStack<>();
        this.redoStack = new MyStack<>();
        this.maxHistory = -1;
    }

    /**
     * Creates a Controller with initial text and unbounded history.
     *
     * @param initialText initial document content
     */
    public Controller(String initialText) {
        this.buffer = new DocumentBuffer(initialText);
        this.undoStack = new MyStack<>();
        this.redoStack = new MyStack<>();
        this.maxHistory = -1;
    }

    /**
     * Creates a Controller with bounded history using circular array stacks (Lab 04 core feature).
     *
     * @param maxHistory maximum number of undo/redo steps to retain (must be > 0)
     * @throws IllegalArgumentException if maxHistory <= 0
     */
    public Controller(int maxHistory) {
        if (maxHistory <= 0) {
            throw new IllegalArgumentException("maxHistory must be greater than 0");
        }
        this.buffer = new DocumentBuffer();
        this.undoStack = new BoundedCircularStack<>(maxHistory);
        this.redoStack = new BoundedCircularStack<>(maxHistory);
        this.maxHistory = maxHistory;
    }

    /**
     * Creates a Controller with initial text and bounded history.
     *
     * @param initialText initial document content
     * @param maxHistory maximum number of undo/redo steps to retain (must be > 0)
     * @throws IllegalArgumentException if maxHistory <= 0
     */
    public Controller(String initialText, int maxHistory) {
        if (maxHistory <= 0) {
            throw new IllegalArgumentException("maxHistory must be greater than 0");
        }
        this.buffer = new DocumentBuffer(initialText);
        this.undoStack = new BoundedCircularStack<>(maxHistory);
        this.redoStack = new BoundedCircularStack<>(maxHistory);
        this.maxHistory = maxHistory;
    }

    public boolean executeInsert(int position, String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        try {
            buffer.insert(position, text);
            Action action = new Action(Action.Type.INSERT, position, "", text);
            undoStack.push(action);
            clearRedo();
            return true;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    public boolean executeDelete(int position, int length) {
        if (length <= 0 || position < 0 || position > buffer.length()
                || length > buffer.length() - position) {
            return false;
        }

        String deletedText = buffer.substring(position, position + length);
        buffer.delete(position, length);

        Action action = new Action(Action.Type.DELETE, position, deletedText, "");
        undoStack.push(action);
        clearRedo();
        return true;
    }

    public boolean executeReplace(int position, int length, String newText) {
        if (newText == null || length < 0 || position < 0 || position > buffer.length()
                || length > buffer.length() - position) {
            return false;
        }

        String oldText = buffer.substring(position, position + length);
        if (oldText.equals(newText)) {
            return false; // no change: no Action, and the Redo stack is kept
        }
        buffer.replace(position, length, newText);

        Action action = new Action(Action.Type.REPLACE, position, oldText, newText);
        undoStack.push(action);
        clearRedo();
        return true;
    }

    public boolean isReplacementNoOp(int position, int length, String newText) {
        return buffer.regionMatches(position, length, newText);
    }

    /**
     * Finds all start indices of pattern occurrences using KMP.
     * Read-only query: does not modify buffer or stacks.
     *
     * @param pattern pattern to search
     * @return list of 0-based character offsets
     */
    public List<Integer> find(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("Search pattern cannot be null or empty.");
        }
        return TextSearch.findAllKmp(buffer.getText(), pattern);
    }

    public int executeReplaceAll(String pattern, String replacement) {
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("Search pattern cannot be null or empty.");
        }
        if (replacement == null) {
            replacement = "";
        }

        String oldText = buffer.getText();
        if (oldText.isEmpty()) {
            return 0;
        }

        List<Integer> matches = TextSearch.findAllKmp(oldText, pattern);
        if (matches.isEmpty() || pattern.equals(replacement)) {
            return 0; // Invariant: No change -> do not push Action, preserve redoStack
        }

        // Replace non-overlapping occurrences left-to-right
        StringBuilder sb = new StringBuilder();
        int lastIdx = 0;
        int count = 0;
        int patternLen = pattern.length();

        for (int idx : matches) {
            if (idx >= lastIdx) {
                sb.append(oldText, lastIdx, idx);
                sb.append(replacement);
                lastIdx = idx + patternLen;
                count++;
            }
        }
        sb.append(oldText.substring(lastIdx));
        String newText = sb.toString();

        if (count == 0 || newText.equals(oldText)) {
            return 0;
        }

        Action compactAction = ActionDiff.compact(oldText, newText);
        if (compactAction == null) {
            return 0;
        }

        buffer.apply(compactAction);

        undoStack.push(compactAction);

        clearRedo();

        return count;
    }


    public boolean executeSortLines() {
        String oldText = buffer.getText();
        if (oldText == null || oldText.isEmpty()) {
            return false;
        }

        String sortedText = LineSorter.sortLines(oldText);
        if (sortedText.equals(oldText)) {
            return false; // Invariant: Already sorted -> preserve redoStack
        }

        Action compactAction = ActionDiff.compact(oldText, sortedText);
        if (compactAction == null) {
            return false;
        }

        buffer.apply(compactAction);

        undoStack.push(compactAction);

        clearRedo();

        return true;
    }

    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }

        Action action = undoStack.pop();

        try {
            buffer.apply(action.inverse());
            redoStack.push(action);
            return true;
        } catch (IllegalArgumentException | IndexOutOfBoundsException | IllegalStateException e) {
            return false; // Drop rejected actions so they cannot block older history.
        }
    }


    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }

        Action action = redoStack.pop();

        try {
            buffer.apply(action);
            undoStack.push(action);
            return true;
        } catch (IllegalArgumentException | IndexOutOfBoundsException | IllegalStateException e) {
            return false; // Drop rejected actions so they cannot block older history.
        }
    }

    /**
     * Clears all recorded redo actions.
     */
    public void clearRedo() {
        redoStack.clear();
    }

    public String getText() {
        return buffer.getText();
    }

    public int getLength() {
        return buffer.length();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public int getUndoSize() {
        return undoStack.size();
    }

    public int getRedoSize() {
        return redoStack.size();
    }

    public Action peekUndo() {
        return undoStack.isEmpty() ? null : undoStack.top();
    }

    public Action peekRedo() {
        return redoStack.isEmpty() ? null : redoStack.top();
    }

    /**
     * Returns the maximum history capacity configured, or -1 if unbounded.
     */
    public int getMaxHistory() {
        return maxHistory;
    }

    /**
     * Checks if this controller is configured with bounded history.
     */
    public boolean isBounded() {
        return maxHistory > 0;
    }
}
