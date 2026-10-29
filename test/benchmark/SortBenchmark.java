/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Vo Anh Kiet (SE200764), Person 2 - History Stacks and Sorting
 */
package benchmark;

import model.algorithm.LineSorter;
import java.util.Arrays;
import java.util.Random;

/**
 * Benchmark for Merge Sort versus Insertion Sort.
 *
 * Run with: java SortBenchmark   (takes about 30 seconds: Insertion Sort on 50,000 lines is slow)
 *
 * Fair rules: both algorithms get the same input, both are warmed up first,
 * and the median of several runs is printed. Results depend on the computer,
 * the Java version and the system load.
 */
public class SortBenchmark {

    private static final int[] SIZES = {1000, 10000, 50000};

    public static void main(String[] args) {
        // Warm-up for BOTH algorithms, so the JVM has compiled them before we measure.
        String warm = generateInput(2000, 1L);
        for (int i = 0; i < 20; i++) {
            LineSorter.sortLines(warm);
            LineSorter.sortLinesInsertion(warm);
        }

        System.out.printf("%-10s %-18s %-20s %-10s%n",
                "Lines", "Merge Sort (ms)", "Insertion Sort (ms)", "Ratio");

        for (int size : SIZES) {
            String input = generateInput(size, 12345L + size);

            int mergeRuns = 5;
            int insertionRuns = size >= 50000 ? 3 : 5;   // the big case is slow
            double[] mergeTimes = new double[mergeRuns];
            double[] insertionTimes = new double[insertionRuns];
            String mergeResult = null;
            String insertionResult = null;

            for (int k = 0; k < mergeRuns; k++) {
                long start = System.nanoTime();
                mergeResult = LineSorter.sortLines(input);
                mergeTimes[k] = (System.nanoTime() - start) / 1_000_000.0;
            }
            for (int k = 0; k < insertionRuns; k++) {
                long start = System.nanoTime();
                insertionResult = LineSorter.sortLinesInsertion(input);
                insertionTimes[k] = (System.nanoTime() - start) / 1_000_000.0;
            }

            if (!mergeResult.equals(insertionResult)) {
                throw new AssertionError("Different output at size " + size);
            }

            double merge = median(mergeTimes);
            double insertion = median(insertionTimes);
            System.out.printf("%-10d %-18.3f %-20.3f %-10.1f%n", size, merge, insertion, insertion / merge);
        }
    }

    private static double median(double[] values) {
        double[] copy = values.clone();
        Arrays.sort(copy);
        return copy[copy.length / 2];
    }

    private static String generateInput(int count, long seed) {
        Random random = new Random(seed);
        StringBuilder text = new StringBuilder(count * 12);
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append('\n');
            // Fixed-width-ish random strings make comparisons reasonably varied.
            text.append(String.format("%05d-", random.nextInt(count * 10)));
            for (int j = 0; j < 6; j++) {
                text.append((char) ('a' + random.nextInt(26)));
            }
        }
        return text.toString();
    }
}
