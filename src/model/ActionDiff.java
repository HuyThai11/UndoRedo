/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Nguyen Van Thang (SE201550), Person 1 - Core Model and Searching
 */
package model;


public class ActionDiff {


    public static Action compact(String oldText, String newText) {
        if (oldText == null || newText == null) {
            throw new IllegalArgumentException("oldText and newText must not be null");
        }
        if (oldText.equals(newText)) {
            return null;
        }
        int oldLen = oldText.length();
        int newLen = newText.length();
        int shorter = Math.min(oldLen, newLen);

        int prefix = 0;
        while (prefix < shorter && oldText.charAt(prefix) == newText.charAt(prefix)) {
            prefix++;
        }

        // The suffix must not overlap the prefix, so it can be at most (shorter - prefix) long.
        int suffix = 0;
        int maxSuffix = shorter - prefix;
        while (suffix < maxSuffix
                && oldText.charAt(oldLen - 1 - suffix) == newText.charAt(newLen - 1 - suffix)) {
            suffix++;
        }

        String oldMiddle = oldText.substring(prefix, oldLen - suffix);
        String newMiddle = newText.substring(prefix, newLen - suffix);
        return new Action(Action.Type.REPLACE, prefix, oldMiddle, newMiddle);
    }

    /** Number of characters an Action keeps in memory (old + new text). Time O(1). */
    public static int storedChars(Action action) {
        return action.getOldText().length() + action.getNewText().length();
    }
}
