/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Hoang Anh Thu (SE204849), Person 4 - CLI and Test Suite
 */
package test;

import controller.Controller;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import model.Action;
import view.DemoCLI;

/**
 * Automatic tests for the CLI (no typing needed - the commands are passed as Strings).
 * Run this class: every test prints PASS or FAIL, then a summary line.
 *
 * Part A = the 5 required test cases of the task.
 * Part B = extra tests (all commands, wrong syntax, exceptions from lower layers).
 * Part C = Lab 03: the new commands FIND, REPLACEALL and SORT.
 * Part D = Lab 04: the commands HISTORY and LIMIT, the "oldest step dropped" note, and DEMO3.
 */
public class DemoCLITest {

    private static final PrintStream REAL_OUT = System.out;
    private static final PrintStream REAL_ERR = System.err;

    private static int passed = 0;
    private static int failed = 0;

    // What the CLI printed in the current test.
    private static ByteArrayOutputStream printed;

    public static void main(String[] args) {
        REAL_OUT.println("=== Part A: required test cases ===");
        run("Insert -> Undo: text goes back to before the insert", new Test() {
            public void run() { testInsertUndo(); }
        });
        run("Insert -> Undo -> Redo: text is the same as after the insert", new Test() {
            public void run() { testInsertUndoRedo(); }
        });
        run("Undo on an empty stack: no crash, says 'Nothing to undo'", new Test() {
            public void run() { testUndoEmpty(); }
        });
        run("Insert -> Undo -> new Insert: Redo stack is cleared", new Test() {
            public void run() { testNewInsertClearsRedo(); }
        });
        run("Insert at a position outside the text: refused, text unchanged", new Test() {
            public void run() { testInsertOutOfRange(); }
        });

        REAL_OUT.println();
        REAL_OUT.println("=== Part B: extra tests ===");
        run("INSERT / DELETE / REPLACE / UNDO / REDO / PRINT give the right text", new Test() {
            public void run() { testAllCommands(); }
        });
        run("Commands work in lower case and keep spaces inside the text", new Test() {
            public void run() { testCaseAndSpaces(); }
        });
        run("Wrong syntax (missing parameter, not a number, unknown command): no crash", new Test() {
            public void run() { testWrongSyntax(); }
        });
        run("DELETE / REPLACE outside the text: refused, text unchanged", new Test() {
            public void run() { testDeleteReplaceOutOfRange(); }
        });
        run("Redo on an empty stack: no crash, says 'Nothing to redo'", new Test() {
            public void run() { testRedoEmpty(); }
        });
        run("An unexpected exception from a lower layer does not crash the CLI", new Test() {
            public void run() { testLowerLayerException(); }
        });
        run("EXIT stops the program, other commands keep it running", new Test() {
            public void run() { testExit(); }
        });
        run("DEMO command runs to the end", new Test() {
            public void run() { testDemo(); }
        });


        REAL_OUT.println();
        REAL_OUT.println("=== Part C: Lab 03 commands (FIND, REPLACEALL, SORT) ===");
        run("FIND shows every position, overlaps included, and does not change anything", new Test() {
            public void run() { testFind(); }
        });
        run("FIND with no match, with a missing parameter, and with a space in the pattern", new Test() {
            public void run() { testFindEdgeCases(); }
        });
        run("REPLACEALL replaces every match; ONE Undo and ONE Redo cover all of them", new Test() {
            public void run() { testReplaceAll(); }
        });
        run("REPLACEALL that changes nothing keeps the Undo and Redo stacks", new Test() {
            public void run() { testReplaceAllNoChange(); }
        });
        run("REPLACE with the same text changes nothing and keeps the Undo and Redo stacks", new Test() {
            public void run() { testReplaceSameText(); }
        });
        run("REPLACEALL wrong syntax: missing parameter, no crash, text unchanged", new Test() {
            public void run() { testReplaceAllWrongSyntax(); }
        });
        run("SORT sorts the lines; ONE Undo and ONE Redo cover the whole sort", new Test() {
            public void run() { testSort(); }
        });
        run("SORT on sorted or empty text changes nothing and keeps the Redo stack", new Test() {
            public void run() { testSortNoChange(); }
        });
        run("\\n gives a line break, \\\\ gives a backslash, and the text is shown on one line", new Test() {
            public void run() { testEscapes(); }
        });
        run("New commands work in lower case and after a new edit the Redo stack is cleared", new Test() {
            public void run() { testNewCommandsCaseAndRedo(); }
        });
        run("An exception from FIND, REPLACEALL or SORT does not crash the CLI", new Test() {
            public void run() { testNewCommandsLowerLayerException(); }
        });
        run("A failed UNDO or REDO (Controller returns false) shows an error and keeps the CLI alive", new Test() {
            public void run() { testUndoRedoFailureMessage(); }
        });
        run("HELP lists the new commands", new Test() {
            public void run() { testHelpListsNewCommands(); }
        });
        run("DEMO2 runs to the end and shows the Lab 03 story", new Test() {
            public void run() { testDemo2(); }
        });

        REAL_OUT.println();
        REAL_OUT.println("=== Part D: Lab 04 commands (HISTORY, LIMIT) ===");
        run("HISTORY with no limit: says 'no limit', both counters, and '(none)' when there is no step", new Test() {
            public void run() { testHistoryUnlimited(); }
        });
        run("HISTORY with a limit shows 'n of N', the next UNDO and the next REDO", new Test() {
            public void run() { testHistoryLimited(); }
        });
        run("HISTORY is read-only: text and both stacks stay the same", new Test() {
            public void run() { testHistoryReadOnly(); }
        });
        run("HISTORY shows a Replace All as ONE small REPLACE with its stored characters", new Test() {
            public void run() { testHistoryCompactStep(); }
        });
        run("HISTORY cuts a long text and shows a line break as \\n", new Test() {
            public void run() { testHistoryCutsLongText(); }
        });
        run("LIMIT keeps the text, clears the old steps, and turns the limit on and off", new Test() {
            public void run() { testLimitCommand(); }
        });
        run("LIMIT wrong syntax (missing, not a number, 0, negative, two words): refused, nothing changes", new Test() {
            public void run() { testLimitWrongSyntax(); }
        });
        run("A full history shows a note when an edit pushes out the oldest step (not before, not for refused edits)", new Test() {
            public void run() { testOldestDroppedNote(); }
        });
        run("Replace All and Sort Lines also show the note, and the limit really cuts the Undo steps", new Test() {
            public void run() { testLimitCutsUndo(); }
        });
        run("The counter line shows 'History limit' only when a limit is set", new Test() {
            public void run() { testCounterShowsLimit(); }
        });
        run("A failed UNDO or REDO says the step was dropped (not 'kept')", new Test() {
            public void run() { testFailedUndoSaysDropped(); }
        });
        run("An exception from HISTORY or LIMIT does not crash the CLI", new Test() {
            public void run() { testHistoryLimitLowerLayerException(); }
        });
        run("Starting with a number (java DemoCLI 5) sets the limit; a wrong number starts without a limit", new Test() {
            public void run() { testMainArgument(); }
        });
        run("HELP lists HISTORY, LIMIT and DEMO3; DEMO3 runs to the end and shows the Lab 04 story", new Test() {
            public void run() { testHelpAndDemo3(); }
        });

        REAL_OUT.println();
        REAL_OUT.println("Passed: " + passed + "  Failed: " + failed);
        REAL_OUT.println(failed == 0 ? "ALL CLI TESTS PASSED." : "SOME CLI TESTS FAILED.");
    }

