/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Huynh Huy Thai (SE203059), Person 3 - Controller
 */
package test;

import model.ActionDiff;
import model.algorithm.LineSorter;
import controller.Controller;
import model.history.HistoryStack;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import model.Action;
import model.DocumentBuffer;

/**
 * Comprehensive correctness & integration tests for Controller in Lab 04 (includes all Lab 03 tests).
 * Implemented by Huynh Huy Thai (SE203059 - Person 3: Controller / Orchestrator).
 */
public class ControllerTest {

    private static int testsPassed = 0;
    private static int totalTests = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  CONTROLLER TEST SUITE - LAB 04");
        System.out.println("===============================================================");

        // Regression Tests from Lab 02
        runTest("Test 1: Basic Insert, Undo, Redo", ControllerTest::testBasicInsertUndoRedo);
        runTest("Test 2: Invariant Clear Redo on New Edit", ControllerTest::testInvariantClearRedoOnNewEdit);
        runTest("Test 3: Delete, Undo, Redo", ControllerTest::testDeleteUndoRedo);
        runTest("Test 4: Replace, Undo, Redo", ControllerTest::testReplaceUndoRedo);
        runTest("Test 5: Empty Stack Handling", ControllerTest::testEmptyStackGracefulHandling);
        runTest("Test 6: Boundary Error Handling", ControllerTest::testBoundaryErrorHandling);
        runTest("Test 7: Complex Editing Sequence", ControllerTest::testComplexEditingSequence);

        // New Algorithm Integration Tests in Lab 03
        runTest("Test 8: Find (KMP Integration)", ControllerTest::testFindKmp);
        runTest("Test 9: Execute Replace All & Atomic Undo/Redo", ControllerTest::testExecuteReplaceAll);
        runTest("Test 10: Invariant on Replace All (No-Op Preservation)", ControllerTest::testReplaceAllInvariants);
        runTest("Test 11: Execute Sort Lines & Atomic Undo/Redo", ControllerTest::testExecuteSortLines);
        runTest("Test 12: Invariant on Sort Lines (Already Sorted)", ControllerTest::testSortLinesInvariants);
        runTest("Test 13: Non-blocking Rollback on Apply Failure (Lab 04)", ControllerTest::testNonBlockingRollback);
        runTest("Test 14: Stress Random Sequence (500 Rounds, with Undo/Redo model)", ControllerTest::testStressRandomOperations);
        runTest("Test 15: Replace All Edge Cases", ControllerTest::testReplaceAllEdgeCases);
        runTest("Test 16: Sort Lines Edge Cases", ControllerTest::testSortLinesEdgeCases);

        // New in Lab 04
        runTest("Test 17: Compact Action in Replace All (Memory Reduction)", ControllerTest::testCompactActionReplaceAll);
        runTest("Test 18: Compact Action in Sort Lines", ControllerTest::testCompactActionSortLines);
        runTest("Test 19: Bounded Circular Stack Eviction (MAX_HISTORY = 3)", ControllerTest::testBoundedCircularStackEviction);
        runTest("Test 20: Bounded Circular Stack Redo Branch Management", ControllerTest::testBoundedCircularStackRedo);
        runTest("Test 21: Stress Random Operations with Bounded Model (500 Rounds)", ControllerTest::testStressRandomOperationsBounded);
        runTest("Test 22: Bounded Model with Replace All and Sort Lines (1,000 Rounds)", ControllerTest::testBoundedModelWithBulkEdits);
        runTest("Test 23: Replace With the Same Text Is a No-Op", ControllerTest::testReplaceSameTextNoOp);

