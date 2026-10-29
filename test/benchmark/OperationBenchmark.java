/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Hoang Anh Thu (SE204849), Person 4 - CLI and Test Suite
 */
package benchmark;

import controller.Controller;
import java.util.Arrays;

/**
 * Real time of the four basic operations for n = 10, 100, 1,000 and 10,000 steps.
 *
 *   INSERT : n inserts of "word " at the end of the text
 *   DELETE : n deletes of the last 5 characters (the text gets shorter)
 *   UNDO   : n undos (after n inserts)
 *   REDO   : n redos (after the n undos)
 *
 * For each n: 5 warm-up runs, then the median of 7 runs. The table shows the total time
 * and the time of ONE operation. If one operation is O(1), the time per operation stays about
 * the same when n grows 10 times; if it is O(n), it also grows about 10 times.
 */
public class OperationBenchmark {

    private static final int[] SIZES = {10, 100, 1000, 10000};
    private static final String WORD = "word ";
    private static final int WARMUP = 5;
    private static final int ROUNDS = 7;

    public static void main(String[] args) {
        // let the JIT compile the code before the first measured row
        for (int w = 0; w < 30; w++) {
            for (int k = 0; k < 4; k++) {
                run(k, 2000);
            }
        }
        String[] names = {"INSERT", "DELETE", "UNDO", "REDO"};
        System.out.println("Time of n operations (median of " + ROUNDS + " runs)");
        System.out.printf("%-8s %-10s %-14s %-16s%n", "Op", "n", "total (us)", "per op (us)");
        for (int k = 0; k < names.length; k++) {
            for (int n : SIZES) {
                for (int w = 0; w < WARMUP; w++) {
                    run(k, n);
                }
                double[] t = new double[ROUNDS];
                for (int r = 0; r < ROUNDS; r++) {
                    t[r] = run(k, n);
                }
                Arrays.sort(t);
                double us = t[ROUNDS / 2] / 1e3;
                System.out.printf("%-8s %-10d %-14.1f %-16.3f%n", names[k], n, us, us / n);
            }
        }
        System.out.println("Java " + System.getProperty("java.version") + " on " + System.getProperty("os.name"));
    }

    // Returns the time in nanoseconds of ONLY the measured operation.
    private static long run(int kind, int n) {
        Controller c = new Controller();
        if (kind == 0) {                                   // INSERT
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                c.executeInsert(c.getLength(), WORD);
            }
            return System.nanoTime() - t0;
        }
        for (int i = 0; i < n; i++) {                      // prepare n inserts
            c.executeInsert(c.getLength(), WORD);
        }
        if (kind == 1) {                                   // DELETE
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                c.executeDelete(c.getLength() - WORD.length(), WORD.length());
            }
            return System.nanoTime() - t0;
        }
        if (kind == 2) {                                   // UNDO
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                c.undo();
            }
            return System.nanoTime() - t0;
        }
        for (int i = 0; i < n; i++) {                      // REDO: prepare n undos
            c.undo();
        }
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            c.redo();
        }
        return System.nanoTime() - t0;
    }
}
