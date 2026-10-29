/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Vo Anh Kiet (SE200764), Person 2 - History Stacks and Sorting
 */
package benchmark;

import model.history.BoundedCircularStack;
import model.history.BoundedLinkedStack;

/**
 * Benchmark for the Report 1 research question:
 *
 *   How does the data structure of a bounded Undo Stack (singly linked list
 *   vs circular array) affect the time of evicting the oldest action once
 *   MAX_HISTORY is reached?
 *
 * Both stacks have the SAME capacity and are filled to capacity before
 * timing, so every measured push has to evict the oldest item.
 *
 * Note: System.nanoTime() is affected by JVM warm-up and machine load, so
 * this is an empirical comparison, not a formal proof of complexity.
 */
public class StackBenchmark {
    private static final int MAX_HISTORY = 1000;
    private static final int PUSH_COUNT = 100_000;
    private static final int WARMUP = 10_000;
    private static final int ROUNDS = 10;

    // Stops the JIT from removing the measured loop as "unused".
    private static long sink = 0;

    public static void main(String[] args) {
        System.out.println("=== Bounded Linked List vs Circular Array Benchmark ===");
        System.out.println("MAX_HISTORY = " + MAX_HISTORY);
        System.out.println("Measured pushes after stack is full = " + PUSH_COUNT);
        System.out.println("Rounds = " + ROUNDS);
        System.out.println();

        // Discard the first (cold JVM) round for both implementations.
        measureLinkedList();
        measureCircularArray();

        long linkedTotal = 0;
        long circularTotal = 0;

        for (int round = 1; round <= ROUNDS; round++) {
            linkedTotal += measureLinkedList();
            circularTotal += measureCircularArray();
        }

        double linkedAvgMs = linkedTotal / (double) ROUNDS / 1_000_000.0;
        double circularAvgMs = circularTotal / (double) ROUNDS / 1_000_000.0;

        System.out.printf("Bounded Linked List average: %.3f ms per %d pushes%n",
                linkedAvgMs, PUSH_COUNT);
        System.out.printf("Circular Array average:      %.3f ms per %d pushes%n",
                circularAvgMs, PUSH_COUNT);
        if (circularAvgMs > 0) {
            System.out.printf("Linked List is about %.1f times slower%n",
                    linkedAvgMs / circularAvgMs);
        }

        System.out.println();
        System.out.println("Complexity of push when the stack is FULL (evict oldest):");
        System.out.println("- Bounded Linked List: O(n)  (walk to the tail)");
        System.out.println("- Circular Array:      O(1)  (overwrite the oldest slot)");
        System.out.println();
        System.out.println("The measured values depend on JVM warm-up, CPU, memory/cache,");
        System.out.println("and the machine running the test.");
        System.out.println("(check value: " + sink + ")");
    }

    private static long measureLinkedList() {
        BoundedLinkedStack<Integer> stack = new BoundedLinkedStack<>(MAX_HISTORY);

        for (int i = 0; i < MAX_HISTORY; i++) {
            stack.push(i);
        }

        for (int i = 0; i < WARMUP; i++) {
            stack.push(i);
        }

        long start = System.nanoTime();

        for (int i = 0; i < PUSH_COUNT; i++) {
            stack.push(i);
        }

        long elapsed = System.nanoTime() - start;
        sink += stack.peek();
        return elapsed;
    }

    private static long measureCircularArray() {
        BoundedCircularStack<Integer> stack = new BoundedCircularStack<>(MAX_HISTORY);

        for (int i = 0; i < MAX_HISTORY; i++) {
            stack.push(i);
        }

        for (int i = 0; i < WARMUP; i++) {
            stack.push(i);
        }

        long start = System.nanoTime();

        for (int i = 0; i < PUSH_COUNT; i++) {
            stack.push(i);
        }

        long elapsed = System.nanoTime() - start;
        sink += stack.peek();
        return elapsed;
    }
}