    // ---------------- Part A: the 5 required cases ----------------

    private static void testInsertUndo() {
        Controller c = new Controller("Hello");
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 5  World");                 // text " World" (first space is the separator)
        check("Hello World".equals(c.getText()), "insert did not give 'Hello World'");
        cli.handle("UNDO");
        check("Hello".equals(c.getText()), "undo did not give back 'Hello'");
    }

    private static void testInsertUndoRedo() {
        Controller c = new Controller("Hello");
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 5  World");
        cli.handle("UNDO");
        cli.handle("REDO");
        check("Hello World".equals(c.getText()), "redo did not give 'Hello World'");
    }

    private static void testUndoEmpty() {
        Controller c = new Controller("abc");
        DemoCLI cli = newCli(c);
        cli.handle("UNDO");
        check(printed.toString().contains("Nothing to undo"), "message 'Nothing to undo' is missing");
        check("abc".equals(c.getText()), "undo on an empty stack changed the text");
    }

    private static void testNewInsertClearsRedo() {
        Controller c = new Controller();
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 0 A");
        cli.handle("INSERT 1 B");
        cli.handle("UNDO");
        check(c.getRedoSize() == 1, "after UNDO the Redo stack should have 1 step");
        cli.handle("INSERT 1 C");
        check(c.getRedoSize() == 0, "a new INSERT must clear the Redo stack");
        cli.handle("REDO");
        check(printed.toString().contains("Nothing to redo"), "REDO after a new edit must say 'Nothing to redo'");
        check("AC".equals(c.getText()), "REDO after a new edit changed the text");
    }

