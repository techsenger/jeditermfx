package com.techsenger.jeditermfx.core;

import com.techsenger.jeditermfx.core.TerminalKeyEncoder;
import com.techsenger.jeditermfx.core.util.Platform;
import com.techsenger.jeditermfx.core.input.InputEvent;
import com.techsenger.jeditermfx.core.input.KeyEvent;
import com.techsenger.jeditermfx.core.util.Ascii;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TerminalKeyEncoderTest {

    @Test
    public void altBackspace() {
        assertKeyCode(Ascii.BS, InputEvent.ALT_MASK, new byte[]{Ascii.ESC, Ascii.DEL});
    }

    @Test
    public void altLeft() {
        String expected;
        if (Platform.isMacOS())
            expected = prependEsc("b");
        else
            expected = prependEsc("[1;3D");
        assertKeyCode(KeyEvent.VK_LEFT, InputEvent.ALT_MASK, expected);
    }

    @Test
    public void shiftLeft() {
        assertKeyCode(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK, prependEsc("[1;2D"));
    }

    @Test
    public void shiftLeftApplication() {
        assertKeyCode(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK, prependEsc("[1;2D"));
    }

    @Test
    public void controlF1() {
        assertKeyCode(KeyEvent.VK_F1, InputEvent.CTRL_MASK, prependEsc("[1;5P"));
    }

    @Test
    public void controlF11() {
        assertKeyCode(KeyEvent.VK_F11, InputEvent.CTRL_MASK, prependEsc("[23;5~"));
    }

    private void assertKeyCode(int key, int modifiers, String expectedKeyCodeStr) {
        assertKeyCode(key, modifiers, expectedKeyCodeStr.getBytes(StandardCharsets.UTF_8));
    }

    private void assertKeyCode(int key, int modifiers, byte[] expectedKeyCode) {
        var keyEncoder = new TerminalKeyEncoder();
        var actual = keyEncoder.getCode(key, modifiers);
        assertArrayEquals(expectedKeyCode, actual);
    }

    private String prependEsc(String str) {
        return Ascii.ESC_CHAR + str;
    }
}
