/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Vo Anh Kiet (SE200764), Person 2 - History Stacks and Sorting
 */
package model.algorithm;

import java.util.ArrayList;
import java.util.List;


public final class LineSorter {

    private LineSorter() {
    }

 
    public static String sortLines(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (text.isEmpty()) {
            return text;
        }

        LineLayout layout = splitLines(text);
        if (isSorted(layout.lines)) {
            return text;
        }
        mergeSort(layout.lines);
        return joinLines(layout, text.length());
    }

    /**
     * Sorts all lines using Insertion Sort. Intended for comparison/benchmarking.
     */
    public static String sortLinesInsertion(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (text.isEmpty()) {
            return text;
        }

        LineLayout layout = splitLines(text);
        if (isSorted(layout.lines)) {
            return text;
        }
        insertionSort(layout.lines);
        return joinLines(layout, text.length());
    }

    private static LineLayout splitLines(String text) {
        List<String> lines = new ArrayList<>();
        List<String> separators = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                int end = i > start && text.charAt(i - 1) == '\r' ? i - 1 : i;
                lines.add(text.substring(start, end));
                separators.add(text.substring(end, i + 1));
                start = i + 1;
            }
        }
        if (start < text.length()) {
            lines.add(text.substring(start));
            separators.add("");
        }
        return new LineLayout(lines.toArray(new String[0]), separators.toArray(new String[0]));
    }

    private static boolean isSorted(String[] lines) {
        for (int i = 1; i < lines.length; i++) {
            if (lines[i - 1].compareTo(lines[i]) > 0) {
                return false;
            }
        }
        return true;
    }

    private static String joinLines(LineLayout layout, int originalLength) {
        StringBuilder result = new StringBuilder(originalLength);
        for (int i = 0; i < layout.lines.length; i++) {
            result.append(layout.lines[i]).append(layout.separators[i]);
        }
        return result.toString();
    }

    private static final class LineLayout {
        final String[] lines;
        final String[] separators;

        LineLayout(String[] lines, String[] separators) {
            this.lines = lines;
            this.separators = separators;
        }
    }

    private static void mergeSort(String[] lines) {
        if (lines.length <= 1) {
            return;
        }
        String[] temp = new String[lines.length];
        mergeSort(lines, temp, 0, lines.length - 1);
    }

    private static void mergeSort(String[] lines, String[] temp, int left, int right) {
        if (left >= right) {
            return;
        }

        int mid = left + (right - left) / 2;
        mergeSort(lines, temp, left, mid);
        mergeSort(lines, temp, mid + 1, right);
        merge(lines, temp, left, mid, right);
    }

    private static void merge(String[] lines, String[] temp,
                              int left, int mid, int right) {
        int i = left;
        int j = mid + 1;
        int k = left;

        while (i <= mid && j <= right) {
            // <= is essential for stability: take the left item when equal.
            if (lines[i].compareTo(lines[j]) <= 0) {
                temp[k++] = lines[i++];
            } else {
                temp[k++] = lines[j++];
            }
        }

        while (i <= mid) {
            temp[k++] = lines[i++];
        }
        while (j <= right) {
            temp[k++] = lines[j++];
        }

        for (int p = left; p <= right; p++) {
            lines[p] = temp[p];
        }
    }

    private static void insertionSort(String[] lines) {
        for (int i = 1; i < lines.length; i++) {
            String key = lines[i];
            int j = i - 1;

            // Strictly greater preserves the relative order of equal strings.
            while (j >= 0 && lines[j].compareTo(key) > 0) {
                lines[j + 1] = lines[j];
                j--;
            }
            lines[j + 1] = key;
        }
    }
}
