/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Nguyen Van Thang (SE201550), Person 1 - Core Model and Searching
 */
package model.algorithm;

import java.util.ArrayList;
import java.util.List;

public class TextSearch {
    private static void checkArguments(String text, String pattern) {
        if (text == null || pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("bad arguments");
        }
    }

    public static List<Integer> findAllNaive(String text, String pattern) {
        checkArguments(text, pattern);
        List<Integer> result = new ArrayList<>();
        int n = text.length(), m = pattern.length();
        for (int i = 0; i + m <= n; i++) {
            int j = 0;
            while (j < m && text.charAt(i + j) == pattern.charAt(j)) j++;
            if (j == m) result.add(i);
        }
        return result;
    }

    public static int[] prefixTable(String pattern) {
        int m = pattern.length();
        int[] table = new int[m];
        int len = 0;
        for (int i = 1; i < m; i++) {
            while (len > 0 && pattern.charAt(i) != pattern.charAt(len)) {
                len = table[len - 1];
            }
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
            }
            table[i] = len;
        }
        return table;
    }

    public static List<Integer> findAllKmp(String text, String pattern) {
        checkArguments(text, pattern);
        List<Integer> result = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();
        if (m > n) {
            return result;
        }
        int[] table = prefixTable(pattern);
        int matched = 0;
        for (int i = 0; i < n; i++) {
            while (matched > 0 && text.charAt(i) != pattern.charAt(matched)) {
                matched = table[matched - 1];
            }
            if (text.charAt(i) == pattern.charAt(matched)) {
                matched++;
            }
            if (matched == m) {
                result.add(i - m + 1);
                matched = table[matched - 1];
            }
        }
        return result;
    }
}
