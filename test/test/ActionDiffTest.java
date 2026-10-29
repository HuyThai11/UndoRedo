/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Nguyen Van Thang (SE201550), Person 1 - Core Model and Searching
 */
package test;

import model.ActionDiff;
import model.algorithm.LineSorter;
import java.util.*;
import model.Action;
import model.DocumentBuffer;

/** Self-check for ActionDiff. Prints PASS/FAIL for each check. */
public class ActionDiffTest {

    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        Action a = ActionDiff.compact("one two three", "one 2 three");
        check("1. change in the middle keeps only the changed part",
                a != null && a.getType() == Action.Type.REPLACE && a.getPosition() == 4
                        && a.getOldText().equals("two") && a.getNewText().equals("2"));

        check("2. equal texts give null", ActionDiff.compact("same", "same") == null
                && ActionDiff.compact("", "") == null);

        a = ActionDiff.compact("", "abc");
        check("3. empty to text: REPLACE(0, \"\", \"abc\")",
                a.getPosition() == 0 && a.getOldText().isEmpty() && a.getNewText().equals("abc"));

        a = ActionDiff.compact("abc", "");
        check("4. text to empty: REPLACE(0, \"abc\", \"\")",
                a.getPosition() == 0 && a.getOldText().equals("abc") && a.getNewText().isEmpty());

        a = ActionDiff.compact("hello", "hello world");
        check("5. change only at the end: only the added text is kept",
                a.getPosition() == 5 && a.getOldText().isEmpty() && a.getNewText().equals(" world"));

        a = ActionDiff.compact("world", "hello world");
        check("6. change only at the start: only the added text is kept",
                a.getPosition() == 0 && a.getOldText().isEmpty() && a.getNewText().equals("hello "));

        a = ActionDiff.compact("aaa", "aaaa");
        check("7. prefix and suffix overlap: 'aaa' -> 'aaaa' adds one 'a' at 3",
                a.getPosition() == 3 && a.getOldText().isEmpty() && a.getNewText().equals("a"));

        a = ActionDiff.compact("abab", "ab");
        check("8. overlap the other way: 'abab' -> 'ab' removes 'ab' at 2",
                a.getPosition() == 2 && a.getOldText().equals("ab") && a.getNewText().isEmpty());

        check("9. null argument throws IllegalArgumentException",
                throwsIAE(() -> ActionDiff.compact(null, "x")) && throwsIAE(() -> ActionDiff.compact("x", null)));

        // random cross-check: apply, undo, and the changed part is as small as possible
        Random r = new Random(404);
        int badApply = 0, badMinimal = 0, badSize = 0;
        for (int round = 0; round < 50000; round++) {
            int alphabet = 2 + r.nextInt(2);
            String oldT = randomString(r, r.nextInt(14), alphabet);
            String newT = r.nextInt(4) == 0 ? mutate(r, oldT, alphabet) : randomString(r, r.nextInt(14), alphabet);
            Action c = ActionDiff.compact(oldT, newT);
            if (c == null) {
                if (!oldT.equals(newT)) badApply++;
                continue;
            }
            DocumentBuffer d = new DocumentBuffer(oldT);
            d.apply(c);
            boolean forward = d.getText().equals(newT);
            d.apply(c.inverse());
            boolean back = d.getText().equals(oldT);
            if (!forward || !back) badApply++;
            // maximal cut: the prefix and the suffix could not be longer
            int p = c.getPosition();
            int s2 = oldT.length() - (p + c.getOldText().length());
            int shorter = Math.min(oldT.length(), newT.length());
            boolean prefixMaximal = p == shorter || oldT.charAt(p) != newT.charAt(p);
            boolean suffixMaximal = s2 == shorter - p
                    || oldT.charAt(oldT.length() - 1 - s2) != newT.charAt(newT.length() - 1 - s2);
            if (!prefixMaximal || !suffixMaximal) badMinimal++;
            if (ActionDiff.storedChars(c) > oldT.length() + newT.length()) badSize++;
        }
        check("10. 50000 random cases: apply gives new text, inverse gives old text (failures: " + badApply + ")", badApply == 0);
        check("11. the common prefix and suffix are cut as far as possible (failures: " + badMinimal + ")", badMinimal == 0);
        check("12. a compact Action never stores more than the whole old + new text (failures: " + badSize + ")", badSize == 0);

        // Sort Lines and Replace All results, using the group's real LineSorter and String.replace
        int badEdit = 0;
        for (int round = 0; round < 3000; round++) {
            StringBuilder sb = new StringBuilder();
            int lines = r.nextInt(8);
            for (int i = 0; i < lines; i++) {
                sb.append(randomString(r, r.nextInt(4), 3)).append('\n');
            }
            String text = sb.toString();
            String sorted = LineSorter.sortLines(text);
            String replaced = text.replace("ab", "X");
            for (String target : new String[]{sorted, replaced}) {
                Action c = ActionDiff.compact(text, target);
                if (c == null) { if (!text.equals(target)) badEdit++; continue; }
                DocumentBuffer d = new DocumentBuffer(text);
                d.apply(c);
                if (!d.getText().equals(target)) badEdit++;
                d.apply(c.inverse());
                if (!d.getText().equals(text)) badEdit++;
            }
        }
        check("13. 3000 random texts: Sort Lines and Replace All results compact, apply and undo correctly (failures: " + badEdit + ")", badEdit == 0);

        // storedChars must be exactly old + new length of the compact Action (the benchmark relies on it)
        a = ActionDiff.compact("one two three", "one 2 three");
        check("14. storedChars counts old + new characters exactly (\"two\" + \"2\" = 4)",
                ActionDiff.storedChars(a) == 4
                        && ActionDiff.storedChars(ActionDiff.compact("", "abc")) == 3
                        && ActionDiff.storedChars(ActionDiff.compact("abab", "ab")) == 2);

        System.out.println();
        System.out.println("Passed: " + passed + "  Failed: " + failed);
        System.out.println(failed == 0 ? "ALL ACTIONDIFF TESTS PASSED." : "SOME ACTIONDIFF TESTS FAILED.");
    }

    static String mutate(Random r, String s, int alphabet) {
        if (s.isEmpty()) return randomString(r, 1, alphabet);
        int p = r.nextInt(s.length());
        return s.substring(0, p) + randomString(r, r.nextInt(3), alphabet) + s.substring(Math.min(s.length(), p + r.nextInt(3)));
    }

    static String randomString(Random r, int length, int alphabet) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append((char) ('a' + r.nextInt(alphabet)));
        return sb.toString();
    }

    static boolean throwsIAE(Runnable run) {
        try { run.run(); return false; } catch (IllegalArgumentException e) { return true; }
    }

    static void check(String description, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + description);
        if (ok) passed++; else failed++;
    }
}