    private static void testInsertOutOfRange() {
        Controller c = new Controller("abc");
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 99 hello");
        check(printed.toString().contains("Error"), "no error message for position 99");
        check("abc".equals(c.getText()), "text changed after INSERT 99");
        cli.handle("INSERT -1 hello");
        check(printed.toString().contains("Error"), "no error message for position -1");
        check("abc".equals(c.getText()), "text changed after INSERT -1");
        check(c.getUndoSize() == 0, "a refused INSERT must not go into the Undo stack");
    }

    // ---------------- Part B: extra tests ----------------

    private static void testAllCommands() {
        Controller c = new Controller();
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 0 Hello World");
        cli.handle("DELETE 5 6");
        check("Hello".equals(c.getText()), "DELETE failed");
        cli.handle("INSERT 5 , Java");
        cli.handle("REPLACE 7 4 C++");
        check("Hello, C++".equals(c.getText()), "REPLACE failed");
        cli.handle("UNDO");
        check("Hello, Java".equals(c.getText()), "UNDO of REPLACE failed");
        cli.handle("REDO");
        check("Hello, C++".equals(c.getText()), "REDO of REPLACE failed");
        cli.handle("PRINT");
        check(printed.toString().contains("Hello, C++"), "PRINT must show the text");
        check(printed.toString().contains("Undo steps: 4"), "PRINT must show the Undo counter");
        check(printed.toString().contains("Redo steps: 0"), "PRINT must show the Redo counter");
    }

    private static void testCaseAndSpaces() {
        Controller c = new Controller();
        DemoCLI cli = newCli(c);
        cli.handle("insert 0 a  b");                   // text "a  b" has two spaces inside
        check("a  b".equals(c.getText()), "spaces inside the text were not kept");
        cli.handle("  Undo");                          // spaces before the command, mixed case
        check("".equals(c.getText()), "mixed case / leading spaces not accepted");
    }

    private static void testWrongSyntax() {
        Controller c = new Controller("abc");
        DemoCLI cli = newCli(c);
        String[] wrong = {
            "INSERT", "INSERT 5", "INSERT x hello", "DELETE", "DELETE 1", "DELETE 1 2 3",
            "DELETE a b", "REPLACE", "REPLACE 1 1", "REPLACE 1 x y", "DANCE", "123", "", "   "
        };
        for (String line : wrong) {
            cli.handle(line);                          // must not throw
        }
        check("abc".equals(c.getText()), "wrong commands changed the text");
        check(c.getUndoSize() == 0, "wrong commands were recorded in the Undo stack");
        check(printed.toString().contains("whole number"), "message 'whole number' is missing");
        check(printed.toString().contains("unknown command"), "message 'unknown command' is missing");
        check(printed.toString().contains("missing parameter"), "message 'missing parameter' is missing");
    }

    private static void testDeleteReplaceOutOfRange() {
        Controller c = new Controller("abc");
        DemoCLI cli = newCli(c);
        cli.handle("DELETE 1 50");
        cli.handle("DELETE -1 1");
        cli.handle("DELETE 0 0");
        cli.handle("REPLACE 5 1 x");
        cli.handle("REPLACE 2 9 x");
        check("abc".equals(c.getText()), "text changed by a refused DELETE / REPLACE");
        check(c.getUndoSize() == 0, "a refused DELETE / REPLACE went into the Undo stack");
    }

    private static void testRedoEmpty() {
        Controller c = new Controller("abc");
        DemoCLI cli = newCli(c);
        cli.handle("REDO");
        check(printed.toString().contains("Nothing to redo"), "message 'Nothing to redo' is missing");
        check("abc".equals(c.getText()), "redo on an empty stack changed the text");
    }

    // A Controller that fails in a way nobody expected: the CLI must still survive.
    private static void testLowerLayerException() {
        Controller broken = new Controller("abc") {
            @Override public boolean executeInsert(int position, String text) {
                throw new IllegalStateException("boom");
            }
            @Override public boolean executeDelete(int position, int length) {
                throw new NullPointerException();
            }
            @Override public boolean undo() {
                throw new ArrayIndexOutOfBoundsException("boom");
            }
            @Override public boolean canUndo() {
                return true;
            }
        };
        DemoCLI cli = newCli(broken);
        check(cli.handle("INSERT 0 x"), "CLI must keep running after an exception");
        check(cli.handle("DELETE 0 1"), "CLI must keep running after a NullPointerException");
        check(cli.handle("UNDO"), "CLI must keep running after an exception in UNDO");
        check(printed.toString().contains("The program keeps running"), "friendly error message is missing");
    }

    private static void testExit() {
        DemoCLI cli = newCli(new Controller());
        check(cli.handle("PRINT"), "PRINT must keep the program running");
        check(cli.handle("HELP"), "HELP must keep the program running");
        check(!cli.handle("EXIT"), "EXIT must stop the program");
        check(!cli.handle("exit"), "exit (lower case) must stop the program");
    }

