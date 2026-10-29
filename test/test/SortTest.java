/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Vo Anh Kiet (SE200764), Person 2 - History Stacks and Sorting
 */
package test;

import model.algorithm.LineSorter;
import java.util.Arrays;
import java.util.Random;

/**
 * Standalone tests: run with `java SortTest`.
 * No external testing library is required.
 */
public class SortTest {

    private static int passed = 0;

    public static void main(String[] args) {
        test("ordinary input",
                "banana\napple\ncherry",
                "apple\nbanana\ncherry");
        test("already sorted",
                "apple\nbanana\ncherry",
                "apple\nbanana\ncherry");
        test("reverse sorted",
                "cherry\nbanana\napple",
                "apple\nbanana\ncherry");
        test("duplicate lines",
                "pear\napple\npear\napple",
                "apple\napple\npear\npear");
        test("empty input", "", "");
        test("one line", "hello", "hello");
        test("final newline preserved",
                "banana\napple\n",
                "apple\nbanana\n");
        test("blank line is sortable",
                "b\n\na",
                "\na\nb");
        test("CRLF preserved",
                "b\r\na\r\n",
                "a\r\nb\r\n");

        // New in the fixed version: rules that were written in the report but not tested.
        test("uppercase comes before lowercase",
                "b\nB\na\nA",
                "A\nB\na\nb");
        test("numbers are compared as text",
                "9\n10\n2",
                "10\n2\n9");
        test("only a newline",
                "\n",
                "\n");
        test("last blank line is kept as the final newline",
                "a\n\n",
                "\na\n");
        test("mixed line endings retain their positions",
                "b\na\r\nc",
                "a\nb\r\nc");
        test("sorted mixed line endings remain unchanged",
                "a\r\nb\nc",
                "a\r\nb\nc");
        test("mixed final line endings remain unchanged",
                "a\r\nb\n",
                "a\r\nb\n");

        testNullThrows();
        testSortedTwice();

        // Deterministic randomized cross-check: both algorithms must agree.
        Random random = new Random(20261017L);
        for (int trial = 0; trial < 300; trial++) {
            int count = random.nextInt(40);
            StringBuilder input = new StringBuilder();
            for (int i = 0; i < count; i++) {
                if (i > 0) input.append('\n');
                int length = random.nextInt(8);
                for (int j = 0; j < length; j++) {
                    input.append((char) ('a' + random.nextInt(5)));
                }
            }
            String text = input.toString();
            String merge = LineSorter.sortLines(text);
            String insertion = LineSorter.sortLinesInsertion(text);
            if (!merge.equals(insertion)) {
                throw new AssertionError("Algorithms differ on randomized case: " + text);
            }
        }
        passed++;
        System.out.println("PASS: 300 randomized cross-checks (Merge Sort = Insertion Sort)");

        // Independent check against the library sort (used here only to test, not in LineSorter).
        Random random2 = new Random(7L);
        for (int trial = 0; trial < 1000; trial++) {
            int count = 1 + random.nextInt(60);
            String[] lines = new String[count];
            for (int i = 0; i < count; i++) {
                int length = (i == count - 1) ? 1 + random2.nextInt(5) : random2.nextInt(6);
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < length; j++) {
                    sb.append((char) ('a' + random2.nextInt(3)));
                }
                lines[i] = sb.toString();
            }
            String text = String.join("\n", lines);
            String[] copy = lines.clone();
            Arrays.sort(copy);
            String expected = String.join("\n", copy);
            if (!expected.equals(LineSorter.sortLines(text))) {
                throw new AssertionError("Differs from Arrays.sort on: " + printable(text));
            }
        }
        passed++;
        System.out.println("PASS: 1000 randomized checks against Arrays.sort");

        System.out.println("Passed: " + passed + "  Failed: 0");
        System.out.println("ALL SORT TESTS PASSED.");
    }

    private static void test(String name, String input, String expected) {
        String actual = LineSorter.sortLines(input);
        if (!expected.equals(actual)) {
            throw new AssertionError(name + " failed.\nExpected: "
                    + printable(expected) + "\nActual:   " + printable(actual));
        }
        String insertion = LineSorter.sortLinesInsertion(input);
        if (!expected.equals(insertion)) {
            throw new AssertionError(name + " failed for Insertion Sort.\nExpected: "
                    + printable(expected) + "\nActual:   " + printable(insertion));
        }
        passed++;
        System.out.println("PASS: " + name);
    }

    private static void testNullThrows() {
        try {
            LineSorter.sortLines(null);
            throw new AssertionError("null input should throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            passed++;
            System.out.println("PASS: null input rejected");
        }
    }

    // Sorting a sorted text must not change it. The Controller relies on this:
    // when nothing changes, it does not create an Action.
    private static void testSortedTwice() {
        String once = LineSorter.sortLines("d\nc\nb\na\n");
        String twice = LineSorter.sortLines(once);
        if (!once.equals(twice)) {
            throw new AssertionError("sorting a sorted text changed it");
        }
        passed++;
        System.out.println("PASS: sorting twice gives the same text");
    }

    private static String printable(String value) {
        return value.replace("\r", "\\r").replace("\n", "\\n");
    }
}
