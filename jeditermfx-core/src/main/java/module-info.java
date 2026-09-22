
module com.techsenger.jeditermfx.core {
    requires org.jetbrains.annotations;
    requires org.slf4j;
    requires java.desktop;

    exports com.techsenger.jeditermfx.core;
    exports com.techsenger.jeditermfx.core.compatibility;
    exports com.techsenger.jeditermfx.core.emulator;
    exports com.techsenger.jeditermfx.core.emulator.charset;
    exports com.techsenger.jeditermfx.core.emulator.mouse;
    exports com.techsenger.jeditermfx.core.input;
    exports com.techsenger.jeditermfx.core.model;
    exports com.techsenger.jeditermfx.core.model.hyperlinks;
    exports com.techsenger.jeditermfx.core.typeahead;
    exports com.techsenger.jeditermfx.core.util;
}
