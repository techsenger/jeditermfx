
module com.techsenger.jeditermfx.ui {
    requires com.techsenger.jeditermfx.core;
    requires pty4j;
    requires purejavacomm;
    requires org.jetbrains.annotations;
    requires org.slf4j;
    requires java.desktop;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;

    exports com.techsenger.jeditermfx.ui;
    exports com.techsenger.jeditermfx.ui.settings;
}