    private static void testDemo() {
        Controller c = new Controller();
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 0 old text");               // the demo starts a fresh document
        cli.handle("DEMO");
        check(printed.toString().contains("DEMO finished"), "demo did not finish");
        check(printed.toString().contains("\"Hello World!\""), "demo should end with 'Hello World!'");
    }

    // ---------------- Part C: Lab 03 commands ----------------

    private static void testFind() {
        Controller c = new Controller("banana and band");
        DemoCLI cli = newCli(c);
        cli.handle("FIND ban");
        check(printed.toString().contains("Found 2 match(es)"), "FIND must say it found 2 matches");
        check(printed.toString().contains("position(s): 0, 11"), "FIND must show the positions 0 and 11");
        cli.handle("FIND ana");                        // overlapping letters: 'ana' in 'banana' at 1 and 3
        check(printed.toString().contains("position(s): 1, 3"), "FIND must show overlapping matches 1 and 3");
        check("banana and band".equals(c.getText()), "FIND changed the text");
        check(c.getUndoSize() == 0 && c.getRedoSize() == 0, "FIND must not touch the stacks");

        // FIND is read-only: the Redo stack of an earlier Undo must survive it
        Controller d = new Controller();
        DemoCLI cli2 = newCli(d);
        cli2.handle("INSERT 0 abc");
        cli2.handle("UNDO");
        cli2.handle("FIND a");
        check(d.getRedoSize() == 1, "FIND must not clear the Redo stack");
    }

    private static void testFindEdgeCases() {
        Controller c = new Controller("one two three");
        DemoCLI cli = newCli(c);
        cli.handle("FIND zzz");
        check(printed.toString().contains("Not found"), "FIND must say 'Not found'");
        cli.handle("FIND");
        check(printed.toString().contains("missing parameter"), "FIND without a pattern must say 'missing parameter'");
        cli.handle("FIND two three");                  // the whole rest of the line is the pattern
        check(printed.toString().contains("position(s): 4"), "a pattern with a space must be found at 4");
        check("one two three".equals(c.getText()), "FIND changed the text");
    }

    private static void testReplaceAll() {
        Controller c = new Controller("cat and cat and cat");
        DemoCLI cli = newCli(c);
        cli.handle("REPLACEALL cat dog");
        check("dog and dog and dog".equals(c.getText()), "REPLACEALL did not replace every match");
        check(printed.toString().contains("Replaced 3 match(es)"), "REPLACEALL must say how many were replaced");
        check(c.getUndoSize() == 1, "REPLACEALL must make exactly ONE Action");
        cli.handle("UNDO");
        check("cat and cat and cat".equals(c.getText()), "ONE Undo must bring back all matches");
        cli.handle("REDO");
        check("dog and dog and dog".equals(c.getText()), "ONE Redo must replace all matches again");

        // The replacement is the rest of the line, so it may contain spaces
        Controller d = new Controller("hi");
        DemoCLI cli2 = newCli(d);
        cli2.handle("REPLACEALL hi good morning");
        check("good morning".equals(d.getText()), "a replacement with a space was cut");

        // Overlapping matches: 'aa' in 'aaaa' gives 2 replacements
        Controller e = new Controller("aaaa");
        DemoCLI cli3 = newCli(e);
        cli3.handle("REPLACEALL aa b");
        check("bb".equals(e.getText()), "overlapping matches must be replaced from left to right");
    }

    private static void testReplaceSameText() {
        Controller c = new Controller("abc");
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 3 d");
        cli.handle("UNDO");                            // Redo stack has 1 step
        check(c.getRedoSize() == 1, "set-up: Redo stack should have 1 step");
        cli.handle("REPLACE 0 1 a");                   // "a" replaced by "a"
        check(printed.toString().contains("No change"), "must say 'No change' when the new text is the same");
        check(c.getRedoSize() == 1, "a REPLACE that changes nothing must keep the Redo stack");
        check(c.getUndoSize() == 0, "a REPLACE that changes nothing must not add an Undo step");
        check("abc".equals(c.getText()), "text changed by a no-change REPLACE");
    }

    private static void testReplaceAllNoChange() {
        Controller c = new Controller("apple pie");
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 9  yummy");                 // text " yummy" after the first separator space
        cli.handle("UNDO");                            // Redo stack has 1 step
        check(c.getRedoSize() == 1, "set-up: Redo stack should have 1 step");
        cli.handle("REPLACEALL banana orange");        // pattern not found
        check(printed.toString().contains("No change"), "must say 'No change' when nothing matches");
        cli.handle("REPLACEALL apple apple");          // replacement equal to the pattern
        check(c.getRedoSize() == 1, "a REPLACEALL that changes nothing must keep the Redo stack");
        check(c.getUndoSize() == 0, "a REPLACEALL that changes nothing must not add an Undo step");
        check("apple pie".equals(c.getText()), "text changed by a no-change REPLACEALL");
    }

