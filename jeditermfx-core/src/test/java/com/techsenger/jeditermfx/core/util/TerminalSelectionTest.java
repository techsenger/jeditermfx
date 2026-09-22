package com.techsenger.jeditermfx.core.util;

import com.techsenger.jeditermfx.core.compatibility.Point;
import com.techsenger.jeditermfx.core.model.TerminalSelection;
import com.techsenger.jeditermfx.core.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author traff
 */
public class TerminalSelectionTest {

    @Test
    public void testSameRow() {
        TerminalSelection s = new TerminalSelection(new Point(2, 1), new Point(4, 1));
        Pair<Integer, Integer> intersection = s.intersect(3, 1, 1);
        doTest(intersection, 3, 1);
    }

    @Test
    public void testSameRow2() {
        TerminalSelection s = new TerminalSelection(new Point(2, 1), new Point(4, 1));
        Pair<Integer, Integer> intersection = s.intersect(3, 1, 10);
        doTest(intersection, 3, 2);
    }

    @Test
    public void testSameRow3() {
        TerminalSelection s = new TerminalSelection(new Point(2, 1), new Point(4, 1));
        Pair<Integer, Integer> intersection = s.intersect(1, 1, 10);
        doTest(intersection, 2, 3);
    }

    @Test
    public void testSameRowNotIntersect() {
        TerminalSelection s = new TerminalSelection(new Point(2, 1), new Point(4, 1));
        Pair<Integer, Integer> intersection = s.intersect(1, 1, 1);
        Assertions.assertNull(intersection);
    }

    @Test
    public void testEndRow() {
        TerminalSelection s = new TerminalSelection(new Point(5, 1), new Point(4, 2));
        Pair<Integer, Integer> intersection = s.intersect(2, 2, 10);
        doTest(intersection, 2, 3);
    }

    @Test
    public void testStartRow() {
        TerminalSelection s = new TerminalSelection(new Point(5, 1), new Point(4, 2));
        Pair<Integer, Integer> intersection = s.intersect(5, 1, 10);
        doTest(intersection, 5, 10);
    }

    @Test
    public void testStartRowUnsorted() {
        TerminalSelection s = new TerminalSelection(new Point(4, 2), new Point(5, 1));
        Pair<Integer, Integer> intersection = s.intersect(5, 1, 10);
        doTest(intersection, 5, 10);
    }

    @Test
    public void testRowOut() {
        TerminalSelection s = new TerminalSelection(new Point(5, 1), new Point(4, 2));
        Pair<Integer, Integer> intersection = s.intersect(5, 3, 10);
        Assertions.assertNull(intersection);
    }

    @Test
    public void testRowOut2() {
        TerminalSelection s = new TerminalSelection(new Point(2, 2), new Point(4, 2));
        Pair<Integer, Integer> intersection = s.intersect(5, 1, 10);
        Assertions.assertNull(intersection);
    }

    @Test
    public void testConsRows() {
        TerminalSelection s = new TerminalSelection(new Point(5, 2), new Point(5, 3));
        Pair<Integer, Integer> intersection = s.intersect(0, 2, 20);
        doTest(intersection, 5, 15);
    }

    private void doTest(Pair<Integer, Integer> intersection, int x, int len) {
        Assertions.assertTrue(x == intersection.getFirst() && len == intersection.getSecond(),
                intersection.toString() + " instead of " + x + ", " + len);
    }
}
