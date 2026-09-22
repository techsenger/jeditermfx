
package com.techsenger.jeditermfx.core.emulator;

import com.techsenger.jeditermfx.core.util.Ascii;
import com.techsenger.jeditermfx.core.TerminalDataStream;
import com.techsenger.jeditermfx.core.util.CharUtils;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SystemCommandSequence {

    @NotNull
    private final String text;

    @NotNull
    private final List<String> args;

    private static final char ST = '\u009c';

    private static final char ARG_SEPARATOR = ';';

    public static final char OSC = '\u009d';

    public SystemCommandSequence(@NotNull TerminalDataStream stream) throws IOException {
        super();
        Objects.requireNonNull(stream, "stream");
        var textBuf = new StringBuilder();
        do {
            textBuf.append(stream.getChar());
        } while (!isTerminated(textBuf));
        text = textBuf.toString();
        var body = text.substring(0, text.length() - terminatorLength(text));
        var splits = body.split(Pattern.quote(String.valueOf(ARG_SEPARATOR)), -1);
        args = List.of(splits);
    }

    @NotNull
    public final List<String> getArgs() {
        return this.args;
    }

    @Nullable
    public final String getStringAt(int index) {
        return (String) CollectionsKt.getOrNull(this.args, index);
    }

    public int getIntAt(int index, int defaultValue) {
        return parseInt(getStringAt(index), defaultValue);
    }

    private int parseInt(String value, int defaultValue) {
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public String format(List<String> args) {
        // https://invisible-island.net/xterm/ctlseqs/ctlseqs.html#h3-Operating-System-Commands
        // XTerm accepts either BEL  or ST  for terminating OSC
        // sequences, and when returning information, uses the same
        // terminator used in a query.
        return Ascii.ESC_CHAR + "]" + String.join(String.valueOf(ARG_SEPARATOR), args) +
                text.substring(text.length() - terminatorLength(text));
    }

    @Override
    public String toString() {
        return CharUtils.toHumanReadableText(Ascii.ESC_CHAR + "]" + text);
    }

    private static boolean isTerminated(CharSequence text) {
        var len = text.length();
        if (len > 0) {
            var ch = text.charAt(len - 1);
            return ch == Ascii.BEL_CHAR || ch == ST || isTwoBytesTerminator(text, len, ch);
        }
        return false;
    }

    private static boolean isTwoBytesTerminator(CharSequence text, int length, char lastChar) {
        return lastChar == '\\' && length >= 2 && text.charAt(length - 2) == Ascii.ESC_CHAR;
    }

    private static int terminatorLength(CharSequence text) {
        var length = text.length();
        if (isTwoBytesTerminator(text, length, text.charAt(length - 1))) {
            return 2;
        } else {
            return 1;
        }
    }
}
