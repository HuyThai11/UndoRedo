/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Nguyen Van Thang (SE201550), Person 1 - Core Model and Searching
 */
package test;

import model.algorithm.TextSearch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Tests for TextSearch (naive and KMP).
 * 11 fixed cases + 1 random cross-check (30,000 cases) = 12 checks.
 * Run this class: every line prints PASS or FAIL, then a summary line.
 */
public class SearchTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testOverlaps();
        testNoMatch();
        testPatternLongerThanText();
        testEmptyText();
        testMatchAtStartAndEnd();
        testWholeText();
        testPrefixTableAbabc();
        testPrefixTableAaaa();
        testEmptyPattern();
        testNullPattern();
        testNullText();
        testRandomAgainstIndexOf();

        System.out.println();
        System.out.println("Passed: " + passed + "  Failed: " + failed);
        System.out.println(failed == 0 ? "ALL SEARCH TESTS PASSED." : "SOME SEARCH TESTS FAILED.");
    }

    private static List<Integer> list(Integer... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    // Both algorithms must give the same answer for a fixed input.
    private static boolean both(String text, String pattern, List<Integer> expected) {
        return expected.equals(TextSearch.findAllNaive(text, pattern))
                && expected.equals(TextSearch.findAllKmp(text, pattern));
    }

    private static void testOverlaps() {
        report("Overlapping matches: 'aa' in 'aaaa' gives [0, 1, 2]", both("aaaa", "aa", list(0, 1, 2)));
    }

    private static void testNoMatch() {
        report("No match gives an empty list", both("abcdef", "xyz", list()));
    }

    private static void testPatternLongerThanText() {
        report("Pattern longer than the text gives an empty list", both("ab", "abc", list()));
    }

    private static void testEmptyText() {
        report("Empty text gives an empty list", both("", "a", list()));
    }

    private static void testMatchAtStartAndEnd() {
        report("Matches at the start and at the end of the text", both("abXXab", "ab", list(0, 4)));
    }

    private static void testWholeText() {
        report("Pattern equal to the whole text gives [0]", both("hello", "hello", list(0)));
    }

    private static void testPrefixTableAbabc() {
        report("Prefix table of 'ababc' is [0, 0, 1, 2, 0]",
                Arrays.equals(new int[]{0, 0, 1, 2, 0}, TextSearch.prefixTable("ababc")));
    }

    private static void testPrefixTableAaaa() {
        report("Prefix table of 'aaaa' is [0, 1, 2, 3]",
                Arrays.equals(new int[]{0, 1, 2, 3}, TextSearch.prefixTable("aaaa")));
    }

    private static void testEmptyPattern() {
        report("Empty pattern throws IllegalArgumentException (naive and KMP)",
                throwsIllegalArgument("abc", "", true) && throwsIllegalArgument("abc", "", false));
    }

    private static void testNullPattern() {
        report("Null pattern throws IllegalArgumentException (naive and KMP)",
                throwsIllegalArgument("abc", null, true) && throwsIllegalArgument("abc", null, false));
    }

    private static void testNullText() {
        report("Null text throws IllegalArgumentException (naive and KMP)",
                throwsIllegalArgument(null, "a", true) && throwsIllegalArgument(null, "a", false));
    }

    private static boolean throwsIllegalArgument(String text, String pattern, boolean naive) {
        try {
            if (naive) {
                TextSearch.findAllNaive(text, pattern);
            } else {
                TextSearch.findAllKmp(text, pattern);
            }
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    // 30,000 random cases on 2 and 3 letter alphabets (many overlaps).
    // The reference is an independent loop with String.indexOf.
    private static void testRandomAgainstIndexOf() {
        Random random = new Random(2026);
        int mismatches = 0;
        for (int round = 0; round < 30000; round++) {
            int alphabet = 2 + random.nextInt(2);
            String text = randomString(random, random.nextInt(30), alphabet);
            String pattern = randomString(random, 1 + random.nextInt(5), alphabet);

            List<Integer> expected = new ArrayList<>();
            int from = text.indexOf(pattern);
            while (from >= 0) {
                expected.add(from);
                from = text.indexOf(pattern, from + 1);   // from + 1: overlaps count
            }
            if (!both(text, pattern, expected)) {
                mismatches++;
            }
        }
        report("30,000 random cases: naive and KMP equal the String.indexOf loop (mismatches: " + mismatches + ")",
                mismatches == 0);
    }

    private static String randomString(Random random, int length, int alphabet) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(alphabet)));
        }
        return sb.toString();
    }

    private static void report(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }
}
