Lab 04 - Undo/Redo Engine - project of the whole group (Group 4, CSD201)
NetBeans 13 project (Java with Ant, Java 8). Source packages follow MVC; tests and benchmarks use the test source root.

How CT gives the packages
  1. Decomposition       -> 3 main modules: text editing (controller), history (model.history), tools (model.algorithm),
                            plus the console (view).
  2. Pattern recognition -> all three stacks do the same job (push, pop, top, isEmpty, size, clear),
                            so they share ONE interface: HistoryStack<T> (package model.history).
                            Every edit (Insert, Delete, Replace, Replace All, Sort) is ONE Action with inverse().
  3. Abstraction         -> only the data that Undo/Redo needs: Action (type, position, oldText, newText)
                            and DocumentBuffer (the text). Package model.
  4. Algorithm design    -> KMP search and Merge Sort are in model.algorithm;
                            ActionDiff (compact Action) is in model.

Package structure
  model          Action, ActionDiff, DocumentBuffer                    (Thang)   - data, compact Action and text
  model.history  HistoryStack (interface), StackNode, StackEmptyException,
                 MyStack, BoundedCircularStack, BoundedLinkedStack     (Kiet)    - Pattern recognition: the Undo / Redo stacks
  model.algorithm TextSearch (KMP), LineSorter (Merge Sort)            (Thang, Kiet) - Algorithm design
  controller     Controller                                            (Thai)    - the only class that changes the text
  view           DemoCLI                                               (Thu)     - console: reads commands, prints results
  test           9 test programs + RunAllTests (separate test root)     (all)
  benchmark      7 benchmark programs (separate test root)              (all)
  Direction of use: view -> controller -> model. The model never uses the controller or the view.

Open:  File > Open Project > choose the folder Lab04_Group4_CT
       (if NetBeans asks about build-impl.xml / "Resolve Problems", accept - it regenerates it).
Run ALL tests:  in Test Packages, right-click RunAllTests.java > Run File -> "9 of 9 test programs passed."
Interface:  right-click the project > Run (main class = view.DemoCLI). Type HELP, then DEMO3, DEMO2 or DEMO.
       To start with a history limit: Project Properties > Run > Arguments = 50
       (or type LIMIT 50 inside the program).
Benchmarks: in Test Packages, right-click a benchmark file > Run File.
BufferBenchmark compares repeated middle-position deletes in the gap buffer and StringBuilder.

What is new in Lab 04
  Thang  : ActionDiff.java (compact Action), ActionDiffTest.java (14 checks), ActionDiffBenchmark.java
  Kiet   : HistoryStack.java (interface), MyStack / BoundedCircularStack / BoundedLinkedStack implement it,
           HistoryStackTest.java (29 checks), ControllerHistoryLimitTest.java (9 checks)
  Thai   : Controller.java (limited history, compact Actions, non-blocking rollback), ControllerTest.java (23 tests),
           ControllerBenchmark.java
  Thu    : DemoCLI.java (HISTORY, LIMIT, DEMO3, start argument), DemoCLITest.java (41 tests), RunAllTests.java (9 programs)

Who wrote the other files (unchanged from Lab 03)
  Thang  : Action.java, DocumentBuffer.java, TextSearch.java, CoreModelDemo.java, SearchTest.java, SearchBenchmark.java
  Kiet   : StackNode.java, StackTest.java, StackBenchmark.java, LineSorter.java, SortTest.java, SortBenchmark.java

The current DocumentBuffer uses a gap buffer. Repeated edits near one cursor avoid shifting
the whole document each time; moving the cursor far away still requires a copy proportional
to the distance. DELETE and REPLACE take the removed substring directly from the buffer.
Reading the complete document with getText() still takes O(n) time and memory.
The automatic CLI status uses a compact message for documents over 200 characters; PRINT
still displays the complete document. Action is immutable and exposes getters plus value-based
equals and hashCode.
LineSorter preserves each LF or CRLF separator at its original position. SORT on already
ordered lines leaves the exact text and Undo/Redo history unchanged.
TextSearch (KMP) keeps the same algorithm.
Do not use the copies of Action / DocumentBuffer / TextSearch / LineSorter that were inside the first Lab04_Thai_Controller.zip:
they differ from the group project.

Consistency (checked before submission)
  - Every class starts with the same header: course, group, file owner and role.
  - All program output is in English; every test program ends with "Passed: X  Failed: Y" and
    "ALL <NAME> TESTS PASSED." (RunAllTests checks this line).
  - Production classes are grouped by MVC responsibility (see above).
  - Stack names follow lecture 2A (Stacks): push, pop, top, isEmpty, clear (+ size); pop() and top() on an
    empty stack throw StackEmptyException("Stack is empty"). peek() is kept as another name for top(), and
    StackEmptyException extends IllegalStateException, so the Lab 02-03 code still works.
  - UndoRedoIntegrationExample.java (a Lab 02 sketch with its own stacks, not used by anything) was removed:
    the only Undo/Redo engine is Controller.

Benchmark numbers on earlier review slides describe the previous StringBuilder implementation.
Re-run benchmarks and update the report or slides before citing numbers for this version.

Remaining limits: ActionDiff stores one continuous changed range, so widely separated edits can
still keep nearly the entire document in history. History limits count steps rather than stored
characters. Text positions use Java char offsets, so a caller can split a Unicode surrogate pair.
