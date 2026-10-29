/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Vo Anh Kiet (SE200764), Person 2 - History Stacks and Sorting
 */
package test;

import controller.Controller;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lab 04: tests for the limited history of the Controller (Controller(text, maxHistory)).
 * Needs the Lab 04 Controller change (HistoryStack fields + the new constructor).
 */
public class ControllerHistoryLimitTest {

    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        // 1. limit 3, five inserts: only the newest 3 edits can be undone
        Controller c = new Controller("", 3);
        String[] parts = {"a", "b", "c", "d", "e"};
        for (int i = 0; i < parts.length; i++) c.executeInsert(c.getLength(), parts[i]);
        check("1a. five inserts, limit 3: undo size is 3, text is abcde", c.getUndoSize() == 3 && c.getText().equals("abcde"));
        boolean u1 = c.undo(), u2 = c.undo(), u3 = c.undo(), u4 = c.undo();
        check("1b. three undos work, the fourth says nothing to undo; text is ab (a and b are not undoable)",
                u1 && u2 && u3 && !u4 && c.getText().equals("ab"));
        boolean r1 = c.redo(), r2 = c.redo(), r3 = c.redo(), r4 = c.redo();
        check("1c. three redos restore abcde, the fourth fails", r1 && r2 && r3 && !r4 && c.getText().equals("abcde"));

        // 2. new edit after undo clears redo
        c.undo(); c.undo();
        c.executeInsert(0, "X");
        check("2. a new edit after two undos clears redo; undo size is 2 (one old edit + the new one)",
                !c.canRedo() && c.getUndoSize() == 2 && c.getText().equals("Xabc"));

        // 3. invalid limit
        boolean z = false, n = false;
        try { new Controller("", 0); } catch (IllegalArgumentException e) { z = true; }
        try { new Controller("", -5); } catch (IllegalArgumentException e) { n = true; }
        check("3. limit 0 and -5 throw IllegalArgumentException", z && n);

        // 4. Replace All and Sort Lines count as one history step each
        Controller d = new Controller("b\na\nb\n", 2);
        d.executeReplaceAll("b", "z");
        d.executeSortLines();
        d.executeInsert(0, "!");
        check("4a. three edits, limit 2: only the last two can be undone",
                d.undo() && d.undo() && !d.undo() && d.getText().equals("z\na\nz\n"));
        check("4b. redo brings back the sorted text and the insert",
                d.redo() && d.getText().equals("a\nz\nz\n") && d.redo() && d.getText().equals("!a\nz\nz\n"));

        // 5. unlimited constructors still work like Lab 03
        Controller u = new Controller("");
        for (int i = 0; i < 1000; i++) u.executeInsert(u.getLength(), "x");
        int undone = 0; while (u.undo()) undone++;
        check("5. unlimited history: 1000 edits, 1000 undos, empty text", undone == 1000 && u.getText().isEmpty());

        // 6. random cross-check against a model of states
        Random r = new Random(404);
        int bad = 0;
        for (int round = 0; round < 2000; round++) {
            int limit = 1 + r.nextInt(5);
            boolean limited = r.nextBoolean();
            Controller ctl = limited ? new Controller("", limit) : new Controller("");
            List<String> undoStates = new ArrayList<>(), redoStates = new ArrayList<>();
            String cur = "";
            for (int step = 0; step < 50; step++) {
                int op = r.nextInt(4);
                if (op == 0) {
                    int pos = r.nextInt(cur.length() + 1);
                    String t = r.nextBoolean() ? "a" : "bc";
                    String next = cur.substring(0, pos) + t + cur.substring(pos);
                    ctl.executeInsert(pos, t);
                    undoStates.add(cur);
                    if (limited && undoStates.size() > limit) undoStates.remove(0);
                    redoStates.clear();
                    cur = next;
                } else if (op == 1 && !cur.isEmpty()) {
                    int pos = r.nextInt(cur.length());
                    int len = 1 + r.nextInt(cur.length() - pos);
                    String next = cur.substring(0, pos) + cur.substring(pos + len);
                    ctl.executeDelete(pos, len);
                    undoStates.add(cur);
                    if (limited && undoStates.size() > limit) undoStates.remove(0);
                    redoStates.clear();
                    cur = next;
                } else if (op == 2) {
                    boolean ok = ctl.undo();
                    if (ok != !undoStates.isEmpty()) bad++;
                    if (!undoStates.isEmpty()) { redoStates.add(cur); cur = undoStates.remove(undoStates.size() - 1); }
                } else if (op == 3) {
                    boolean ok = ctl.redo();
                    if (ok != !redoStates.isEmpty()) bad++;
                    if (!redoStates.isEmpty()) { undoStates.add(cur); cur = redoStates.remove(redoStates.size() - 1); }
                }
                if (!ctl.getText().equals(cur) || ctl.getUndoSize() != undoStates.size() || ctl.getRedoSize() != redoStates.size()) bad++;
            }
        }
        check("6. 2000 random runs x 50 steps (limited and unlimited) agree with a simple model (mismatches: " + bad + ")", bad == 0);

        System.out.println();
        System.out.println("Passed: " + passed + "  Failed: " + failed);
        System.out.println(failed == 0 ? "ALL HISTORY LIMIT TESTS PASSED." : "SOME HISTORY LIMIT TESTS FAILED.");
    }

    static void check(String description, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + description);
        if (ok) passed++; else failed++;
    }
}
