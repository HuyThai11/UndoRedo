/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Nguyen Van Thang (SE201550), Person 1 - Core Model and Searching
 */
package test;

import model.Action;
import model.DocumentBuffer;

/**
 * Demo and self-check for the Core Model (Action + DocumentBuffer).
 * Run this class: every line must print PASS.
 */
public class CoreModelDemo {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testInsertThenDelete();
        testInvalidPositions();
        testInverseAllTypes();
        testReplaceRoundTrip();
        testReplaceDifferentLength();
        testUndoRedoCycle();
        testInvalidArguments();
        testInsertAtEnds();
        testEditsAcrossGap();
        testActionValueEquality();
        testApplyRejectsWrongOldText();

        System.out.println();
        System.out.println("Passed: " + passed + "  Failed: " + failed);
        System.out.println(failed == 0 ? "ALL CORE MODEL TESTS PASSED." : "SOME CORE MODEL TESTS FAILED.");
    }

    // Insert then delete exactly what was inserted -> text returns to the original.
    private static void testInsertThenDelete() {
        DocumentBuffer buffer = new DocumentBuffer("Hello World");
        buffer.insert(5, ", there");
        report("Insert gives 'Hello, there World'", "Hello, there World".equals(buffer.getText()));

        buffer.delete(5, ", there".length());
        report("Delete returns 'Hello World'", "Hello World".equals(buffer.getText()));
    }

    // Wrong positions must be rejected and must NOT change the text.
    private static void testInvalidPositions() {
        DocumentBuffer buffer = new DocumentBuffer("abc");

        report("Insert at -1 throws and keeps text",
                throwsIndexError(buffer, "insert", -1, 0) && "abc".equals(buffer.getText()));
        report("Insert at 99 throws and keeps text",
                throwsIndexError(buffer, "insert", 99, 0) && "abc".equals(buffer.getText()));
        report("Delete past the end throws and keeps text",
                throwsIndexError(buffer, "delete", 1, 5) && "abc".equals(buffer.getText()));
        report("Delete with negative length throws and keeps text",
                throwsIndexError(buffer, "delete", 1, -1) && "abc".equals(buffer.getText()));
        report("Replace past the end throws and keeps text",
                throwsIndexError(buffer, "replace", 2, 5) && "abc".equals(buffer.getText()));
    }

    // inverse() must swap correctly for INSERT, DELETE and REPLACE.
    private static void testInverseAllTypes() {
        Action insert = new Action(Action.Type.INSERT, 3, "", "xyz");
        Action undoInsert = insert.inverse();
        report("inverse(INSERT) is DELETE of the inserted text",
                undoInsert.getType() == Action.Type.DELETE
                && undoInsert.getPosition() == 3
                && "xyz".equals(undoInsert.getOldText())
                && "".equals(undoInsert.getNewText()));

        Action delete = new Action(Action.Type.DELETE, 2, "ab", "");
        Action undoDelete = delete.inverse();
        report("inverse(DELETE) is INSERT of the deleted text",
                undoDelete.getType() == Action.Type.INSERT
                && undoDelete.getPosition() == 2
                && "".equals(undoDelete.getOldText())
                && "ab".equals(undoDelete.getNewText()));

        Action replace = new Action(Action.Type.REPLACE, 1, "old", "new");
        Action undoReplace = replace.inverse();
        report("inverse(REPLACE) swaps old and new",
                undoReplace.getType() == Action.Type.REPLACE
                && undoReplace.getPosition() == 1
                && "new".equals(undoReplace.getOldText())
                && "old".equals(undoReplace.getNewText()));

        Action twice = replace.inverse().inverse();
        report("inverse().inverse() gives the original action",
                twice.getType() == replace.getType()
                && twice.getPosition() == replace.getPosition()
                && twice.getOldText().equals(replace.getOldText())
                && twice.getNewText().equals(replace.getNewText()));
    }

    private static void testReplaceRoundTrip() {
        DocumentBuffer buffer = new DocumentBuffer("I like cats");
        Action replace = new Action(Action.Type.REPLACE, 7, "cats", "dogs");

        buffer.apply(replace);
        report("Replace gives 'I like dogs'", "I like dogs".equals(buffer.getText()));

        buffer.apply(replace.inverse());
        report("Apply inverse gives 'I like cats' again", "I like cats".equals(buffer.getText()));
    }

    private static void testReplaceDifferentLength() {
        DocumentBuffer buffer = new DocumentBuffer("I like cats");
        Action replace = new Action(Action.Type.REPLACE, 7, "cats", "elephants");

        buffer.apply(replace);
        boolean longer = "I like elephants".equals(buffer.getText());
        buffer.apply(replace.inverse());
        boolean back = "I like cats".equals(buffer.getText());

        report("Replace with a longer text and undo it", longer && back);
    }

    // How the Controller uses the model: Redo = apply(action), Undo = apply(action.inverse()).
    private static void testUndoRedoCycle() {
        DocumentBuffer buffer = new DocumentBuffer("Hello World");
        Action insert = new Action(Action.Type.INSERT, 5, "", ", there");

        buffer.apply(insert);
        buffer.apply(insert.inverse());
        boolean undone = "Hello World".equals(buffer.getText());
        buffer.apply(insert);
        boolean redone = "Hello, there World".equals(buffer.getText());

        report("Do, Undo, Redo gives the right text each time", undone && redone);
    }

    private static void testInvalidArguments() {
        boolean nullTextRejected = false;
        try {
            new DocumentBuffer("abc").insert(0, null);
        } catch (IllegalArgumentException e) {
            nullTextRejected = true;
        }

        boolean nullActionRejected = false;
        try {
            new Action(Action.Type.INSERT, 0, null, "x");
        } catch (IllegalArgumentException e) {
            nullActionRejected = true;
        }

        report("null text is rejected (no 'null' inserted)", nullTextRejected);
        report("Action with null text is rejected", nullActionRejected);
    }

    private static void testInsertAtEnds() {
        DocumentBuffer buffer = new DocumentBuffer("bc");
        buffer.insert(0, "a");
        buffer.insert(buffer.length(), "d");
        boolean ok = "abcd".equals(buffer.getText());
        buffer.delete(0, buffer.length());
        report("Insert at start / end, then delete everything",
                ok && buffer.length() == 0 && "".equals(buffer.getText()));
    }

    private static void testEditsAcrossGap() {
        DocumentBuffer buffer = new DocumentBuffer("abcdefghij");
        buffer.delete(3, 2);
        boolean deleted = "abcfghij".equals(buffer.getText())
                && "bcfghi".equals(buffer.substring(1, 7));
        buffer.insert(6, "XY");
        boolean inserted = "abcfghXYij".equals(buffer.getText())
                && "hXYi".equals(buffer.substring(5, 9));
        buffer.replace(2, 4, "Q");
        report("Edits and substrings across the moving gap",
                deleted && inserted && "abQXYij".equals(buffer.getText()));
    }

    private static void testActionValueEquality() {
        Action first = new Action(Action.Type.REPLACE, 2, "old", "new");
        Action same = new Action(Action.Type.REPLACE, 2, "old", "new");
        Action different = new Action(Action.Type.REPLACE, 2, "old", "next");
        report("Actions compare by type, position and text",
                first.equals(same) && first.hashCode() == same.hashCode()
                        && !first.equals(different) && !first.equals(null));
    }

    // Lab 03: apply() must check that oldText is really in the document (3 checks).
    private static void testApplyRejectsWrongOldText() {
        DocumentBuffer buffer = new DocumentBuffer("I like cats");

        boolean deleteRejected = false;
        try {
            buffer.apply(new Action(Action.Type.DELETE, 7, "dogs", ""));   // text there is "cats"
        } catch (IllegalArgumentException e) {
            deleteRejected = true;
        }
        report("apply(DELETE) with a wrong oldText is rejected and the text is unchanged",
                deleteRejected && "I like cats".equals(buffer.getText()));

        boolean replaceRejected = false;
        try {
            buffer.apply(new Action(Action.Type.REPLACE, 7, "dogs", "birds"));
        } catch (IllegalArgumentException e) {
            replaceRejected = true;
        }
        report("apply(REPLACE) with a wrong oldText is rejected and the text is unchanged",
                replaceRejected && "I like cats".equals(buffer.getText()));

        boolean pastEndRejected = false;
        try {
            buffer.apply(new Action(Action.Type.DELETE, 7, "cats and more", ""));   // runs past the end
        } catch (IndexOutOfBoundsException e) {
            pastEndRejected = true;
        }
        report("apply(DELETE) with an oldText running past the end is rejected and the text is unchanged",
                pastEndRejected && "I like cats".equals(buffer.getText()));
    }

    // Calls insert/delete/replace with bad arguments and checks for IndexOutOfBoundsException.
    private static boolean throwsIndexError(DocumentBuffer buffer, String method, int position, int length) {
        try {
            if ("insert".equals(method)) {
                buffer.insert(position, "x");
            } else if ("delete".equals(method)) {
                buffer.delete(position, length);
            } else {
                buffer.replace(position, length, "x");
            }
        } catch (IndexOutOfBoundsException e) {
            return true;
        }
        return false;
    }

    private static void report(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
    }
}