    private static void testReplaceAllWrongSyntax() {
        Controller c = new Controller("abc");
        DemoCLI cli = newCli(c);
        String[] wrong = {"REPLACEALL", "REPLACEALL a", "REPLACEALL a ", "replaceall"};
        for (String line : wrong) {
            cli.handle(line);                          // must not throw
        }
        check(printed.toString().contains("missing parameter"), "message 'missing parameter' is missing");
        check("abc".equals(c.getText()), "wrong REPLACEALL changed the text");
        check(c.getUndoSize() == 0, "wrong REPLACEALL was recorded in the Undo stack");
    }

    private static void testSort() {
        Controller c = new Controller("pear\nbanana\napple");
        DemoCLI cli = newCli(c);
        cli.handle("SORT");
        check("apple\nbanana\npear".equals(c.getText()), "SORT did not sort the lines");
        check(printed.toString().contains("Lines sorted"), "SORT must say 'Lines sorted'");
        check(c.getUndoSize() == 1, "SORT must make exactly ONE Action");
        cli.handle("UNDO");
        check("pear\nbanana\napple".equals(c.getText()), "ONE Undo must bring back the old order");
        cli.handle("REDO");
        check("apple\nbanana\npear".equals(c.getText()), "ONE Redo must sort again");
    }

    private static void testSortNoChange() {
        Controller d = new Controller("a\nb\nc");
        DemoCLI cli2 = newCli(d);
        cli2.handle("INSERT 5 !");
        cli2.handle("UNDO");                           // Redo stack has 1 step
        cli2.handle("SORT");                           // already sorted
        check(printed.toString().contains("No change"), "must say 'No change' for sorted text");
        check(d.getRedoSize() == 1, "a SORT that changes nothing must keep the Redo stack");
        check(d.getUndoSize() == 0, "a SORT that changes nothing must not add an Undo step");

        Controller e = new Controller();
        DemoCLI cli3 = newCli(e);
        cli3.handle("SORT");
        check(printed.toString().contains("No change"), "SORT on empty text must say 'No change'");
        check(e.getUndoSize() == 0, "SORT on empty text must not add an Undo step");
    }

    private static void testEscapes() {
        Controller c = new Controller();
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 0 a\\nb");                  // typed: INSERT 0 a\nb
        check("a\nb".equals(c.getText()), "\\n must give a real line break");
        check(printed.toString().contains("\"a\\nb\""), "the text must be shown on one line with \\n");
        cli.handle("INSERT 0 x\\\\y");                // typed: INSERT 0 x\\y
        check("x\\ya\nb".equals(c.getText()), "two backslashes must give one backslash");
        check(DemoCLI.unescape("a\\qb").equals("a\\qb"), "an unknown escape must stay as it is");
        check(DemoCLI.unescape("end\\").equals("end\\"), "a single backslash at the end must stay");
    }

    private static void testNewCommandsCaseAndRedo() {
        Controller c = new Controller("b\na");
        DemoCLI cli = newCli(c);
        cli.handle("  sort");                          // lower case and leading spaces
        check("a\nb".equals(c.getText()), "lower case 'sort' not accepted");
        cli.handle("undo");
        cli.handle("replaceall a z");
        check("b\nz".equals(c.getText()), "lower case 'replaceall' not accepted");
        check(c.getRedoSize() == 0, "a REPLACEALL that changes the text must clear the Redo stack");
        cli.handle("find z");
        check(printed.toString().contains("Found 1 match(es)"), "lower case 'find' not accepted");
    }

    private static void testNewCommandsLowerLayerException() {
        Controller broken = new Controller("abc") {
            @Override public java.util.List<Integer> find(String pattern) {
                throw new NullPointerException();
            }
            @Override public int executeReplaceAll(String pattern, String replacement) {
                throw new IllegalArgumentException("Search pattern cannot be null or empty.");
            }
            @Override public boolean executeSortLines() {
                throw new IllegalStateException("boom");
            }
        };
        DemoCLI cli = newCli(broken);
        check(cli.handle("FIND a"), "CLI must keep running after an exception in FIND");
        check(cli.handle("REPLACEALL a b"), "CLI must keep running after an exception in REPLACEALL");
        check(cli.handle("SORT"), "CLI must keep running after an exception in SORT");
        check(printed.toString().contains("The program keeps running"), "friendly error message is missing");
    }

