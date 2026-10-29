/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * Benchmark for repeated editing at a fixed middle position.
 */
package benchmark;

import java.util.Arrays;
import model.DocumentBuffer;

public final class BufferBenchmark {

    private static final int DOCUMENT_LENGTH = 2_000_000;
    private static final int DELETES = 10_000;
    private static volatile int sink;

    private BufferBenchmark() {
    }

    public static void main(String[] args) {
        char[] chars = new char[DOCUMENT_LENGTH];
        Arrays.fill(chars, 'x');
        String initial = new String(chars);

        for (int i = 0; i < 2; i++) {
            measureGapBuffer(initial, 100);
            measureStringBuilder(initial, 100);
        }

        double[] gapTimes = new double[3];
        double[] builderTimes = new double[3];
        for (int i = 0; i < 3; i++) {
            gapTimes[i] = measureGapBuffer(initial, DELETES);
            builderTimes[i] = measureStringBuilder(initial, DELETES);
        }
        Arrays.sort(gapTimes);
        Arrays.sort(builderTimes);
        System.out.println("Repeated DELETE at the same middle position, Java "
                + System.getProperty("java.version"));
        System.out.println("Document length: " + DOCUMENT_LENGTH + ", deletes: " + DELETES);
        System.out.printf("Gap buffer:     %.3f ms%n", gapTimes[1]);
        System.out.printf("StringBuilder:  %.3f ms%n", builderTimes[1]);
        System.out.printf("Ratio:          %.1fx%n", builderTimes[1] / gapTimes[1]);
        System.out.println("Check value: " + sink);
    }

    private static double measureGapBuffer(String initial, int deletes) {
        DocumentBuffer buffer = new DocumentBuffer(initial);
        int cursor = initial.length() / 2;
        int checksum = 0;
        long start = System.nanoTime();
        for (int i = 0; i < deletes; i++) {
            checksum += buffer.substring(cursor, cursor + 1).charAt(0);
            buffer.delete(cursor, 1);
        }
        double millis = (System.nanoTime() - start) / 1_000_000.0;
        sink += checksum;
        if (buffer.length() != initial.length() - deletes) {
            throw new IllegalStateException("Gap buffer length mismatch");
        }
        return millis;
    }

    private static double measureStringBuilder(String initial, int deletes) {
        StringBuilder buffer = new StringBuilder(initial);
        int cursor = initial.length() / 2;
        int checksum = 0;
        long start = System.nanoTime();
        for (int i = 0; i < deletes; i++) {
            checksum += buffer.substring(cursor, cursor + 1).charAt(0);
            buffer.delete(cursor, cursor + 1);
        }
        double millis = (System.nanoTime() - start) / 1_000_000.0;
        sink += checksum;
        if (buffer.length() != initial.length() - deletes) {
            throw new IllegalStateException("StringBuilder length mismatch");
        }
        return millis;
    }
}
