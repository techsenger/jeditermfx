package com.techsenger.jeditermfx.ui;

import com.techsenger.jeditermfx.core.model.CharBuffer;
import org.jetbrains.annotations.NotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * @author traff
 */
public class SubstringFinderTest {

    @Test
    public void test0() {
        doTest("abc", "abc");
    }

    @Test
    public void test2() {
        doTest("abc", "xa", "bc");
    }

    @Test
    public void test3() {
        doTest("abc", "xyza", "ba", "bcd");
    }

    @Test
    public void test4() {
        doTest("abcdef", "xxxxxxxxxxxxxxxxxxxxxxxxxxx", "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyabc", "defzzzzzzzzzzzzzzzzzzzzzz");
    }

    @Test
    public void test5() {
        doTest("abc", "xxxxxxxxxxxxabcxxxxxxxxxxxxxxx");
    }

    @Test
    public void test6() {
        SubstringFinder.FindResultImpl res = getFindResult("aba", "abacaba");
        assertEquals(2, res.getMatches().size());
        for (int i = 0; i < res.getMatches().size(); i++) {
            assertEquals("aba", res.getMatches().get(i).getText());
        }
    }

    @Test
    public void test7() {
        SubstringFinder.FindResultImpl res = getFindResult("aa", "aaaa");
        //after a pattern is matched we start from the next character
        assertEquals(2, res.getMatches().size());
        for (int i = 0; i < res.getMatches().size(); i++) {
            assertEquals("aa", res.getMatches().get(i).getText());
        }
    }

    @Test
    public void test8() {
        SubstringFinder.FindResultImpl res = getFindResult("aaa", "aa", "aa", "aa");
        //after a pattern is matched we start from the next character
        assertEquals(2, res.getMatches().size());
        for (int i = 0; i < res.getMatches().size(); i++) {
            assertEquals("aaa", res.getMatches().get(i).getText());
        }
    }

    @Test
    public void test9() {
        doTest("2Menu", " 2", "Menu ");
    }

    @Test
    public void test10() {
        doTest("git log", "g", "i", "t", " ", "l", "o", "g");
    }

    @Test
    public void test11() {
        doTest("Hello World", "print('Hello", " ", "World')");
    }

    @Test
    public void testIgnoreCase() {
        SubstringFinder.FindResultImpl res = getFindResult("abc", " ABC ");
        //after a pattern is matched we start from the next character
        assertEquals(1, res.getMatches().size());
        assertEquals("ABC", res.getMatches().get(0).getText());
    }

    private void doTest(String patter, String... strings) {
        SubstringFinder.FindResultImpl res = getFindResult(patter, strings);

        assertEquals(1, res.getMatches().size());
        assertEquals(patter, res.getMatches().get(0).getText());
    }

    @NotNull
    private static SubstringFinder.FindResultImpl getFindResult(@NotNull String patter, String... strings) {
        SubstringFinder f = new SubstringFinder(patter, true);
        for (String string : strings) {
            CharBuffer cb = new CharBuffer(string);

            for (int j = 0; j < cb.length(); j++) {
                f.nextChar(0, 0, cb, j);
            }
        }

        return (SubstringFinder.FindResultImpl) f.getResult();
    }
}