    // The new Controller (Lab 03) returns false when apply() rejects an Action. The CLI must say so.
    private static void testUndoRedoFailureMessage() {
        Controller failing = new Controller("abc") {
            @Override public boolean canUndo() { return true; }
            @Override public boolean canRedo() { return true; }
            @Override public boolean undo() { return false; }
            @Override public boolean redo() { return false; }
        };
        DemoCLI cli = newCli(failing);
        check(cli.handle("UNDO"), "CLI must keep running after a failed UNDO");
        check(printed.toString().contains("undo failed"), "a failed UNDO must show an error message");
        check(cli.handle("REDO"), "CLI must keep running after a failed REDO");
        check(printed.toString().contains("redo failed"), "a failed REDO must show an error message");
    }

    private static void testHelpListsNewCommands() {
        DemoCLI cli = newCli(new Controller());
        cli.handle("HELP");
        String help = printed.toString();
        check(help.contains("FIND"), "HELP must list FIND");
        check(help.contains("REPLACEALL"), "HELP must list REPLACEALL");
        check(help.contains("SORT"), "HELP must list SORT");
        check(help.contains("DEMO2"), "HELP must list DEMO2");
    }

    private static void testDemo2() {
        Controller c = new Controller();
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 0 old text");               // the demo starts a fresh document
        cli.handle("DEMO2");
        String shown = printed.toString();
        check(shown.contains("DEMO2 finished"), "demo 2 did not finish");
        check(shown.contains("Replaced 2 match(es)"), "demo 2 must replace 2 matches");
        check(shown.contains("Lines sorted"), "demo 2 must sort the lines");
        check(shown.contains("No change"), "demo 2 must show a change that changes nothing");
    }


    // ---------------- Part D: Lab 04 ----------------

    private static void testHistoryUnlimited() {
        Controller c = new Controller();
        DemoCLI cli = newCli(c);
        cli.handle("HISTORY");
        String out = printed.toString();
        check(out.contains("no limit"), "HISTORY must say there is no limit");
        check(out.contains("Undo    : 0 steps"), "HISTORY must show 0 Undo steps");
        check(out.contains("Next UNDO: (none)"), "HISTORY must show (none) when there is nothing to undo");
        check(out.contains("Next REDO: (none)"), "HISTORY must show (none) when there is nothing to redo");
        cli.handle("INSERT 0 ab");
        cli.handle("HISTORY");
        check(printed.toString().contains("Undo    : 1 steps"), "HISTORY must count the Undo step");
    }

    private static void testHistoryLimited() {
        Controller c = new Controller();
        DemoCLI cli = newCli(c);
        cli.handle("LIMIT 3");
        cli.handle("INSERT 0 abc");
        cli.handle("DELETE 1 1");
        cli.handle("HISTORY");
        String out = printed.toString();
        check(out.contains("limited to the newest 3 steps"), "HISTORY must show the limit");
        check(out.contains("Undo    : 2 of 3"), "HISTORY must show 'Undo : 2 of 3'");
        check(out.contains("Redo    : 0 of 3"), "HISTORY must show 'Redo : 0 of 3'");
        check(out.contains("Next UNDO: DELETE at 1: \"b\""), "HISTORY must describe the DELETE as the next UNDO");
        cli.handle("UNDO");
        cli.handle("HISTORY");
        out = printed.toString();
        check(out.contains("Next UNDO: INSERT at 0: \"abc\""), "after UNDO the next UNDO is the INSERT");
        check(out.contains("Next REDO: DELETE at 1: \"b\""), "after UNDO the next REDO is the DELETE");
    }

    private static void testHistoryReadOnly() {
        Controller c = new Controller("abc");
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 3 d");
        cli.handle("UNDO");
        cli.handle("INSERT 3 x");
        cli.handle("UNDO");                       // Undo 0, Redo 1
        cli.handle("HISTORY");
        cli.handle("history");
        check("abc".equals(c.getText()), "HISTORY changed the text");
        check(c.getUndoSize() == 0 && c.getRedoSize() == 1, "HISTORY changed a stack");
    }

    private static void testHistoryCompactStep() {
        Controller c = new Controller("one two three");
        DemoCLI cli = newCli(c);
        cli.handle("REPLACEALL two 2");
        cli.handle("HISTORY");
        String out = printed.toString();
        check(c.getUndoSize() == 1, "Replace All must be one step");
        check(out.contains("Next UNDO: REPLACE at 4: \"two\" -> \"2\"   (4 character(s) stored)"),
                "the compact REPLACE and its 4 stored characters must be shown: " + out);
        // The Action keeps only the changed part, not the whole text
        Action a = c.peekUndo();
        check(a.getOldText().length() + a.getNewText().length() < c.getLength(), "the step must be smaller than the text");
        check(c.undo() && "one two three".equals(c.getText()), "the compact step must still undo the whole Replace All");
    }

