/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Hoang Anh Thu (SE204849), Person 4 - CLI and Test Suite
 */
package view;

import model.ActionDiff;
import controller.Controller;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;
import model.Action;

/**
 * Demo Interface (console / CLI) for the Undo/Redo Engine - Lab 04 version.
 * The CLI only reads a command line, calls the Controller and prints the result.
 * All editing rules stay in Controller. After EVERY command the CLI prints the
 * current text and how many steps are left in the Undo and Redo stacks
 * ("Render Current Text & Stack Counters to UI" in the Report 1 flowchart).
 *
 * Commands (upper or lower case):
 *   INSERT     position text              insert text at position
 *   DELETE     position length            delete length characters from position
 *   REPLACE    position length text       replace length characters from position with text
 *   FIND       pattern                    show every position of the pattern (does not change the text)
 *   REPLACEALL pattern replacement        replace every match; ONE Undo brings everything back
 *   SORT                                  sort the lines of the text; ONE Undo brings the old order back
 *   UNDO                                  go back one step
 *   REDO                                  go forward one step
 *   HISTORY                               (Lab 04) show the history limit, both counters and the next Undo / Redo step
 *   LIMIT      number | OFF               (Lab 04) keep only the newest N steps (OFF = no limit); the text is kept,
 *                                         the old Undo and Redo steps are cleared
 *   PRINT                                 show the text and the Undo / Redo counters
 *   EXIT                                  leave the program
 * Extra: HELP (list the commands), DEMO, DEMO2 and DEMO3 (ready-made examples).
 * Start the program with a number (java DemoCLI 50) to begin with a history limit of 50 steps.
 *
 * A line break cannot be typed in a one-line command, so the two characters \n mean a line break
 * inside any text, pattern or replacement, and \\ means one backslash.
 *
 * The CLI catches every exception, so a wrong command never crashes the program.
 */
public class DemoCLI {

    private static final int MAX_SHOWN_POSITIONS = 20;
    private static final int MAX_SHOWN_CHARS = 20;
    private static final int MAX_STATE_CHARS = 200;

    // Number of Undo steps before the current command ran (used to notice that the oldest step was dropped).
    private int undoBefore;

    private Controller controller;
    private final PrintStream out;

    public DemoCLI(Controller controller, PrintStream out) {
        this.controller = controller;
        this.out = out;
    }

    public static void main(String[] args) {
        Controller start = new Controller();
        if (args.length > 0) {
            try {
                start = new Controller(Integer.parseInt(args[0].trim()));
            } catch (IllegalArgumentException e) {
                System.out.println("Error: the history limit must be a whole number greater than 0 (you typed '"
                        + args[0] + "'). Starting without a limit.");
            }
        }
        DemoCLI cli = new DemoCLI(start, System.out);
        Scanner scanner = new Scanner(System.in);

        cli.out.println("=== Undo/Redo Engine for Text Editors (CSD201 - Group 4) ===");
        cli.printHelp();
        cli.printState();

        boolean running = true;
        while (running && scanner.hasNextLine()) {
            cli.out.print("Command: ");
            cli.out.flush();
            running = cli.handle(scanner.nextLine());
        }
        cli.out.println("Bye.");
    }

