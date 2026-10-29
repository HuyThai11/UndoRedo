# Undo/Redo Engine for Text Editors

[![Java](https://img.shields.io/badge/Java-8%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Course](https://img.shields.io/badge/Course-CSD201%20(Data%20Structures%20%26%20Algorithms)-0052CC?style=for-the-badge)](https://fpt.edu.vn/)
[![Architecture](https://img.shields.io/badge/Architecture-MVC-green?style=for-the-badge)](#architecture--package-structure)
[![Build](https://img.shields.io/badge/Build-NetBeans%20Ant-orange?style=for-the-badge)](#how-to-run)

A high-performance, robust **Undo/Redo Engine** for text editors developed as **Lab 04 (Group 4 - IS2002)** for the **CSD201 (Data Structures and Algorithms)** course.

This project implements advanced data structures and algorithms—including **Gap Buffer**, **Bounded Circular Stacks**, **KMP String Search**, **Merge Sort**, and the **Command Pattern**—to support non-blocking, memory-capped multi-level undo/redo operations.

---

## Table of Contents

- [Core Highlights & Computational Thinking](#core-highlights--computational-thinking)
- [Architecture & Package Structure](#architecture--package-structure)
- [Key Features](#key-features)
- [Data Structures & Algorithms](#data-structures--algorithms)
- [Command-Line Interface (CLI)](#command-line-interface-cli)
- [How to Run](#how-to-run)
  - [Using NetBeans IDE](#using-netbeans-ide)
  - [Using Terminal / Command Line](#using-terminal--command-line)
- [Testing & Benchmarking](#testing--benchmarking)
- [Team Members & Roles](#team-members--roles)
- [License](#license)

---

## Core Highlights & Computational Thinking

The engine was architected around the 4 pillars of **Computational Thinking (CT)**:

1. **Decomposition**:
   - The system is divided into clear, decoupled responsibilities:
     - Document editing coordinator (`controller.Controller`)
     - History and stack management (`model.history.*`)
     - Text processing algorithms (`model.algorithm.*`)
     - Interactive terminal interface (`view.DemoCLI`)
2. **Pattern Recognition**:
   - All stack implementations share the unified `HistoryStack<T>` interface (`push`, `pop`, `top`/`peek`, `isEmpty`, `size`, `clear`).
   - Every document modification (Insert, Delete, Replace, Replace All, Sort) is an encapsulated `Action` paired with a deterministic `inverse()` operation (Command Pattern).
3. **Abstraction**:
   - The undo/redo engine only tracks delta changes required to reverse an edit (`Action(type, position, oldText, newText)` and `ActionDiff`), avoiding redundant full-document snapshots.
4. **Algorithm Design**:
   - Knuth-Morris-Pratt (KMP) pattern matching for instant search and bulk replacements.
   - Stable line Merge Sort preserving LF/CRLF newline delimiters.
   - Bounded circular stack with $O(1)$ eviction of oldest history items.

---

## Architecture & Package Structure

The project follows a strict **Model-View-Controller (MVC)** architecture.
Direction of dependency: `View` $\rightarrow$ `Controller` $\rightarrow$ `Model`. The Model is completely decoupled and never imports the Controller or View.

```text
Lab04_Group4_CT/
├── nbproject/                 # NetBeans Ant configuration
├── src/
│   ├── controller/
│   │   └── Controller.java    # Coordinates editing & history limits
│   ├── model/
│   │   ├── Action.java        # Immutable editing action (type, pos, old/new text)
│   │   ├── ActionDiff.java    # Compact action diff storing only changed range
│   │   ├── DocumentBuffer.java# Efficient text buffer using Gap Buffer
│   │   ├── algorithm/
│   │   │   ├── LineSorter.java# Stable Merge Sort for document lines
│   │   │   └── TextSearch.java# Knuth-Morris-Pratt (KMP) string search
│   │   └── history/
│   │       ├── HistoryStack.java       # Unified history stack interface
│   │       ├── BoundedCircularStack.java# Circular array stack with O(1) eviction
│   │       ├── BoundedLinkedStack.java  # Node-based bounded stack
│   │       ├── MyStack.java            # Standard unbounded linked stack
│   │       ├── StackNode.java          # Singly-linked stack node
│   │       └── StackEmptyException.java# Custom runtime exception
│   └── view/
│       └── DemoCLI.java       # Console user interface, REPL & demo runners
├── test/
│   ├── benchmark/             # Performance & memory benchmarks
│   │   ├── ActionDiffBenchmark.java
│   │   ├── BufferBenchmark.java        # GapBuffer vs StringBuilder
│   │   ├── ControllerBenchmark.java
│   │   ├── OperationBenchmark.java
│   │   ├── SearchBenchmark.java
│   │   ├── SortBenchmark.java
│   │   └── StackBenchmark.java
│   └── test/                  # Test suite
│       ├── ActionDiffTest.java
│       ├── ControllerHistoryLimitTest.java
│       ├── ControllerTest.java
│       ├── CoreModelDemo.java
│       ├── DemoCLITest.java
│       ├── HistoryStackTest.java
│       ├── RunAllTests.java   # Single test runner for all 9 test suites
│       ├── SearchTest.java
│       ├── SortTest.java
│       └── StackTest.java
├── build.xml                  # Ant build file
├── manifest.mf
└── README.md
```

---

## Key Features

- **Gap Buffer Text Storage (`DocumentBuffer`)**:
  - Replaces traditional `StringBuilder` with an expandable gap buffer, eliminating bulk text shifting during localized typing and deletion.
- **Multi-Level Undo & Redo**:
  - Full bidirectional navigation of document states.
  - Executing a new action properly truncates the redo history.
- **Bounded History Limits (`LIMIT <N>`)**:
  - Configurable history ceiling using circular array stacks.
  - When the history limit is reached, the oldest undo step is automatically discarded in $O(1)$ time, preventing memory exhaustion on long sessions.
- **Atomic Compound Actions**:
  - `REPLACEALL` and `SORT` execute across the whole document but are recorded as a single inverse-capable `Action`, allowing one-click rollback.
- **Robust Error Handling**:
  - All input parameters, indices, and boundary conditions are validated.
  - The CLI catches exceptions gracefully to prevent crashes.

---

## Data Structures & Algorithms

| Feature / Operation | Underlying Data Structure / Algorithm | Time Complexity | Space Complexity |
| :--- | :--- | :--- | :--- |
| **Document Buffer** | Gap Buffer (`char[]`) | Insert/Delete at cursor: $O(1)$ amortized<br>Cursor move: $O(d)$ | $O(n)$ |
| **Bounded History** | Circular Array (`BoundedCircularStack`) | Push / Pop / Evict oldest: $O(1)$ | $O(\text{limit})$ |
| **Unbounded History** | Singly Linked List (`MyStack`) | Push / Pop: $O(1)$ | $O(k)$ |
| **Text Search (`FIND`)** | Knuth-Morris-Pratt (KMP) | Pattern Preprocessing: $O(m)$<br>Search: $O(n)$ | $O(m)$ |
| **Line Sorting (`SORT`)**| Merge Sort (Stable) | $O(L \log L)$ ($L$ lines) | $O(L)$ |

---

## Command-Line Interface (CLI)

The CLI accepts both uppercase and lowercase inputs. Special escape characters like `\n` (newline) and `\\` (backslash) are supported.

| Command | Syntax | Description |
| :--- | :--- | :--- |
| `INSERT` | `INSERT <pos> <text>` | Inserts text at character index `<pos>`. |
| `DELETE` | `DELETE <pos> <length>` | Removes `<length>` characters starting at `<pos>`. |
| `REPLACE` | `REPLACE <pos> <len> <text>` | Replaces `<len>` characters at `<pos>` with `<text>`. |
| `FIND` | `FIND <pattern>` | Locates all occurrences of `<pattern>` using KMP. |
| `REPLACEALL` | `REPLACEALL <pattern> <text>` | Replaces all matches; reversible with 1 `UNDO`. |
| `SORT` | `SORT` | Alphabetically sorts lines; reversible with 1 `UNDO`. |
| `UNDO` | `UNDO` | Steps back to the previous document state. |
| `REDO` | `REDO` | Steps forward to re-apply an undone state. |
| `HISTORY` | `HISTORY` | Shows history limit, stack sizes, and upcoming steps. |
| `LIMIT` | `LIMIT <N \| OFF>` | Sets maximum history steps or disables limit. |
| `PRINT` | `PRINT` | Prints the complete document and stack counters. |
| `HELP` | `HELP` | Displays the list of available commands. |
| `DEMO` | `DEMO` / `DEMO2` / `DEMO3` | Runs pre-configured end-to-end demonstrations. |
| `EXIT` | `EXIT` | Quits the application. |

---

## How to Run

### Using NetBeans IDE

1. Open NetBeans (version 12 or newer recommended).
2. Go to **File > Open Project...** and select the folder `Lab04_Group4_CT`.
3. Right-click the project and click **Run** (Main class is `view.DemoCLI`).
4. To pass history limit arguments (e.g. limit = 50):
   - Right-click project > **Properties** > **Run** > set **Arguments** to `50`.

### Using Terminal / Command Line

You can build and run using `ant` or directly with standard `javac`/`java`:

#### Option 1: Using Apache Ant (Recommended)
```bash
# Build the project
ant compile

# Run the CLI
ant run

# Run with history limit argument
ant run -Dapplication.args="50"
```

#### Option 2: Using standard `javac` & `java`
```bash
# Compile all source files into bin directory
mkdir bin
javac -d bin -sourcepath src src/view/DemoCLI.java

# Run DemoCLI
java -cp bin view.DemoCLI

# Or run with initial history limit of 50
java -cp bin view.DemoCLI 50
```

---

## Testing & Benchmarking

### Automated Test Suites
Run the centralized test suite `RunAllTests.java`, which verifies all 9 test programs:
- In NetBeans: Navigate to `Test Packages` > `test` > Right-click `RunAllTests.java` > **Run File**.
- Expected output:
  ```text
  9 of 9 test programs passed.
  ALL TESTS PASSED.
  ```

### Benchmarks
Performance benchmarks are located in the `benchmark` package:
- `BufferBenchmark`: Compares repeated deletions in middle positions between `DocumentBuffer` (Gap Buffer) and standard `StringBuilder`.
- `StackBenchmark`: Compares memory & throughput between `BoundedCircularStack`, `BoundedLinkedStack`, and `MyStack`.
- `SearchBenchmark`: KMP pattern matching throughput.
- `SortBenchmark`: Merge Sort efficiency on large line sets.

---

## Team Members & Roles

**Course:** CSD201 - Data Structures and Algorithms  
**Class:** IS2002 | **Group:** 4

| Person | Member | Student ID | Core Responsibilities |
| :---: | :--- | :---: | :--- |
| **1** | **Nguyen Van Thang** | SE201550 | `model` (`Action`, `ActionDiff`, `DocumentBuffer`), KMP Search (`TextSearch`), CoreModel tests & benchmarks. |
| **2** | **Vo Anh Kiet** | SE200764 | `model.history` (`HistoryStack`, `BoundedCircularStack`, `BoundedLinkedStack`, `MyStack`), Line Sort (`LineSorter`), Stack & Sort tests. |
| **3** | **Huynh Huy Thai** | SE203059 | `controller` (`Controller` coordination, bounded history management, non-blocking rollback), Controller tests & benchmarks. |
| **4** | **Hoang Anh Thu** | SE204849 | `view` (`DemoCLI` REPL, command parsing, formatted UI output, history limits, demos), `RunAllTests` test suite. |

---

## License

This project was developed for educational purposes as part of the CSD201 curriculum. All rights belong to Group 4 (IS2002).
