/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Vo Anh Kiet (SE200764), Person 2 - History Stacks and Sorting
 */
package test;

import model.history.BoundedCircularStack;
import model.history.BoundedLinkedStack;
import model.history.HistoryStack;
import model.history.MyStack;
import model.history.StackEmptyException;
import java.util.ArrayDeque;
import java.util.Random;
import model.Action;

/**
 * Lab 04 tests for the HistoryStack contract. No libraries needed.
 * The same contract checks run on all three stacks: MyStack, BoundedCircularStack and
 * BoundedLinkedStack. Prints PASS or FAIL for each check and a summary line.
 */
public class HistoryStackTest {

    static int passed = 0, failed = 0;

    /** Creates a fresh stack for one implementation. capacity is ignored by MyStack. */
    interface Factory { HistoryStack<Integer> make(int capacity); }

    public static void main(String[] args) {
        Factory unbounded = c -> new MyStack<Integer>();
        Factory circular = c -> new BoundedCircularStack<Integer>(c);
        Factory linked = c -> new BoundedLinkedStack<Integer>(c);

        contract("MyStack", unbounded, false);
        contract("BoundedCircularStack", circular, true);
        contract("BoundedLinkedStack", linked, true);

        // Kiet's own Lab 04 scenarios, kept (the empty case now throws instead of returning null)
        BoundedCircularStack<String> w = new BoundedCircularStack<>(3);
        w.push("A"); w.push("B"); w.push("C");
        boolean ok = "C".equals(w.pop());
        w.push("D"); w.push("E");
        ok &= "E".equals(w.pop()) && "D".equals(w.pop()) && "B".equals(w.pop()) && w.isEmpty();
        check("circular wrap-around: A B C, pop, D E -> E D B (A was dropped)", ok);

        BoundedCircularStack<Integer> cap = new BoundedCircularStack<>(3);
        for (int i = 1; i <= 5; i++) cap.push(i);
        check("overflow keeps the newest 3 items (5 4 3), capacity() = 3",
                cap.size() == 3 && cap.capacity() == 3 && cap.pop() == 5 && cap.pop() == 4 && cap.pop() == 3 && cap.isEmpty());

        // Model cross-check: random operations against a simple ArrayDeque model
        Random r = new Random(2026);
        int bad = 0;
        for (int round = 0; round < 3000; round++) {
            int capacity = 1 + r.nextInt(6);
            HistoryStack<Integer> a = new BoundedCircularStack<>(capacity);
            HistoryStack<Integer> b = new BoundedLinkedStack<>(capacity);
            HistoryStack<Integer> u = new MyStack<>();
            ArrayDeque<Integer> modelB = new ArrayDeque<>();   // bounded model: top = first
            ArrayDeque<Integer> modelU = new ArrayDeque<>();
            for (int step = 0; step < 60; step++) {
                int op = r.nextInt(10);
                if (op < 5) {
                    int v = r.nextInt(1000);
                    a.push(v); b.push(v); u.push(v);
                    modelB.addFirst(v);
                    if (modelB.size() > capacity) modelB.removeLast();
                    modelU.addFirst(v);
                } else if (op < 8) {
                    if (modelB.isEmpty()) {
                        if (!throwsEmpty(a) || !throwsEmpty(b)) bad++;
                    } else {
                        int e = modelB.removeFirst();
                        if (a.pop() != e || b.pop() != e) bad++;
                    }
                    if (modelU.isEmpty()) { if (!throwsEmpty(u)) bad++; }
                    else if (!u.pop().equals(modelU.removeFirst())) bad++;
                } else if (op == 8) {
                    if (!modelB.isEmpty() && (!a.peek().equals(modelB.peekFirst()) || !b.peek().equals(modelB.peekFirst()))) bad++;
                } else if (r.nextInt(4) == 0) {
                    a.clear(); b.clear(); u.clear(); modelB.clear(); modelU.clear();
                }
                if (a.size() != modelB.size() || b.size() != modelB.size() || u.size() != modelU.size()) bad++;
                if (a.isEmpty() != modelB.isEmpty() || b.isEmpty() != modelB.isEmpty()) bad++;
            }
        }
        check("3000 random runs x 60 steps: all three stacks agree with a simple model (mismatches: " + bad + ")", bad == 0);

        // The Controller field type: code written against HistoryStack<Action> works with any of them
        HistoryStack<Action> undo = new BoundedCircularStack<>(2);
        undo.push(new Action(Action.Type.INSERT, 0, "", "a"));
        undo.push(new Action(Action.Type.INSERT, 1, "", "b"));
        undo.push(new Action(Action.Type.INSERT, 2, "", "c"));
        check("HistoryStack<Action> with capacity 2: three pushes keep the newest two (c then b)",
                undo.size() == 2 && undo.pop().getNewText().equals("c")
                        && undo.pop().getNewText().equals("b") && undo.isEmpty());

        System.out.println();
        System.out.println("Passed: " + passed + "  Failed: " + failed);
        System.out.println(failed == 0 ? "ALL HISTORY STACK TESTS PASSED." : "SOME HISTORY STACK TESTS FAILED.");
    }

