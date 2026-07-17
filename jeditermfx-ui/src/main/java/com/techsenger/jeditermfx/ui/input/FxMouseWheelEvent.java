package com.techsenger.jeditermfx.ui.input;

import javafx.scene.input.ScrollEvent;
import com.techsenger.jeditermfx.core.input.MouseWheelEvent;
import org.jetbrains.annotations.NotNull;
import com.techsenger.jeditermfx.core.emulator.mouse.MouseButtonCodes;
import com.techsenger.jeditermfx.core.emulator.mouse.MouseButtonModifierFlags;

public final class FxMouseWheelEvent extends MouseWheelEvent {

    private static int createButtonCode(@NotNull ScrollEvent fxMouseEvent) {
        // JavaFX deltaY > 0 means scrolling up, the opposite of AWT's getWheelRotation() > 0
        double deltaY = fxMouseEvent.getDeltaY();
        if (deltaY > 0) {
            return MouseButtonCodes.SCROLLDOWN;
        } else if (deltaY < 0) {
            return MouseButtonCodes.SCROLLUP;
        } else {
            return MouseButtonCodes.NONE; // Ignore deltaY=0 events
        }
    }

    private static int getModifierKeys(@NotNull ScrollEvent fxMouseEvent) {
        int modifier = 0;
        if (fxMouseEvent.isControlDown()) {
            modifier |= MouseButtonModifierFlags.MOUSE_BUTTON_CTRL_FLAG;
        }
        if (fxMouseEvent.isShiftDown()) {
            modifier |= MouseButtonModifierFlags.MOUSE_BUTTON_SHIFT_FLAG;
        }
        if (fxMouseEvent.isMetaDown()) {
            modifier |= MouseButtonModifierFlags.MOUSE_BUTTON_META_FLAG;
        }
        return modifier;
    }

    private final ScrollEvent myFxMouseWheelEvent;

    public FxMouseWheelEvent(@NotNull ScrollEvent fxMouseWheelEvent) {
        super(createButtonCode(fxMouseWheelEvent), getModifierKeys(fxMouseWheelEvent));
        myFxMouseWheelEvent = fxMouseWheelEvent;
    }

    @Override
    public String toString() {
        return myFxMouseWheelEvent.toString();
    }
}