        System.out.println("\n===============================================================");
        System.out.printf("  Passed: %d  Failed: %d%n", testsPassed, totalTests - testsPassed);
        if (testsPassed == totalTests) {
            System.out.println("  ALL CONTROLLER TESTS PASSED.");
        }
        System.out.println("===============================================================");
    }

    private static void runTest(String name, Runnable test) {
        totalTests++;
        System.out.print("[" + totalTests + "] " + name + " ... ");
        try {
            test.run();
            testsPassed++;
            System.out.println("PASS");
        } catch (Throwable t) {
            System.out.println("FAIL: " + t.getMessage());
            t.printStackTrace(System.out);
        }
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(msg + " | Expected: [" + expected + "] but was: [" + actual + "]");
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError(msg);
    }

    private static void assertFalse(boolean condition, String msg) {
        if (condition) throw new AssertionError(msg);
    }

    // --- Regression Tests ---

    private static void testBasicInsertUndoRedo() {
        Controller c = new Controller();
        c.executeInsert(0, "Hello");
        assertEquals("Hello", c.getText(), "Insert 0 'Hello'");

        c.executeInsert(5, " World");
        assertEquals("Hello World", c.getText(), "Insert 5 ' World'");

        assertTrue(c.undo(), "Undo should succeed");
        assertEquals("Hello", c.getText(), "Undo back to 'Hello'");

        assertTrue(c.redo(), "Redo should succeed");
        assertEquals("Hello World", c.getText(), "Redo back to 'Hello World'");
    }

    private static void testInvariantClearRedoOnNewEdit() {
        Controller c = new Controller();
        c.executeInsert(0, "A");
        c.executeInsert(1, "B");
        c.executeInsert(2, "C");
        c.undo(); // back to "AB", redo has "C"
        assertTrue(c.canRedo(), "Can redo before new edit");

        c.executeInsert(2, "D"); // Branching history
        assertEquals("ABD", c.getText(), "Text after branch");
        assertFalse(c.canRedo(), "Redo must be cleared after new edit");
        assertEquals(0, c.getRedoSize(), "Redo size must be 0");
    }

    private static void testDeleteUndoRedo() {
        Controller c = new Controller("ABCDEF");
        c.executeDelete(2, 2); // remove "CD" -> "ABEF"
        assertEquals("ABEF", c.getText(), "Text after delete");

        c.undo();
        assertEquals("ABCDEF", c.getText(), "Text after delete undo");

        c.redo();
        assertEquals("ABEF", c.getText(), "Text after delete redo");
    }

    private static void testReplaceUndoRedo() {
        Controller c = new Controller("Hello Cat");
        c.executeReplace(6, 3, "Dog"); // "Hello Dog"
        assertEquals("Hello Dog", c.getText(), "Text after replace");

        c.undo();
        assertEquals("Hello Cat", c.getText(), "Text after replace undo");

        c.redo();
        assertEquals("Hello Dog", c.getText(), "Text after replace redo");
    }

    // Test 23: replacing text with the same text changes nothing, so it must not
    // create an Undo step and must not clear the Redo stack ("no change = no step").
    private static void testReplaceSameTextNoOp() {
        Controller c = new Controller();
        c.executeInsert(0, "abc");
        c.executeInsert(3, "d");
        c.undo();                                   // "abc", Undo 1, Redo 1
        assertFalse(c.executeReplace(0, 1, "a"), "Replace a with a returns false");
        assertEquals("abc", c.getText(), "Text is unchanged");
        assertEquals(1, c.getUndoSize(), "No new Undo step");
        assertEquals(1, c.getRedoSize(), "Redo stack is kept");
        assertFalse(c.executeReplace(1, 0, ""), "Replace nothing with nothing returns false");
        assertTrue(c.redo(), "Redo still works");
        assertEquals("abcd", c.getText(), "Redo brings d back");
    }

    private static void testEmptyStackGracefulHandling() {
        Controller c = new Controller("Init");
        assertFalse(c.undo(), "Undo on empty stack should return false");
        assertFalse(c.redo(), "Redo on empty stack should return false");
        assertEquals("Init", c.getText(), "Text must remain intact");
    }

    private static void testBoundaryErrorHandling() {
        Controller c = new Controller("Test");
        assertFalse(c.executeInsert(-1, "X"), "Insert negative pos should fail");
        assertFalse(c.executeInsert(100, "X"), "Insert pos > length should fail");
        assertFalse(c.executeDelete(-1, 2), "Delete negative pos should fail");
        assertFalse(c.executeDelete(2, 10), "Delete length > bounds should fail");
        assertFalse(c.executeDelete(1, Integer.MAX_VALUE), "Delete overflow length should fail");
        assertFalse(c.executeReplace(-1, 2, "A"), "Replace negative pos should fail");
        assertFalse(c.executeReplace(2, 10, "A"), "Replace length > bounds should fail");
        assertFalse(c.executeReplace(1, Integer.MAX_VALUE, "A"), "Replace overflow length should fail");
        assertEquals("Test", c.getText(), "Buffer must not be corrupted");
    }

    private static void testComplexEditingSequence() {
        Controller c = new Controller();
        c.executeInsert(0, "The quick fox");
        c.executeInsert(10, "brown "); // "The quick brown fox"
        c.executeDelete(0, 4);          // "quick brown fox"
        c.executeReplace(12, 3, "dog"); // "quick brown dog"

        assertEquals("quick brown dog", c.getText(), "Final text");

        // Undo all 4 steps
        c.undo(); assertEquals("quick brown fox", c.getText(), "Undo 1");
        c.undo(); assertEquals("The quick brown fox", c.getText(), "Undo 2");
        c.undo(); assertEquals("The quick fox", c.getText(), "Undo 3");
        c.undo(); assertEquals("", c.getText(), "Undo 4");
        assertFalse(c.undo(), "No more undo");

        // Redo all 4 steps
        c.redo(); assertEquals("The quick fox", c.getText(), "Redo 1");
        c.redo(); assertEquals("The quick brown fox", c.getText(), "Redo 2");
        c.redo(); assertEquals("quick brown fox", c.getText(), "Redo 3");
        c.redo(); assertEquals("quick brown dog", c.getText(), "Redo 4");
        assertFalse(c.redo(), "No more redo");
    }

    // --- New Algorithm Tests ---

    private static void testFindKmp() {
        Controller c = new Controller("banana and band and banner");
        List<Integer> banMatches = c.find("ban");
        assertEquals(3, banMatches.size(), "Should find 3 occurrences of 'ban'");
        assertEquals(0, (int) banMatches.get(0), "Index of 1st 'ban'");
        assertEquals(11, (int) banMatches.get(1), "Index of 2nd 'ban'");
        assertEquals(20, (int) banMatches.get(2), "Index of 3rd 'ban'");

        // Overlapping match test: searching "aa" in "aaaa"
        Controller c2 = new Controller("aaaa");
        List<Integer> aaMatches = c2.find("aa");
        assertEquals(3, aaMatches.size(), "Should find 3 overlapping occurrences: [0, 1, 2]");
        assertEquals(0, (int) aaMatches.get(0), "Index 0");
        assertEquals(1, (int) aaMatches.get(1), "Index 1");
        assertEquals(2, (int) aaMatches.get(2), "Index 2");

        // Pattern not found
        List<Integer> none = c.find("orange");
        assertTrue(none.isEmpty(), "Should return empty list for non-existent pattern");

        // Invalid pattern arguments
        boolean caught = false;
        try {
            c.find("");
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assertTrue(caught, "Empty pattern must throw IllegalArgumentException");
    }

    private static void testExecuteReplaceAll() {
        Controller c = new Controller("cat and cat and cat");
        int replaced = c.executeReplaceAll("cat", "dog");
        assertEquals(3, replaced, "Should replace 3 instances");
        assertEquals("dog and dog and dog", c.getText(), "Text after Replace All");

        // Exactly 1 Undo must revert ALL 3 replacements
        assertTrue(c.undo(), "Undo must succeed");
        assertEquals("cat and cat and cat", c.getText(), "Single Undo restores full original text");

        // Exactly 1 Redo must re-apply ALL 3 replacements
        assertTrue(c.redo(), "Redo must succeed");
        assertEquals("dog and dog and dog", c.getText(), "Single Redo re-applies full replacement");
    }

    private static void testReplaceAllInvariants() {
        Controller c = new Controller("apple pie");
        c.executeInsert(9, " yummy"); // "apple pie yummy"
        c.undo();                     // Redo has " yummy"
        assertTrue(c.canRedo(), "Can redo before replace all attempt");
        int initialUndoSize = c.getUndoSize();

        // 1. Search for non-existent pattern -> no change
        int res1 = c.executeReplaceAll("banana", "orange");
        assertEquals(0, res1, "No replacements should occur");
        assertEquals("apple pie", c.getText(), "Text must remain unchanged");
        assertEquals(initialUndoSize, c.getUndoSize(), "Undo size must not increase");
        assertTrue(c.canRedo(), "Redo stack must be preserved on no-op");

        // 2. Pattern equals replacement -> no change
        int res2 = c.executeReplaceAll("apple", "apple");
        assertEquals(0, res2, "Replacing identical string gives 0");
        assertTrue(c.canRedo(), "Redo stack must still be preserved");
    }

    private static void testExecuteSortLines() {
        String unsorted = "banana\napple\norange\ngrape";
        Controller c = new Controller(unsorted);

        boolean changed = c.executeSortLines();
        assertTrue(changed, "Sort should return true when lines changed");
        assertEquals("apple\nbanana\ngrape\norange", c.getText(), "Text must be alphabetically sorted");

        // Exactly 1 Undo must restore original unsorted order
        assertTrue(c.undo(), "Undo sort should succeed");
        assertEquals(unsorted, c.getText(), "Single Undo must restore exact unsorted order");

        // Exactly 1 Redo must re-sort
        assertTrue(c.redo(), "Redo sort should succeed");
        assertEquals("apple\nbanana\ngrape\norange", c.getText(), "Single Redo must re-apply sort");
    }

    private static void testSortLinesInvariants() {
        String sorted = "apple\nbanana\ngrape";
        Controller c = new Controller(sorted);
        c.executeInsert(18, "!"); // "apple\nbanana\ngrape!"
        c.undo();                 // Redo has "!"
        assertTrue(c.canRedo(), "Can redo before sort attempt");
        int initialUndoSize = c.getUndoSize();

        // Sort on already sorted text -> returns false, preserves Redo
        boolean changed = c.executeSortLines();
        assertFalse(changed, "Sort on already-sorted text should return false");
        assertEquals(sorted, c.getText(), "Text remains unchanged");
        assertEquals(initialUndoSize, c.getUndoSize(), "Undo size must not change");
        assertTrue(c.canRedo(), "Redo stack must be preserved on no-op sort");
    }

    // Test 13: Non-blocking rollback (Lab 04). A failed undo() or redo() drops the rejected Action,
    // leaves the text and the other stack unchanged, and lets the next call reach older Actions.
    // The failures are made on purpose: a bogus Action pushed into the stack by reflection (IllegalArgumentException)
    // and a DocumentBuffer whose apply() fails once with IllegalStateException.
    private static void testNonBlockingRollback() {
        try {
            // --- undo(): bogus Action on top of a valid one ---
            Controller c = new Controller("Initial Text");
            c.executeInsert(12, " [Step 1]");
            stackOf(c, "undoStack").push(new Action(Action.Type.REPLACE, 0, "NON_EXISTENT_CONTENT", "NEW"));
            assertEquals(2, c.getUndoSize(), "Bogus Action is on top of the valid one");
            assertFalse(c.undo(), "Undo of the bogus Action must fail");
            assertEquals("Initial Text [Step 1]", c.getText(), "Text must not change on a failed undo");
            assertEquals(0, c.getRedoSize(), "Nothing may be pushed to redoStack on failure");
            assertEquals(1, c.getUndoSize(), "The rejected Action is dropped");
            assertTrue(c.undo(), "The next undo reaches the older, valid Action");
            assertEquals("Initial Text", c.getText(), "Older Action undone");

            // --- redo(): bogus Action on top of the redo stack ---
            Controller r = new Controller("abc");
            r.executeInsert(3, "d");
            assertTrue(r.undo(), "Normal undo");                 // redoStack holds the valid Action
            stackOf(r, "redoStack").push(new Action(Action.Type.DELETE, 0, "NOPE", ""));
            assertEquals(2, r.getRedoSize(), "Bogus Action is on top of the redo stack");
            assertFalse(r.redo(), "Redo of the bogus Action must fail");
            assertEquals("abc", r.getText(), "Text must not change on a failed redo");
            assertEquals(0, r.getUndoSize(), "Nothing may be pushed to undoStack on failure");
            assertEquals(1, r.getRedoSize(), "The rejected Action is dropped");
            assertTrue(r.redo(), "The next redo reaches the older, valid Action");
            assertEquals("abcd", r.getText(), "Older redo Action applied");

            // --- IllegalStateException from apply(): same rule, for undo and redo ---
            Field fb = Controller.class.getDeclaredField("buffer");
            fb.setAccessible(true);
            Controller s = new Controller("abc");
            s.executeInsert(3, "d");                             // "abcd"
            s.executeInsert(4, "e");                             // "abcde"
            fb.set(s, new FlakyBuffer("abcde", 1));              // the first apply() fails
            assertFalse(s.undo(), "Undo must fail when apply() throws IllegalStateException");
            assertEquals(1, s.getUndoSize(), "The rejected Action is dropped (IllegalStateException)");
            assertEquals(0, s.getRedoSize(), "Nothing may be pushed to redoStack (IllegalStateException)");
            assertTrue(s.undo(), "The next undo works on the older Action");

            Controller t = new Controller("abc");
            t.executeInsert(3, "d");
            t.executeInsert(4, "e");
            assertTrue(t.undo() && t.undo(), "Two normal undos");  // redoStack holds two Actions
            fb.set(t, new FlakyBuffer("abc", 1));
            assertFalse(t.redo(), "Redo must fail when apply() throws IllegalStateException");
            assertEquals(1, t.getRedoSize(), "The rejected Action is dropped (IllegalStateException)");
            assertEquals(0, t.getUndoSize(), "Nothing may be pushed to undoStack (IllegalStateException)");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Reflection problem: " + e);
        }

        // Normal undo and redo still work
        Controller n = new Controller("Hello World");
        n.executeInsert(11, "!");
        assertTrue(n.undo(), "Normal undo works");
        assertTrue(n.redo(), "Normal redo works");
        assertEquals("Hello World!", n.getText(), "Text after normal undo and redo");
    }

    /** A DocumentBuffer whose first failCount apply() calls throw IllegalStateException. */
    private static class FlakyBuffer extends DocumentBuffer {
        private int failuresLeft;
        FlakyBuffer(String text, int failCount) { super(text); this.failuresLeft = failCount; }
        @Override public void apply(Action action) {
            if (failuresLeft > 0) { failuresLeft--; throw new IllegalStateException("forced"); }
            super.apply(action);
        }
    }

    @SuppressWarnings("unchecked")
    private static HistoryStack<Action> stackOf(Controller c, String field) throws ReflectiveOperationException {
        Field f = Controller.class.getDeclaredField(field);
        f.setAccessible(true);
        return (HistoryStack<Action>) f.get(c);
    }

    private static DocumentBuffer bufferOf(Controller c) throws ReflectiveOperationException {
        Field f = Controller.class.getDeclaredField("buffer");
        f.setAccessible(true);
        return (DocumentBuffer) f.get(c);
    }

    // Test 14 (stronger): the model keeps its own history of snapshots, so Undo and Redo are
    // checked against the model, and Replace All and Sort Lines take part in the random sequence.
    private static void testStressRandomOperations() {
        Controller c = new Controller();
        String model = "";
        Deque<String> undoModel = new ArrayDeque<String>();
        Deque<String> redoModel = new ArrayDeque<String>();
        Random rng = new Random(42);
        String[] words = {"pear", "apple", "fig", "kiwi", "plum"};

        for (int i = 0; i < 500; i++) {
            int op = rng.nextInt(7);
            if (op == 0) { // Insert
                int pos = rng.nextInt(model.length() + 1);
                String s = words[rng.nextInt(words.length)] + (rng.nextBoolean() ? "\n" : " ");
                c.executeInsert(pos, s);
                undoModel.push(model);
                redoModel.clear();
                model = model.substring(0, pos) + s + model.substring(pos);
            } else if (op == 1 && model.length() > 3) { // Delete
                int pos = rng.nextInt(model.length() - 2);
                int len = 1 + rng.nextInt(Math.min(4, model.length() - pos));
                c.executeDelete(pos, len);
                undoModel.push(model);
                redoModel.clear();
                model = model.substring(0, pos) + model.substring(pos + len);
            } else if (op == 2 && model.length() > 3) { // Replace
                int pos = rng.nextInt(model.length() - 2);
                int len = 1 + rng.nextInt(Math.min(3, model.length() - pos));
                String s = words[rng.nextInt(words.length)];
                boolean changed = !model.substring(pos, pos + len).equals(s);
                assertEquals(changed, c.executeReplace(pos, len, s), "Replace reports a change only when the text changes at step " + i);
                if (changed) {
                    undoModel.push(model);
                    redoModel.clear();
                    model = model.substring(0, pos) + s + model.substring(pos + len);
                }
            } else if (op == 3) { // Replace All (may be a no-op)
                String pat = words[rng.nextInt(words.length)];
                String rep = words[rng.nextInt(words.length)];
                String expected = model.replace(pat, rep);   // non-overlapping, left to right
                int count = c.executeReplaceAll(pat, rep);
                if (!expected.equals(model)) {
                    assertTrue(count > 0, "Replace All must report a change at step " + i);
                    undoModel.push(model);
                    redoModel.clear();
                    model = expected;
                } else {
                    assertEquals(0, count, "No-op Replace All must return 0 at step " + i);
                }
            } else if (op == 4) { // Sort Lines (may be a no-op)
                String sorted = LineSorter.sortLines(model);
                boolean changed = c.executeSortLines();
                if (!sorted.equals(model)) {
                    assertTrue(changed, "Sort must report a change at step " + i);
                    undoModel.push(model);
                    redoModel.clear();
                    model = sorted;
                } else {
                    assertFalse(changed, "No-op sort must return false at step " + i);
                }
            } else if (op == 5) { // Undo
                boolean ok = c.undo();
                if (undoModel.isEmpty()) {
                    assertFalse(ok, "Undo on empty history must return false");
                } else {
                    assertTrue(ok, "Undo must succeed at step " + i);
                    redoModel.push(model);
                    model = undoModel.pop();
                }
            } else if (op == 6) { // Redo
                boolean ok = c.redo();
                if (redoModel.isEmpty()) {
                    assertFalse(ok, "Redo on empty history must return false");
                } else {
                    assertTrue(ok, "Redo must succeed at step " + i);
                    undoModel.push(model);
                    model = redoModel.pop();
                }
            }
            assertEquals(model, c.getText(), "Model vs Controller text at step " + i);
            assertEquals(undoModel.size(), c.getUndoSize(), "Undo size at step " + i);
            assertEquals(redoModel.size(), c.getRedoSize(), "Redo size at step " + i);
        }
    }

    private static void testReplaceAllEdgeCases() {
        // Overlapping matches: 'aa' in 'aaaa' gives 2 replacements, not 3
        Controller a = new Controller("aaaa");
        assertEquals(2, a.executeReplaceAll("aa", "b"), "Non-overlapping count");
        assertEquals("bb", a.getText(), "Text after non-overlapping replace");

        // Replacement contains the pattern: it must not be searched again
        Controller b = new Controller("cat cat");
        assertEquals(2, b.executeReplaceAll("cat", "cats"), "Count with growing replacement");
        assertEquals("cats cats", b.getText(), "Text with growing replacement");
        assertTrue(b.undo(), "Undo works");
        assertEquals("cat cat", b.getText(), "Undo restores the text");

        // Empty replacement deletes the matches
        Controller d = new Controller("a-b-c");
        assertEquals(2, d.executeReplaceAll("-", ""), "Count with empty replacement");
        assertEquals("abc", d.getText(), "Matches removed");

        // Empty text, null replacement, bad patterns
        assertEquals(0, new Controller().executeReplaceAll("x", "y"), "Empty text gives 0");
        Controller e = new Controller("x");
        assertEquals(1, e.executeReplaceAll("x", null), "null replacement is treated as empty");
        assertEquals("", e.getText(), "Text is empty after replacing with null");
        boolean emptyThrows = false;
        try { e.executeReplaceAll("", "y"); } catch (IllegalArgumentException ex) { emptyThrows = true; }
        assertTrue(emptyThrows, "Empty pattern must throw IllegalArgumentException");
        boolean nullThrows = false;
        try { e.executeReplaceAll(null, "y"); } catch (IllegalArgumentException ex) { nullThrows = true; }
        assertTrue(nullThrows, "Null pattern must throw IllegalArgumentException");

        // A no-op must leave the history exactly as it was
        Controller f = new Controller("one two");
        f.executeInsert(7, "!");
        int undoBefore = f.getUndoSize();
        assertEquals(0, f.executeReplaceAll("zzz", "y"), "Pattern absent");
        assertEquals(undoBefore, f.getUndoSize(), "Undo size unchanged by a no-op");
    }

    private static void testSortLinesEdgeCases() {
        assertFalse(new Controller().executeSortLines(), "Empty text: nothing to sort");

        Controller c = new Controller("b\r\na\r\n");
        assertTrue(c.executeSortLines(), "CRLF text is sorted");
        assertEquals("a\r\nb\r\n", c.getText(), "CRLF is kept after sorting");
        assertTrue(c.undo(), "Undo works");
        assertEquals("b\r\na\r\n", c.getText(), "Undo restores CRLF text");

        // Sorting twice: the second call is a no-op and must not touch the history
        Controller d = new Controller("z\ny\nx");
        assertTrue(d.executeSortLines(), "First sort changes the text");
        int undoSize = d.getUndoSize();
        assertFalse(d.executeSortLines(), "Second sort is a no-op");
        assertEquals(undoSize, d.getUndoSize(), "No new Action for a no-op sort");

        Controller mixed = new Controller("a\r\nb\nc");
        mixed.executeInsert(mixed.getLength(), "!");
        mixed.undo();
        int mixedUndo = mixed.getUndoSize();
        int mixedRedo = mixed.getRedoSize();
        assertFalse(mixed.executeSortLines(), "Sorted mixed line endings are a no-op");
        assertEquals("a\r\nb\nc", mixed.getText(), "Mixed line endings remain unchanged");
        assertEquals(mixedUndo, mixed.getUndoSize(), "No Undo step for sorted mixed line endings");
        assertEquals(mixedRedo, mixed.getRedoSize(), "Redo remains available after no-op sort");

        Controller unsortedMixed = new Controller("b\na\r\nc");
        assertTrue(unsortedMixed.executeSortLines(), "Mixed line endings can be sorted");
        assertEquals("a\nb\r\nc", unsortedMixed.getText(), "Sorting preserves separator positions");
        assertTrue(unsortedMixed.undo(), "Undo mixed line ending sort");
        assertEquals("b\na\r\nc", unsortedMixed.getText(), "Undo restores exact original separators");
        assertTrue(unsortedMixed.redo(), "Redo mixed line ending sort");
        assertEquals("a\nb\r\nc", unsortedMixed.getText(), "Redo restores sorted text");
    }

    // ==========================================
    // LAB 04 TESTS: compact Actions and bounded history
    // ==========================================

    private static void testCompactActionReplaceAll() {
        // Build a document with identical large header and footer, and a small middle target
        String header = "PRE_FIX_HEADER_VERY_LONG_DOCUMENT_SECTION_0123456789_";
        String footer = "_POST_FIX_FOOTER_VERY_LONG_DOCUMENT_SECTION_9876543210";
        String initial = header + "ABC_OLD_DATA" + footer;

        Controller c = new Controller(initial);
        int count = c.executeReplaceAll("ABC_OLD_DATA", "XYZ_NEW_CONTENT");
        assertEquals(1, count, "One replacement made");
        assertEquals(header + "XYZ_NEW_CONTENT" + footer, c.getText(), "Buffer updated");

        // Inspect the recorded Action in undoStack
        Action compactAction = c.peekUndo();
        assertTrue(compactAction != null, "Undo stack has recorded action");
        assertEquals(Action.Type.REPLACE, compactAction.getType(), "Action is REPLACE");
        assertEquals(header.length(), compactAction.getPosition(), "Action position starts after prefix");
        assertEquals("ABC_OLD_DATA", compactAction.getOldText(), "oldText only contains middle word");
        assertEquals("XYZ_NEW_CONTENT", compactAction.getNewText(), "newText only contains replacement");

        // Verify stored characters: 12 + 15 = 27 chars, NOT 2 * initial.length() (> 200 chars)
        int storedChars = ActionDiff.storedChars(compactAction);
        assertEquals(27, storedChars, "storedChars is minimal");
        assertTrue(storedChars < initial.length(), "Stored characters far less than full text");

        // Verify 1-step Undo and 1-step Redo accuracy
        assertTrue(c.undo(), "1-step Undo succeeds");
        assertEquals(initial, c.getText(), "Exact initial text restored");

        assertTrue(c.redo(), "1-step Redo succeeds");
        assertEquals(header + "XYZ_NEW_CONTENT" + footer, c.getText(), "Exact updated text restored");
    }

    private static void testCompactActionSortLines() {
        // Unsorted lines with common prefix
        String doc = "pfx_zebra\n" +
                     "pfx_charlie\n" +
                     "pfx_bravo";

        Controller c = new Controller(doc);
        boolean changed = c.executeSortLines();
        assertTrue(changed, "Sort changed order");

        String expected = "pfx_bravo\n" +
                          "pfx_charlie\n" +
                          "pfx_zebra";

        assertEquals(expected, c.getText(), "Sorted text matches");

        // Inspect recorded compact action
        Action compactAction = c.peekUndo();
        assertTrue(compactAction != null, "Compact action created for sort");

        // Position should skip the common prefix "pfx_" (length 4)
        assertEquals(4, compactAction.getPosition(), "Prefix 'pfx_' was stripped");
        assertTrue(ActionDiff.storedChars(compactAction) < doc.length() * 2, "Saved memory via compact");

        assertTrue(c.undo(), "Undo sort");
        assertEquals(doc, c.getText(), "Document restored to initial unsorted state");
    }

    private static void testBoundedCircularStackEviction() {
        // Initialize bounded controller with capacity = 3
        Controller c = new Controller(3);
        assertTrue(c.isBounded(), "Controller must report bounded");
        assertEquals(3, c.getMaxHistory(), "Capacity must be 3");

        // Execute 5 edits: E1, E2, E3, E4, E5
        c.executeInsert(0, "A"); // E1
        c.executeInsert(1, "B"); // E2
        c.executeInsert(2, "C"); // E3
        assertEquals(3, c.getUndoSize(), "Stack size is 3 (full)");

        c.executeInsert(3, "D"); // E4 -> E1 discarded from bottom
        assertEquals(3, c.getUndoSize(), "Stack size remains capped at 3");

        c.executeInsert(4, "E"); // E5 -> E2 discarded from bottom
        assertEquals(3, c.getUndoSize(), "Stack size remains capped at 3");
        assertEquals("ABCDE", c.getText(), "Current text is ABCDE");

        // Undo 1: Reverts E5 -> text is ABCD
        assertTrue(c.undo(), "Undo 1 (E5)");
        assertEquals("ABCD", c.getText(), "Text after undo 1");

        // Undo 2: Reverts E4 -> text is ABC
        assertTrue(c.undo(), "Undo 2 (E4)");
        assertEquals("ABC", c.getText(), "Text after undo 2");

        // Undo 3: Reverts E3 -> text is AB
        assertTrue(c.undo(), "Undo 3 (E3)");
        assertEquals("AB", c.getText(), "Text after undo 3");

        // Undo 4: Stack is now empty because E1 and E2 were evicted!
        assertFalse(c.canUndo(), "Cannot undo further; oldest elements were discarded");
        assertFalse(c.undo(), "Undo 4 returns false");
        assertEquals("AB", c.getText(), "Text remains AB");
    }

    private static void testBoundedCircularStackRedo() {
        Controller c = new Controller(3);
        c.executeInsert(0, "A");
        c.executeInsert(1, "B");
        c.executeInsert(2, "C");

        // Undo all 3
        c.undo(); // reverts C
        c.undo(); // reverts B
        c.undo(); // reverts A
        assertEquals("", c.getText(), "Undone to empty");
        assertEquals(3, c.getRedoSize(), "Redo stack has 3 items");

        // Redo all 3
        assertTrue(c.redo(), "Redo 1 (A)");
        assertEquals("A", c.getText(), "Text A");
        assertTrue(c.redo(), "Redo 2 (B)");
        assertEquals("AB", c.getText(), "Text AB");
        assertTrue(c.redo(), "Redo 3 (C)");
        assertEquals("ABC", c.getText(), "Text ABC");

        assertFalse(c.canRedo(), "Redo exhausted");
        assertFalse(c.redo(), "Redo 4 returns false");
    }

    private static void testStressRandomOperationsBounded() {
        int maxHist = 20;
        Controller c = new Controller(maxHist);
        StringBuilder groundTruth = new StringBuilder();
        Random rng = new Random(12345);

        for (int step = 0; step < 500; step++) {
            int op = rng.nextInt(6);
            if (op == 0 || op == 1) { // Insert
                int pos = groundTruth.length() == 0 ? 0 : rng.nextInt(groundTruth.length() + 1);
                String str = "x" + rng.nextInt(100);
                groundTruth.insert(pos, str);
                c.executeInsert(pos, str);
            } else if (op == 2 && groundTruth.length() > 3) { // Delete
                int pos = rng.nextInt(groundTruth.length() - 2);
                int len = 1 + rng.nextInt(2);
                groundTruth.delete(pos, pos + len);
                c.executeDelete(pos, len);
            } else if (op == 3 && groundTruth.length() > 3) { // Replace
                int pos = rng.nextInt(groundTruth.length() - 2);
                int len = 1 + rng.nextInt(2);
                String rep = "y" + rng.nextInt(50);
                groundTruth.replace(pos, pos + len, rep);
                c.executeReplace(pos, len, rep);
            } else if (op == 4 && c.canUndo()) { // Undo round-trip test
                String textBefore = c.getText();
                assertTrue(c.undo(), "Stress undo succeeds");
                assertTrue(c.redo(), "Stress redo succeeds");
                assertEquals(textBefore, c.getText(), "Undo/Redo roundtrip invariant");
            }
            // Invariant: Controller text matches groundTruth after non-undo steps
            assertEquals(groundTruth.toString(), c.getText(), "Controller vs GroundTruth at step " + step);
            assertTrue(c.getUndoSize() <= maxHist, "Undo size never exceeds maxHistory");
        }
    }

    // Test 22: a bounded Controller (limit 1 to 5) against a model that keeps its own list of
    // old texts. Checks undo/redo results, both stack sizes and the text after every step,
    // with Replace All and Sort Lines (compact Actions) in the random sequence.
    private static void testBoundedModelWithBulkEdits() {
        Random r = new Random(2026);
        String[] words = {"ab", "b", "a", "ba", "abab", ""};
        for (int round = 0; round < 1000; round++) {
            int limit = 1 + r.nextInt(5);
            Controller c = new Controller("", limit);
            List<String> undoStates = new ArrayList<String>();
            List<String> redoStates = new ArrayList<String>();
            String cur = "";
            for (int step = 0; step < 40; step++) {
                String next = null;                            // null = this step is not an edit
                int op = r.nextInt(7);
                if (op == 0 || op == 1) {
                    int pos = r.nextInt(cur.length() + 1);
                    String t = words[r.nextInt(5)] + (r.nextBoolean() ? "\n" : "");
                    if (!t.isEmpty()) {
                        c.executeInsert(pos, t);
                        next = cur.substring(0, pos) + t + cur.substring(pos);
                    }
                } else if (op == 2 && !cur.isEmpty()) {
                    int pos = r.nextInt(cur.length());
                    int len = 1 + r.nextInt(Math.min(3, cur.length() - pos));
                    c.executeDelete(pos, len);
                    next = cur.substring(0, pos) + cur.substring(pos + len);
                } else if (op == 3) {
                    c.executeReplaceAll("ab", "X");
                    next = cur.replace("ab", "X");
                } else if (op == 4) {
                    c.executeSortLines();
                    next = LineSorter.sortLines(cur);
                } else if (op == 5) {
                    boolean ok = c.undo();
                    assertEquals(!undoStates.isEmpty(), ok, "undo result");
                    if (ok) { redoStates.add(cur); cur = undoStates.remove(undoStates.size() - 1); }
                } else if (op == 6) {
                    boolean ok = c.redo();
                    assertEquals(!redoStates.isEmpty(), ok, "redo result");
                    if (ok) { undoStates.add(cur); cur = redoStates.remove(redoStates.size() - 1); }
                }
                if (next != null && !next.equals(cur)) {       // a real edit: one Action, redo cleared
                    undoStates.add(cur);
                    if (undoStates.size() > limit) undoStates.remove(0);
                    redoStates.clear();
                    cur = next;
                }
                assertEquals(cur, c.getText(), "text, round " + round + " step " + step);
                assertEquals(undoStates.size(), c.getUndoSize(), "undo size, round " + round + " step " + step);
                assertEquals(redoStates.size(), c.getRedoSize(), "redo size, round " + round + " step " + step);
            }
        }
    }
}