    private static void testHistoryCutsLongText() {
        Controller c = new Controller();
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 0 aaaaaaaaaabbbbbbbbbbccccccccccdddddddddd");
        cli.handle("HISTORY");
        check(printed.toString().contains("\"aaaaaaaaaabbbbbbbbbb...\""), "a long text must be cut to 20 characters and '...'");
        cli = newCli(new Controller());
        cli.handle("INSERT 0 x\\ny");
        cli.handle("HISTORY");
        check(printed.toString().contains("INSERT at 0: \"x\\ny\""), "a line break must be shown as \\n");
    }

    private static void testLimitCommand() {
        Controller c = new Controller("abc");
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 3 d");                  // Undo 1
        int mark = printed.size();                 // only look at what LIMIT prints
        cli.handle("LIMIT 2");
        String out = printed.toString().substring(mark);
        check(out.contains("History limit set to 2 steps"), "LIMIT must confirm the new limit");
        check(out.contains("Text  : \"abcd\""), "LIMIT must keep the text");
        check(out.contains("Undo steps: 0"), "LIMIT must clear the old Undo steps");
        check(out.contains("History limit: 2"), "the counter line must show the limit");
        mark = printed.size();
        cli.handle("limit off");
        out = printed.toString().substring(mark);
        check(out.contains("History limit removed"), "LIMIT OFF must confirm");
        check(out.contains("Text  : \"abcd\""), "LIMIT OFF must keep the text");
        check(!out.contains("History limit:"), "after LIMIT OFF the counter line must not show a limit");
        mark = printed.size();
        cli.handle("HISTORY");
        check(printed.toString().substring(mark).contains("no limit"), "after LIMIT OFF HISTORY must say no limit");
    }

    private static void testLimitWrongSyntax() {
        Controller c = new Controller("abc");
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 3 d");
        String[] wrong = {"LIMIT", "LIMIT five", "LIMIT 0", "LIMIT -2", "LIMIT 3 4", "LIMIT 2.5"};
        for (String command : wrong) {
            check(cli.handle(command), "CLI must keep running after: " + command);
        }
        String out = printed.toString();
        check(out.contains("missing parameter"), "LIMIT with no value must say 'missing parameter'");
        check(out.contains("whole number"), "LIMIT five must say 'whole number'");
        check(out.contains("the limit must be greater than 0"), "LIMIT 0 or -2 must say 'the limit must be greater than 0'");
        check(!out.contains("something went wrong"), "a wrong LIMIT must be handled by the CLI, not by the exception catch");
        check(!out.contains("History limit set"), "no wrong LIMIT may set a limit");
        check("abcd".equals(c.getText()) && c.getUndoSize() == 1, "a wrong LIMIT changed the text or the history");
    }

    private static void testOldestDroppedNote() {
        Controller c = new Controller("", 2);
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 0 a");
        cli.handle("INSERT 1 b");                  // 2 steps = full, but nothing was pushed out yet
        check(!printed.toString().contains("Note:"), "no note before an edit pushes a step out");
        cli.handle("INSERT 99 z");                 // refused edit
        check(!printed.toString().contains("Note:"), "a refused edit must not show the note");
        check(c.getUndoSize() == 2 && "ab".equals(c.getText()), "a refused edit must not change the history or the text");
        cli.handle("INSERT 2 c");                  // pushes the oldest step out
        check(printed.toString().contains("Note: the history is full (limit 2)"),
                "the note must appear when the oldest step is dropped");
        check(c.getUndoSize() == 2 && "abc".equals(c.getText()), "history must stay at 2 and the text must be abc");
    }

    private static void testLimitCutsUndo() {
        Controller c = new Controller("", 2);
        DemoCLI cli = newCli(c);
        cli.handle("INSERT 0 b\\na\\nb");          // step 1
        cli.handle("REPLACEALL b c");              // step 2 (history is full)
        check(!printed.toString().contains("Note:"), "no note while the history only becomes full");
        cli.handle("SORT");                        // step 3: pushes step 1 out
        check(printed.toString().contains("Note: the history is full"), "SORT must show the note when it pushes a step out");
        check("a\nc\nc".equals(c.getText()), "text after SORT must be a, c, c");
        check(c.undo() && "c\na\nc".equals(c.getText()), "first Undo must undo the SORT");
        check(c.undo() && "b\na\nb".equals(c.getText()), "second Undo must undo the Replace All");
        check(!c.undo(), "the INSERT was pushed out, so a third Undo must fail");
    }

