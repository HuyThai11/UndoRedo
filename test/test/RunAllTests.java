/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Hoang Anh Thu (SE204849), Person 4 - CLI and Test Suite
 */
package test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Runs every test program of the project one after the other, on one computer, and prints one
 * summary table at the end. This is the program to run (right-click > Run File) before taking
 * the NetBeans screenshot for the report.
 *
 * Each test program keeps its own output. A test program counts as passed when its output
 * contains its own success line (for example "ALL CLI TESTS PASSED.") and it did not throw.
 *
 * Every test program is started by name, so this file compiles even if one of them is
 * not in the project yet; the summary then shows it as NOT FOUND.
 */
public class RunAllTests {

    private static final String[][] TESTS = {
        {"CoreModelDemo",  "ALL CORE MODEL TESTS PASSED"},
        {"StackTest",      "ALL STACK TESTS PASSED"},
        {"SearchTest",     "ALL SEARCH TESTS PASSED"},
        {"SortTest",       "ALL SORT TESTS PASSED"},
        {"ControllerTest", "ALL CONTROLLER TESTS PASSED"},
        {"DemoCLITest",    "ALL CLI TESTS PASSED"},
        // Lab 04
        {"HistoryStackTest",           "ALL HISTORY STACK TESTS PASSED"},
        {"ActionDiffTest",             "ALL ACTIONDIFF TESTS PASSED"},
        {"ControllerHistoryLimitTest", "ALL HISTORY LIMIT TESTS PASSED"},
    };

    public static void main(String[] args) {
        PrintStream realOut = System.out;
        String[] results = new String[TESTS.length];
        int passed = 0;

        for (int i = 0; i < TESTS.length; i++) {
            String name = TESTS[i][0];
            String successLine = TESTS[i][1];
            realOut.println();
            realOut.println("################ " + name + " ################");

            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            System.setOut(new PrintStream(new Tee(realOut, captured), true));
            String result;
            try {
                Class<?> type = Class.forName("test." + name);   // all test programs are in the package "test"
                Method main = type.getMethod("main", String[].class);
                main.invoke(null, (Object) new String[0]);
                result = captured.toString().contains(successLine) ? "PASSED" : "FAILED (success line not printed)";
            } catch (ClassNotFoundException e) {
                result = "NOT FOUND (add " + name + ".java to the project)";
            } catch (InvocationTargetException e) {
                result = "FAILED (" + e.getCause() + ")";
            } catch (ReflectiveOperationException e) {
                result = "FAILED (" + e + ")";
            } finally {
                System.out.flush();
                System.setOut(realOut);
            }
            results[i] = result;
            if (result.equals("PASSED")) {
                passed++;
            }
        }

        realOut.println();
        realOut.println("====================== SUMMARY ======================");
        for (int i = 0; i < TESTS.length; i++) {
            realOut.printf("%-28s %s%n", TESTS[i][0], results[i]);
        }
        realOut.println("-----------------------------------------------------");
        realOut.println(passed + " of " + TESTS.length + " test programs passed.");
        realOut.println("Java " + System.getProperty("java.version") + " on " + System.getProperty("os.name"));
    }

    // Writes to two streams at the same time (the screen and a memory buffer).
    private static class Tee extends OutputStream {
        private final OutputStream first;
        private final OutputStream second;

        Tee(OutputStream first, OutputStream second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public void write(int b) throws java.io.IOException {
            first.write(b);
            second.write(b);
        }

        @Override
        public void flush() throws java.io.IOException {
            first.flush();
            second.flush();
        }
    }
}
