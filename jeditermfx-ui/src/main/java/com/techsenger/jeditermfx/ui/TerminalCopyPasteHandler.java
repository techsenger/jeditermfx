package com.techsenger.jeditermfx.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TerminalCopyPasteHandler {

    void setContents(@NotNull String text, boolean useSystemSelectionClipboardIfAvailable);

    @Nullable
    String getContents(boolean useSystemSelectionClipboardIfAvailable);
}