    private static void testCounterShowsLimit() {
        DemoCLI cli = newCli(new Controller("abc"));
        cli.handle("PRINT");
        check(!printed.toString().contains("History limit"), "an unlimited history must not show 'History limit'");
        cli.handle("LIMIT 7");
        cli.handle("PRINT");
        check(printed.toString().contains("Redo steps: 0   History limit: 7"), "a limited history must show 'History limit: 7'");
    }

    private static void testFailedUndoSaysDropped() {
        Controller failing = new Controller("abc") {
            @Override public boolean canUndo() { return true; }
            @Override public boolean canRedo() { return true; }
            @Override public boolean undo() { return false; }
            @Override public boolean redo() { return false; }
        };
        DemoCLI cli = newCli(failing);
        cli.handle("UNDO");
        cli.handle("REDO");
        String out = printed.toString();
        check(out.contains("dropped from the history"), "a failed UNDO or REDO must say the step was dropped");
        check(!out.contains("history was kept"), "the old text 'The history was kept' must be gone");
    }

    private static void testHistoryLimitLowerLayerException() {
        Controller broken = new Controller("abc") {
            @Override public int getUndoSize() { throw new IllegalStateException("broken"); }
        };
        DemoCLI cli = newCli(broken);
        check(cli.handle("HISTORY"), "CLI must keep running after an exception in HISTORY");
        check(cli.handle("LIMIT 3"), "CLI must keep running after an exception in LIMIT");
        check(printed.toString().contains("The program keeps running"), "friendly error message is missing");
    }

    private static void testMainArgument() {
        check(runMain(new String[]{"5"}, "PRINT\nexit\n").contains("History limit: 5"), "java DemoCLI 5 must set the limit 5");
        check(!runMain(new String[]{}, "PRINT\nexit\n").contains("History limit"), "no argument must mean no limit");
        String wrong = runMain(new String[]{"abc"}, "PRINT\nexit\n");
        check(wrong.contains("must be a whole number greater than 0") && !wrong.contains("History limit:"),
                "a wrong argument must show an error and start without a limit");
        check(runMain(new String[]{"0"}, "PRINT\nexit\n").contains("must be a whole number greater than 0"),
                "the argument 0 must show an error");
    }

    private static void testHelpAndDemo3() {
        DemoCLI cli = newCli(new Controller());
        cli.handle("HELP");
        String help = printed.toString();
        check(help.contains("HISTORY"), "HELP must list HISTORY");
        check(help.contains("LIMIT"), "HELP must list LIMIT");
        check(help.contains("DEMO3"), "HELP must list DEMO3");
        cli = newCli(new Controller());
        cli.handle("DEMO3");
        String out = printed.toString();
        check(out.contains("DEMO3 finished"), "DEMO3 did not finish");
        check(out.contains("Note: the history is full"), "DEMO3 must show the 'history is full' note");
        check(out.contains("REPLACE at 4: \"quick\" -> \"slow\""), "DEMO3 must show the compact REPLACE");
        check(out.contains("Nothing to undo"), "DEMO3 must end with 'Nothing to undo'");
    }

    // Runs DemoCLI.main with the given argument and typed input, and returns everything it printed.
    private static String runMain(String[] args, String input) {
        java.io.InputStream realIn = System.in;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setIn(new java.io.ByteArrayInputStream(input.getBytes()));
        System.setOut(new PrintStream(captured));
        try {
            DemoCLI.main(args);
        } finally {
            System.setIn(realIn);
            System.setOut(new PrintStream(new ByteArrayOutputStream()));
        }
        return captured.toString();
    }

    // ---------------- helpers ----------------

    private interface Test {
        void run();
    }

    // A CLI that writes into a buffer, so a test can read what was printed.
    private static DemoCLI newCli(Controller controller) {
        printed = new ByteArrayOutputStream();
        return new DemoCLI(controller, new PrintStream(printed));
    }

    // Runs one test, hides the Controller's own messages, and prints PASS or FAIL.
    private static void run(String name, Test test) {
        PrintStream quiet = new PrintStream(new ByteArrayOutputStream());
        System.setOut(quiet);
        System.setErr(quiet);
        String result;
        try {
            test.run();
            passed++;
            result = "PASS  " + name;
        } catch (AssertionError e) {
            failed++;
            result = "FAIL  " + name + "   -> " + e.getMessage();
        } catch (RuntimeException e) {
            failed++;
            result = "FAIL  " + name + "   -> crashed: " + e;
        } finally {
            System.setOut(REAL_OUT);
            System.setErr(REAL_ERR);
        }
        REAL_OUT.println(result);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
