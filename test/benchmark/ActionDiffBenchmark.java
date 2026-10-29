/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Nguyen Van Thang (SE201550), Person 1 - Core Model and Searching
 */
package benchmark;

import model.ActionDiff;
import model.algorithm.LineSorter;
import java.util.*;
import model.Action;
import model.DocumentBuffer;

/**
 * Lab 04 memory and time measurement for ActionDiff.compact().
 * Memory is counted in characters kept by one Action (old text + new text).
 * Java 8 keeps 2 bytes per character, so bytes = characters x 2.
 * Time depends on the machine - run this on the machine you submit from.
 */
public class ActionDiffBenchmark {

    static int sink = 0;

    public static void main(String[] args) {
        Random r = new Random(7);
        int lines = 20000;
        String[] base = new String[lines];
        for (int i = 0; i < lines; i++) {
            base[i] = randomLetters(r, 49);
        }
        String original = join(base, -1, -1);
        System.out.println("Text: " + lines + " lines, " + original.length() + " characters.");
        System.out.println("Full Action = whole old text + whole new text (what Lab 03 stored).");
        System.out.println();
        System.out.printf("%-46s %12s %12s %9s %10s%n", "Scenario", "full chars", "compact", "saved", "time (ms)");

        // A: Replace All with exactly one match, in the middle
        String[] a = base.clone();
        a[10000] = a[10000].substring(0, 20) + "needle" + a[10000].substring(26);
        String textA = join(a, -1, -1);
        report("A Replace All, 1 match in the middle", textA, textA.replace("needle", "thread"));

        // B: Replace All, a match in every 100th line (200 matches)
        String[] b = base.clone();
        for (int i = 0; i < lines; i += 100) {
            b[i] = b[i].substring(0, 20) + "needle" + b[i].substring(26);
        }
        String textB = join(b, -1, -1);
        report("B Replace All, 200 matches (1% of lines)", textB, textB.replace("needle", "thread"));

        // C: Sort Lines on a sorted text where two neighbouring lines are out of order
        String[] sorted = base.clone();
        Arrays.sort(sorted);
        String textC = join(sorted, 10000, 10001);
        report("C Sort Lines, 2 lines out of order", textC, LineSorter.sortLines(textC));

        // D: Sort Lines on random lines (worst case for compaction)
        report("D Sort Lines, random order (worst case)", original, LineSorter.sortLines(original));

        System.out.println("(ignore: " + sink + ")");
    }

    static void report(String label, String oldText, String newText) {
        Action c = ActionDiff.compact(oldText, newText);
        if (c == null) throw new IllegalStateException("no change in " + label);
        DocumentBuffer d = new DocumentBuffer(oldText);
        d.apply(c);
        if (!d.getText().equals(newText)) throw new IllegalStateException("apply failed in " + label);
        d.apply(c.inverse());
        if (!d.getText().equals(oldText)) throw new IllegalStateException("undo failed in " + label);
        long full = (long) oldText.length() + newText.length();
        long compact = ActionDiff.storedChars(c);
        double ms = median(() -> sink += ActionDiff.compact(oldText, newText).getPosition());
        System.out.printf("%-46s %12d %12d %8.1f%% %10.2f%n", label, full, compact, 100.0 * (full - compact) / full, ms);
    }

    // joins lines with '\n'; if swapA/swapB are given, those two lines swap places
    static String join(String[] lines, int swapA, int swapB) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            int k = i == swapA ? swapB : (i == swapB ? swapA : i);
            sb.append(lines[k]).append('\n');
        }
        return sb.toString();
    }

    static String randomLetters(Random r, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append((char) ('a' + r.nextInt(26)));
        return sb.toString();
    }

    static double median(Runnable task) {
        for (int i = 0; i < 3; i++) task.run();
        double[] t = new double[5];
        for (int i = 0; i < 5; i++) {
            long s = System.nanoTime();
            task.run();
            t[i] = (System.nanoTime() - s) / 1_000_000.0;
        }
        Arrays.sort(t);
        return t[2];
    }
}
