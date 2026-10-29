/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Huynh Huy Thai (SE203059), Person 3 - Controller
 */
package benchmark;

import model.ActionDiff;
import controller.Controller;
import java.util.Arrays;
import java.util.Random;
import model.Action;

/**
 * ControllerBenchmark for Lab 04: Evaluates memory savings from ActionDiff.compact
 * and runtime performance of BoundedCircularStack vs unbounded MyStack in the Controller.
 * Implemented by Huynh Huy Thai (SE203059 - Person 3: Controller / Orchestrator).
 * Memory numbers are exact counts. Times depend on the machine: run this on the machine you submit from.
 */
public class ControllerBenchmark {

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println("     CONTROLLER BENCHMARK LAB 04 - HUYNH HUY THAI (SE203059)             ");
        System.out.println("=========================================================================");

        benchmarkMemorySavings();
        System.out.println();
        benchmarkStackLatency();
        System.out.println();
        benchmarkUndoRedoScale();

        System.out.println("=========================================================================");
        System.out.println("     BENCHMARK HOAN TAT                                                  ");
        System.out.println("=========================================================================");
    }

    /**
     * Benchmark 1: Memory footprint of Action objects (Full-text Lab 03 vs Compact Lab 04).
     * Measures stored characters and estimated heap memory on a 1,000,000-character document.
     */
    private static void benchmarkMemorySavings() {
        System.out.println("--- BENCHMARK 1: ACTION MEMORY COMPARISON (1,000,000 CHARS TEXT) ---");
        System.out.printf("%-38s %12s %12s %10s %12s%n",
                "Scenario", "Lab 03 (chars)", "Lab 04 (chars)", "Saved (%)", "Memory Diff");

        int lines = 20000;
        Random rng = new Random(42);
        String[] baseLines = new String[lines];
        for (int i = 0; i < lines; i++) {
            baseLines[i] = randomString(rng, 49); // 49 chars + '\n' = 50 chars/line -> 1,000,000 chars
        }
        String doc = String.join("\n", baseLines) + "\n";
        long fullChars = (long) doc.length() * 2; // Lab 03 stored full oldText + full newText

        // Scenario 1: Replace All, 1 match in middle line
        String pattern1 = "needle_xyz";
        String rep1 = "thread_abc";
        int midLine = lines / 2;
        baseLines[midLine] = baseLines[midLine].substring(0, 10) + pattern1 + baseLines[midLine].substring(20);
        String text1 = String.join("\n", baseLines) + "\n";
        Controller c1 = new Controller(text1);
        c1.executeReplaceAll(pattern1, rep1);
        Action a1 = c1.peekUndo();
        long compactChars1 = ActionDiff.storedChars(a1);
        printMemRow("1. Replace All (1 match in middle)", fullChars, compactChars1);

        // Scenario 2: Replace All (10 matches clustered in 1 section)
        for (int i = midLine; i < midLine + 10; i++) {
            baseLines[i] = baseLines[i].substring(0, 10) + pattern1 + baseLines[i].substring(20);
        }
        String text2 = String.join("\n", baseLines) + "\n";
        Controller c2 = new Controller(text2);
        c2.executeReplaceAll(pattern1, rep1);
        Action a2 = c2.peekUndo();
        long compactChars2 = ActionDiff.storedChars(a2);
        printMemRow("2. Replace All (10 matches clustered)", fullChars, compactChars2);

        // Scenario 3: Sort Lines where 2 adjacent lines are inverted in middle
        String[] sortedLines = baseLines.clone();
        Arrays.sort(sortedLines);
        // Swap midLine and midLine + 1
        String temp = sortedLines[midLine];
        sortedLines[midLine] = sortedLines[midLine + 1];
        sortedLines[midLine + 1] = temp;
        String text3 = String.join("\n", sortedLines) + "\n";
        Controller c3 = new Controller(text3);
        c3.executeSortLines();
        Action a3 = c3.peekUndo();
        long compactChars3 = ActionDiff.storedChars(a3);
        printMemRow("3. Sort Lines (2 lines swapped)", fullChars, compactChars3);

        // Scenario 4: Sort Lines on random lines (worst case for one-range compaction)
        String[] randomLines = new String[lines];
        for (int i = 0; i < lines; i++) randomLines[i] = randomString(rng, 49);
        Controller c4 = new Controller(String.join("\n", randomLines) + "\n");
        c4.executeSortLines();
        printMemRow("4. Sort Lines (random order)", fullChars, ActionDiff.storedChars(c4.peekUndo()));
    }

    private static void printMemRow(String label, long fullChars, long compactChars) {
        double savedPct = 100.0 * (fullChars - compactChars) / fullChars;
        long bytesFull = fullChars * 2;
        long bytesCompact = compactChars * 2;
        String diff = String.format("-%d KB", (bytesFull - bytesCompact) / 1024);
        System.out.printf("%-38s %12d %12d %9.2f%% %12s%n",
                label, fullChars, compactChars, savedPct, diff);
    }

    /**
     * Benchmark 2: bounded history (BoundedCircularStack) against unbounded history (MyStack).
     * Both Controllers get the same 50,000 inserts at the end of the text (so the cost of the
     * text buffer stays small and the stack cost is visible), then undo 1,000 steps and redo them.
     * The number of undo and redo steps is the SAME for both, so the times can be compared.
     * The important difference is how many Actions stay in memory after 50,000 edits.
     */
    private static void benchmarkStackLatency() {
        System.out.println("--- BENCHMARK 2: BOUNDED AND UNBOUNDED HISTORY (50,000 EDITS, 1,000 UNDO + 1,000 REDO) ---");
        System.out.printf("%-30s %14s %14s %14s %16s%n",
                "Configuration", "50,000 edits", "1,000 undos", "1,000 redos", "Actions in memory");

        int edits = 50000;
        int steps = 1000;
        int maxHist = 1000;
        int rounds = 7;

        double[][] bounded = new double[3][rounds];
        double[][] unbounded = new double[3][rounds];
        int boundedKept = 0, unboundedKept = 0;
        for (int round = 0; round < rounds; round++) {       // rounds 0-1 are warm-up
            Controller b = new Controller(maxHist);
            double[] tb = measureControllerCycle(b, edits, steps);
            Controller u = new Controller();
            double[] tu = measureControllerCycle(u, edits, steps);
            for (int k = 0; k < 3; k++) { bounded[k][round] = tb[k]; unbounded[k][round] = tu[k]; }
            boundedKept = b.getUndoSize() + b.getRedoSize();
            unboundedKept = u.getUndoSize() + u.getRedoSize();
        }
        System.out.printf("%-30s %14.2f %14.2f %14.2f %16d%n", "Bounded (MAX_HISTORY=1000)",
                median(bounded[0], 2), median(bounded[1], 2), median(bounded[2], 2), boundedKept);
        System.out.printf("%-30s %14.2f %14.2f %14.2f %16d%n", "Unbounded (MyStack)",
                median(unbounded[0], 2), median(unbounded[1], 2), median(unbounded[2], 2), unboundedKept);
        System.out.println("Times in ms, median of 5 runs after 2 warm-up runs.");
    }

    private static double median(double[] values, int skip) {
        double[] v = Arrays.copyOfRange(values, skip, values.length);
        Arrays.sort(v);
        return v[v.length / 2];
    }

    /** Returns {edit time, undo time, redo time} in ms. */
    private static double[] measureControllerCycle(Controller c, int edits, int steps) {
        long start = System.nanoTime();
        for (int i = 0; i < edits; i++) {
            c.executeInsert(c.getLength(), "x");
        }
        double editMs = (System.nanoTime() - start) / 1_000_000.0;

        start = System.nanoTime();
        for (int i = 0; i < steps; i++) {
            if (!c.undo()) throw new IllegalStateException("undo failed");
        }
        double undoMs = (System.nanoTime() - start) / 1_000_000.0;

        start = System.nanoTime();
        for (int i = 0; i < steps; i++) {
            if (!c.redo()) throw new IllegalStateException("redo failed");
        }
        double redoMs = (System.nanoTime() - start) / 1_000_000.0;

        return new double[]{editMs, undoMs, redoMs};
    }

    /**
     * Benchmark 3: Latency of 1-step Undo and 1-step Redo on 1,000,000-char document
     * with compact Action vs full document replacement.
     */
    private static void benchmarkUndoRedoScale() {
        System.out.println("--- BENCHMARK 3: 1-STEP UNDO/REDO LATENCY ON 1,000,000 CHARS ---");
        System.out.printf("%-35s %15s %15s%n", "Operation", "Undo Latency (ms)", "Redo Latency (ms)");

        int lines = 20000;
        Random rng = new Random(99);
        String[] baseLines = new String[lines];
        for (int i = 0; i < lines; i++) {
            baseLines[i] = randomString(rng, 49);
        }
        String text = String.join("\n", baseLines) + "\n";

        // Replace All in Controller (Compact Action)
        Controller c = new Controller(text, 100);
        c.executeReplaceAll("abc", "xyz");

        // Warmup
        for (int i = 0; i < 3; i++) {
            c.undo();
            c.redo();
        }

        double undoMedian = median(() -> {
            c.undo();
            c.redo();
        }, true, c);

        double redoMedian = median(() -> {
            c.undo();
            c.redo();
        }, false, c);

        System.out.printf("%-35s %15.3f %15.3f%n", "Compact Action (Replace All)", undoMedian, redoMedian);
    }

    private static double median(Runnable r, boolean measureUndo, Controller c) {
        double[] times = new double[5];
        for (int i = 0; i < 5; i++) {
            if (measureUndo) {
                // Currently text is replaced. Measure undo:
                long s = System.nanoTime();
                c.undo();
                times[i] = (System.nanoTime() - s) / 1_000_000.0;
                c.redo(); // restore for next iteration
            } else {
                c.undo();
                long s = System.nanoTime();
                c.redo();
                times[i] = (System.nanoTime() - s) / 1_000_000.0;
            }
        }
        Arrays.sort(times);
        return times[2];
    }

    private static String randomString(Random r, int len) {
        char[] ch = new char[len];
        for (int i = 0; i < len; i++) {
            ch[i] = (char) ('a' + r.nextInt(26));
        }
        return new String(ch);
    }
}
