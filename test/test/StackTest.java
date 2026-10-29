/*
 * CSD201 - Group 4 (IS2002) - Undo/Redo Engine for Text Editors
 * File owner: Vo Anh Kiet (SE200764), Person 2 - History Stacks and Sorting
 */
package test;

import model.history.BoundedCircularStack;
import model.history.BoundedLinkedStack;
import model.history.MyStack;
import model.history.StackEmptyException;

/**
 * Basic correctness tests for Kiệt's Stack Engine.
 */
public class StackTest {

    private static int passed = 0;

    public static void main(String[] args) {
        testLifo();
        passed++;
        testEmptyException();
        passed++;
        testPeek();
        passed++;
        testSize();
        passed++;
        testCircularCapacity();
        passed++;
        testClear();
        passed++;
        testBoundedLinkedStack();
        passed++;
        testBothBoundedStacksAgree();
        passed++;

        System.out.println();
        System.out.println("Passed: " + passed + "  Failed: 0");
        System.out.println("ALL STACK TESTS PASSED.");
    }

    private static void testLifo() {
        MyStack<Integer> stack = new MyStack<>();

        stack.push(10);
        stack.push(20);
        stack.push(30);

        check(stack.pop() == 30, "LIFO pop #1 failed");
        check(stack.pop() == 20, "LIFO pop #2 failed");
        check(stack.pop() == 10, "LIFO pop #3 failed");
    }

    // Lecture 2A: pop() and top() on an empty stack throw StackEmptyException.
    private static void testEmptyException() {
        MyStack<Integer> stack = new MyStack<>();

        boolean popThrown = false;
        try {
            stack.pop();
        } catch (StackEmptyException e) {
            popThrown = "Stack is empty".equals(e.getMessage());
        }

        boolean topThrown = false;
        try {
            stack.top();
        } catch (StackEmptyException e) {
            topThrown = "Stack is empty".equals(e.getMessage());
        }

        check(popThrown, "pop() must throw StackEmptyException 'Stack is empty'");
        check(topThrown, "top() must throw StackEmptyException 'Stack is empty'");
    }

    // top() is the lecture name; peek() is kept as the same operation (java.util.Stack name).
    private static void testPeek() {
        MyStack<String> stack = new MyStack<>();
        stack.push("A");
        stack.push("B");

        check("B".equals(stack.top()), "top() returned wrong value");
        check("B".equals(stack.peek()), "peek() must give the same item as top()");
        check(stack.size() == 2, "top() must not remove the top item");
    }

    private static void testSize() {
        MyStack<Integer> stack = new MyStack<>();

        check(stack.size() == 0, "Initial size must be 0");
        stack.push(1);
        stack.push(2);
        check(stack.size() == 2, "Size after push failed");
        stack.pop();
        check(stack.size() == 1, "Size after pop failed");
        stack.pop();
        check(stack.size() == 0, "Size after final pop failed");
        check(stack.isEmpty(), "Stack should be empty");
    }

    private static void testCircularCapacity() {
        BoundedCircularStack<Integer> stack =
                new BoundedCircularStack<>(3);

        stack.push(1);
        stack.push(2);
        stack.push(3);
        stack.push(4); // oldest 1 is discarded

        check(stack.size() == 3, "Circular stack exceeded MAX_HISTORY");
        check(stack.pop() == 4, "Circular top should be newest item");
        check(stack.pop() == 3, "Circular second item failed");
        check(stack.pop() == 2, "Circular oldest retained item failed");
    }

    private static void testClear() {
        MyStack<Integer> stack = new MyStack<>();
        stack.push(1);
        stack.push(2);
        stack.clear();

        check(stack.isEmpty(), "clear() must empty the stack");
        check(stack.size() == 0, "clear() must reset size to 0");

        stack.push(9);
        check(stack.pop() == 9, "stack must work after clear()");
    }

    private static void testBoundedLinkedStack() {
        BoundedLinkedStack<Integer> stack = new BoundedLinkedStack<>(3);

        stack.push(1);
        stack.push(2);
        stack.push(3);
        stack.push(4); // oldest 1 is discarded

        check(stack.size() == 3, "Bounded linked stack exceeded capacity");
        check(stack.pop() == 4, "Bounded linked top should be newest item");
        check(stack.pop() == 3, "Bounded linked second item failed");
        check(stack.pop() == 2, "Bounded linked oldest retained item failed");
        check(stack.isEmpty(), "Bounded linked stack should be empty");

        BoundedLinkedStack<Integer> one = new BoundedLinkedStack<>(1);
        one.push(7);
        one.push(8);
        check(one.size() == 1 && one.pop() == 8, "Capacity 1 must keep newest item");
    }

    // Both bounded stacks must behave identically (same MAX_HISTORY policy).
    private static void testBothBoundedStacksAgree() {
        java.util.Random random = new java.util.Random(1);
        BoundedLinkedStack<Integer> linked = new BoundedLinkedStack<>(5);
        BoundedCircularStack<Integer> circular = new BoundedCircularStack<>(5);

        for (int i = 0; i < 10000; i++) {
            if (random.nextInt(100) < 60 || linked.isEmpty()) {
                linked.push(i);
                circular.push(i);
            } else {
                check(linked.pop().equals(circular.pop()), "Bounded stacks disagree on pop");
            }
            check(linked.size() == circular.size(), "Bounded stacks disagree on size");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