    /** Checks that apply to every HistoryStack. */
    static void contract(String name, Factory f, boolean bounded) {
        HistoryStack<Integer> s = f.make(3);
        check(name + ": new stack is empty, size 0", s.isEmpty() && s.size() == 0);
        check(name + ": pop on empty throws StackEmptyException(\"Stack is empty\")", throwsEmpty(s));
        check(name + ": top on empty throws StackEmptyException(\"Stack is empty\")", peekThrowsEmpty(s));
        s.push(10); s.push(20);
        check(name + ": top returns the newest and does not remove it", s.top() == 20 && s.size() == 2);
        check(name + ": pop is LIFO (20, then 10), then empty", s.pop() == 20 && s.pop() == 10 && s.isEmpty());
        s.push(1); s.push(2);
        s.clear();
        check(name + ": clear empties the stack, and it can be used again", s.isEmpty() && s.size() == 0
                && throwsEmpty(s) && pushPop(s));
        if (bounded) {
            HistoryStack<Integer> b = f.make(3);
            for (int i = 1; i <= 7; i++) b.push(i);
            check(name + ": 7 pushes with capacity 3 keep 7 6 5 and drop the oldest",
                    b.size() == 3 && b.pop() == 7 && b.pop() == 6 && b.pop() == 5 && b.isEmpty());
            HistoryStack<Integer> one = f.make(1);
            one.push(1); one.push(2);
            check(name + ": capacity 1 keeps only the newest item", one.size() == 1 && one.pop() == 2 && one.isEmpty());
            boolean z = false, n = false;
            try { f.make(0); } catch (IllegalArgumentException e) { z = true; }
            try { f.make(-1); } catch (IllegalArgumentException e) { n = true; }
            check(name + ": capacity 0 and -1 throw IllegalArgumentException", z && n);
        } else {
            HistoryStack<Integer> big = f.make(0);
            for (int i = 0; i < 100000; i++) big.push(i);
            check(name + ": unbounded, keeps all 100,000 items", big.size() == 100000 && big.pop() == 99999);
        }
    }

    static boolean pushPop(HistoryStack<Integer> s) { s.push(9); return s.pop() == 9 && s.isEmpty(); }

    static boolean throwsEmpty(HistoryStack<Integer> s) {
        try { s.pop(); return false; } catch (StackEmptyException e) { return "Stack is empty".equals(e.getMessage()); }
    }

    static boolean peekThrowsEmpty(HistoryStack<Integer> s) {
        try { s.top(); return false; } catch (StackEmptyException e) { return "Stack is empty".equals(e.getMessage()); }
    }

    static void check(String description, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + description);
        if (ok) passed++; else failed++;
    }
}
