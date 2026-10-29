/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Nguyen Van Thang (SE201550), Person 1 - Core Model and Searching
 */
package benchmark;

import model.algorithm.TextSearch;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Compares naive search and KMP.
 * Case A: random lowercase text, pattern of 8 letters (typical text).
 * Case B: worst case for naive: text aaa...a and pattern aaa...ab (m = 200).
 * Method: 3 warm-up runs, then the median of 5 runs, System.nanoTime().
 * The times depend on the computer: run it again on the submission machine.
 */
public class SearchBenchmark {

    private static final int[] SIZES = {10000, 100000, 1000000};
    private static final int WARM_UP = 3;
    private static final int RUNS = 5;
    private static long sink;   // keeps the JIT from removing the work

    public static void main(String[] args) {
        System.out.println("Naive against KMP (median of " + RUNS + " runs after " + WARM_UP + " warm-up runs)");
        System.out.println();
        System.out.printf("%-20s %12s %12s %12s%n", "Case", "naive (ms)", "KMP (ms)", "naive / KMP");

        Random random = new Random(12345);
        for (int n : SIZES) {
            String text = randomText(random, n);
            String pattern = randomText(random, 8);
            row("A, n = " + format(n), text, pattern);
        }
        for (int n : SIZES) {
            row("B, n = " + format(n), repeat('a', n), repeat('a', 199) + "b");
        }
        System.out.println();
        System.out.println("(checksum " + sink + ")");
    }

    private static void row(String label, String text, String pattern) {
        double naive = median(text, pattern, true);
        double kmp = median(text, pattern, false);
        System.out.printf("%-20s %12.2f %12.2f %11.1fx%n", label, naive, kmp, naive / kmp);
    }

    private static double median(String text, String pattern, boolean naive) {
        for (int i = 0; i < WARM_UP; i++) {
            run(text, pattern, naive);
        }
        double[] times = new double[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            run(text, pattern, naive);
            times[i] = (System.nanoTime() - start) / 1e6;
        }
        Arrays.sort(times);
        return times[RUNS / 2];
    }

    private static void run(String text, String pattern, boolean naive) {
        List<Integer> result = naive ? TextSearch.findAllNaive(text, pattern) : TextSearch.findAllKmp(text, pattern);
        sink += result.size();
    }

    private static String randomText(Random random, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private static String format(int n) {
        return String.format("%,d", n);
    }
}