    /**
     * Runs one command line. Returns false when the user wants to leave (EXIT).
     * It never throws: every exception is caught and shown as a friendly message.
     */
    public boolean handle(String line) {
        try {
            return runCommand(line);
        } catch (RuntimeException e) {
            out.println("Error: something went wrong (" + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : ": " + e.getMessage())
                    + "). The program keeps running.");
            return true;
        }
    }

    private boolean runCommand(String line) {
        String text = line.replaceAll("^\\s+", "");     // remove spaces at the start only
        if (text.isEmpty()) {
            return true;
        }

        undoBefore = controller.getUndoSize();
        String[] parts = text.split("\\s+", 2);         // command word + the rest
        String command = parts[0].toUpperCase();
        String rest = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "INSERT":
                doInsert(rest);
                break;
            case "DELETE":
                doDelete(rest);
                break;
            case "REPLACE":
                doReplace(rest);
                break;
            case "FIND":
                doFind(rest);
                break;
            case "REPLACEALL":
                doReplaceAll(rest);
                break;
            case "SORT":
                doSort();
                break;
            case "UNDO":
                doUndo();
                break;
            case "REDO":
                doRedo();
                break;
            case "HISTORY":
                doHistory();
                break;
            case "LIMIT":
                doLimit(rest);
                break;
            case "PRINT":
            case "SHOW":
                printDocument();
                printStatus();
                break;
            case "DEMO":
                runDemo();
                break;
            case "DEMO2":
                runDemo2();
                break;
            case "DEMO3":
                runDemo3();
                break;
            case "HELP":
                printHelp();
                break;
            case "EXIT":
            case "QUIT":
                return false;
            default:
                out.println("Error: unknown command '" + parts[0] + "'. Type HELP to see the commands.");
        }
        return true;
    }

    private void doInsert(String rest) {
        String[] args = rest.split(" ", 2);              // keep the spaces inside the text
        if (rest.isEmpty() || args.length < 2) {
            out.println("Error: missing parameter. Usage: INSERT <position> <text>   e.g. INSERT 5 hello");
            return;
        }
        Integer position = toNumber(args[0], "Position");
        if (position == null) {
            return;
        }
        finishEdit(controller.executeInsert(position, unescape(args[1])));
    }

    private void doDelete(String rest) {
        String[] args = rest.trim().split("\\s+");
        if (rest.trim().isEmpty() || args.length != 2) {
            out.println("Error: missing parameter. Usage: DELETE <position> <length>   e.g. DELETE 0 3");
            return;
        }
        Integer position = toNumber(args[0], "Position");
        Integer length = toNumber(args[1], "Length");
        if (position == null || length == null) {
            return;
        }
        finishEdit(controller.executeDelete(position, length));
    }

    private void doReplace(String rest) {
        String[] args = rest.split(" ", 3);
        if (rest.isEmpty() || args.length < 3) {
            out.println("Error: missing parameter. Usage: REPLACE <position> <length> <text>   e.g. REPLACE 2 4 world");
            return;
        }
        Integer position = toNumber(args[0], "Position");
        Integer length = toNumber(args[1], "Length");
        if (position == null || length == null) {
            return;
        }
        String newText = unescape(args[2]);
        if (controller.isReplacementNoOp(position, length, newText)) {
            out.println("No change: the new text is the same as the old text. Undo and Redo steps were not touched.");
            printState();
            return;
        }
        finishEdit(controller.executeReplace(position, length, newText));
    }

    private void doFind(String rest) {
        if (rest.isEmpty()) {
            out.println("Error: missing parameter. Usage: FIND <pattern>   e.g. FIND cat");
            return;
        }
        String pattern = unescape(rest);
        List<Integer> positions = controller.find(pattern);
        if (positions.isEmpty()) {
            out.println("Not found: \"" + show(pattern) + "\"");
        } else {
            StringBuilder list = new StringBuilder();
            for (int i = 0; i < positions.size() && i < MAX_SHOWN_POSITIONS; i++) {
                if (i > 0) {
                    list.append(", ");
                }
                list.append(positions.get(i));
            }
            if (positions.size() > MAX_SHOWN_POSITIONS) {
                list.append(", ... (").append(positions.size() - MAX_SHOWN_POSITIONS).append(" more)");
            }
            out.println("Found " + positions.size() + " match(es) of \"" + show(pattern)
                    + "\" at position(s): " + list);
        }
        printState();
    }

    // REPLACEALL <pattern> <replacement>   (the pattern is one word; the replacement is the rest of the line)
    private void doReplaceAll(String rest) {
        String[] args = rest.split(" ", 2);
        if (rest.isEmpty() || args.length < 2 || args[1].isEmpty()) {
            out.println("Error: missing parameter. Usage: REPLACEALL <pattern> <replacement>   e.g. REPLACEALL cat dog");
            return;
        }
        String pattern = unescape(args[0]);
        String replacement = unescape(args[1]);
        int count = controller.executeReplaceAll(pattern, replacement);
        if (count > 0) {
            out.println("Replaced " + count + " match(es) of \"" + show(pattern) + "\" with \""
                    + show(replacement) + "\". One UNDO brings them all back.");
            noteIfOldestDropped(true);
        } else {
            out.println("No change: the pattern was not found, or the replacement is the same as the pattern. "
                    + "Undo and Redo steps were not touched.");
        }
        printState();
    }

    private void doSort() {
        if (controller.executeSortLines()) {
            out.println("Lines sorted. One UNDO brings the old order back.");
            noteIfOldestDropped(true);
        } else {
            out.println("No change: the text is empty or the lines are already in order. "
                    + "Undo and Redo steps were not touched.");
        }
        printState();
    }

    private void doUndo() {
        if (!controller.canUndo()) {
            out.println("Nothing to undo");
        } else if (!controller.undo()) {
            out.println("Error: undo failed because the saved change does not match the text. "
                    + "That step was dropped from the history, so the next UNDO uses the step before it.");
        }
        printState();
    }

    private void doRedo() {
        if (!controller.canRedo()) {
            out.println("Nothing to redo");
        } else if (!controller.redo()) {
            out.println("Error: redo failed because the saved change does not match the text. "
                    + "That step was dropped from the history, so the next REDO uses the step before it.");
        }
        printState();
    }

    private void doHistory() {
        int undoSize = controller.getUndoSize();
        int redoSize = controller.getRedoSize();
        if (controller.isBounded()) {
            int max = controller.getMaxHistory();
            out.println("History : limited to the newest " + max + " steps");
            out.println("Undo    : " + undoSize + " of " + max + " steps used");
            out.println("Redo    : " + redoSize + " of " + max + " steps used");
        } else {
            out.println("History : no limit (every step is kept)");
            out.println("Undo    : " + undoSize + " steps");
            out.println("Redo    : " + redoSize + " steps");
        }
        out.println("Next UNDO: " + describe(controller.peekUndo()));
        out.println("Next REDO: " + describe(controller.peekRedo()));
    }

    // LIMIT <number> | OFF   (a new Controller with the same text; the old steps cannot move to it)
    private void doLimit(String rest) {
        String word = rest.trim();
        if (word.isEmpty() || word.contains(" ")) {
            out.println("Error: missing parameter. Usage: LIMIT <number> or LIMIT OFF   e.g. LIMIT 50");
            return;
        }
        Controller next;
        if (word.equalsIgnoreCase("OFF")) {
            next = new Controller(controller.getText());
        } else {
            Integer max = toNumber(word, "The limit");
            if (max == null) {
                return;
            }
            if (max <= 0) {
                out.println("Error: the limit must be greater than 0 (or use LIMIT OFF for no limit).");
                return;
            }
            next = new Controller(controller.getText(), max);
        }
        controller = next;
        out.println((word.equalsIgnoreCase("OFF") ? "History limit removed." : "History limit set to " + word + " steps.")
                + " The text was kept; the old Undo and Redo steps were cleared.");
        printState();
    }

    private static String describe(Action a) {
        if (a == null) {
            return "(none)";
        }
        String what;
        switch (a.getType()) {
            case INSERT:
                what = "INSERT at " + a.getPosition() + ": \"" + cut(a.getNewText()) + "\"";
                break;
            case DELETE:
                what = "DELETE at " + a.getPosition() + ": \"" + cut(a.getOldText()) + "\"";
                break;
            default:
                what = "REPLACE at " + a.getPosition() + ": \"" + cut(a.getOldText()) + "\" -> \"" + cut(a.getNewText()) + "\"";
        }
        return what + "   (" + ActionDiff.storedChars(a) + " character(s) stored)";
    }

    private static String cut(String s) {
        String shown = show(s);
        return shown.length() <= MAX_SHOWN_CHARS ? shown : shown.substring(0, MAX_SHOWN_CHARS) + "...";
    }

    // When the history is full, a new edit pushes the oldest step out. Tell the user.
    private void noteIfOldestDropped(boolean applied) {
        if (applied && controller.isBounded()
                && undoBefore == controller.getMaxHistory()
                && controller.getUndoSize() == controller.getMaxHistory()) {
            out.println("Note: the history is full (limit " + controller.getMaxHistory()
                    + "), so the oldest step was dropped and can no longer be undone.");
        }
    }

    private Integer toNumber(String word, String name) {
        try {
            return Integer.valueOf(word);
        } catch (NumberFormatException e) {
            out.println("Error: " + name + " must be a whole number, but you typed '" + word + "'.");
            return null;
        }
    }

    private void finishEdit(boolean done) {
        if (!done) {
            out.println("Error: the command was not applied (position or length is not valid). The text did not change.");
        }
        noteIfOldestDropped(done);
        printState();
    }

    // \n becomes a line break and \\ becomes one backslash. Any other backslash stays as it is.
    public static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                if (next == 'n') {
                    sb.append('\n');
                    i++;
                    continue;
                }
                if (next == '\\') {
                    sb.append('\\');
                    i++;
                    continue;
                }
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    private static String show(String s) {
        return s.replace("\r", "\\r").replace("\n", "\\n");
    }

    private void printState() {
        int length = controller.getLength();
        if (length <= MAX_STATE_CHARS) {
            String text = controller.getText();
            out.println("Text  : \"" + show(text) + "\"");
            boolean oneLine = text.indexOf('\n') < 0 && text.indexOf('\r') < 0;
            if (oneLine && text.length() > 0 && text.length() <= 60) {
                StringBuilder ruler = new StringBuilder();
                for (int i = 0; i < text.length(); i++) {
                    ruler.append(i % 10);
                }
                out.println("Index :  " + ruler);
            }
        } else {
            out.println("Text  : " + length + " characters (use PRINT to view the document)");
        }
        printStatus();
    }

    private void printStatus() {
        out.println("Length: " + controller.getLength()
                + "   Undo steps: " + controller.getUndoSize()
                + "   Redo steps: " + controller.getRedoSize()
                + (controller.isBounded() ? "   History limit: " + controller.getMaxHistory() : ""));
    }

    private void printDocument() {
        String text = controller.getText();
        out.println("Text  : \"" + show(text) + "\"");
        boolean oneLine = text.indexOf('\n') < 0 && text.indexOf('\r') < 0;
        if (oneLine && text.length() > 0 && text.length() <= 60) {
            StringBuilder ruler = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                ruler.append(i % 10);
            }
            out.println("Index :  " + ruler);
        }
    }

    private void printHelp() {
        out.println("Commands (upper or lower case):");
        out.println("  INSERT     <position> <text>          insert text at a position");
        out.println("  DELETE     <position> <length>        delete characters");
        out.println("  REPLACE    <position> <length> <text> replace characters with new text");
        out.println("  FIND       <pattern>                  show every position of the pattern");
        out.println("  REPLACEALL <pattern> <replacement>    replace every match (one UNDO undoes all)");
        out.println("  SORT                                  sort the lines (one UNDO undoes the sort)");
        out.println("  UNDO                                  undo the last change");
        out.println("  REDO                                  redo the change you just undid");
        out.println("  HISTORY                               show the history limit, both counters and the next steps");
        out.println("  LIMIT      <number> | OFF             keep only the newest N steps (the text is kept)");
        out.println("  PRINT                                 show the text and the Undo / Redo counters");
        out.println("  DEMO / DEMO2 / DEMO3                  run a ready-made example");
        out.println("  HELP                                  show this list");
        out.println("  EXIT                                  leave the program");
        out.println("Write \\n for a line break inside a text, and \\\\ for one backslash.");
        out.println("REPLACEALL: the pattern is one word; the replacement is the rest of the line.");
    }

    private void runDemo() {
        out.println("--- DEMO: starting with an empty document ---");
        controller = new Controller();

        String[] script = {
            "INSERT 0 Hello World",
            "INSERT 5 , there",
            "UNDO",
            "REDO",
            "UNDO",
            "INSERT 11 !",
            "REDO",
            "REPLACE 0 5 Howdy",
            "UNDO"
        };
        runScript(script);
        out.println("--- DEMO finished ---");
    }

    private void runDemo2() {
        out.println("--- DEMO2: Find, Replace All and Sort with Undo/Redo ---");
        controller = new Controller();

        String[] script = {
            "INSERT 0 pear\\nbanana\\napple\\nbanana",
            "FIND banana",
            "REPLACEALL banana cherry",
            "UNDO",
            "REDO",
            "SORT",
            "SORT",
            "UNDO",
            "FIND cherry",
            "REPLACEALL zzz y"
        };
        runScript(script);
        out.println("--- DEMO2 finished ---");
    }

    private void runDemo3() {
        out.println("--- DEMO3: history limit and compact steps ---");
        controller = new Controller();

        String[] script = {
            "LIMIT 3",
            "INSERT 0 The quick brown fox jumps over the lazy dog",
            "REPLACEALL quick slow",
            "HISTORY",
            "INSERT 0 A",
            "INSERT 0 B",
            "INSERT 0 C",
            "HISTORY",
            "UNDO", "UNDO", "UNDO",
            "UNDO"
        };
        runScript(script);
        out.println("--- DEMO3 finished ---");
    }

    private void runScript(String[] script) {
        for (String command : script) {
            out.println("Command: " + command);
            handle(command);
        }
    }
}
