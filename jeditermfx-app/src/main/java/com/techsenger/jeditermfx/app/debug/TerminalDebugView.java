package com.techsenger.jeditermfx.app.debug;

import com.techsenger.jeditermfx.app.pty.LoggingTtyConnector;
import com.techsenger.jeditermfx.ui.TerminalSession;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

public class TerminalDebugView {

    @NotNull
    private final TerminalSession terminal;

    @NotNull
    private final LoggingTtyConnector loggingTtyConnector;

    @NotNull
    private final ComboBox<DebugBufferType> typeComboBox = new ComboBox();

    @NotNull
    private final ControlSequenceSettingsView controlSequenceSettingsView = new ControlSequenceSettingsView();

    @NotNull
    private final Slider slider;

    @NotNull
    private final Spinner<Integer> spinner;

    @NotNull
    private final VBox resultPanel;

    @NotNull
    private final Timeline timeline;

    @NotNull
    private final List<StateChangeListener> listeners = new CopyOnWriteArrayList();

    public static final int INITIAL = -1;

    public TerminalDebugView(@NotNull TerminalSession terminal) {
        typeComboBox.getItems().addAll(DebugBufferType.values());
        typeComboBox.setValue(DebugBufferType.Screen);
        Objects.requireNonNull(terminal, "terminal");
        this.terminal = terminal;
        loggingTtyConnector = (LoggingTtyConnector) terminal.getTtyConnector();
        var viewArea = createViewArea();
        var controlSequencesArea = createControlSequenceArea();
        slider = createSlider();
        spinner = new Spinner();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 0));
        resultPanel = createResultPanel(viewArea, controlSequencesArea);
        typeComboBox.getSelectionModel().selectedItemProperty().addListener((ov, oldV, newV) -> update());
        slider.valueProperty().addListener((ov, oldV, newV) -> update());
        spinner.valueProperty().addListener((ov, oldV, newV) -> {
            slider.setValue((int) spinner.getValue());
            //update();
        });
        update();
        timeline = new Timeline(new KeyFrame(Duration.millis(1000), (a) -> update()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private TextArea createViewArea() {
        var viewArea = createTextArea();
        addListener(new StateChangeListener() {

            private String myLastText = null;

            @Override
            public void stateChanged(DebugBufferType type, ControlSequenceSettings controlSequenceSettings, int stateIndex) {
                var text = type.getValue(terminal, stateIndex);
                if (text != myLastText) {
                    viewArea.setText(text);
                    myLastText = text;
                }
            }
        });
        return viewArea;
    }

    private TextArea createControlSequenceArea() {
        var controlSequenceArea = createTextArea();
        addListener(new StateChangeListener() {

            private String myLastText = null;

            @Override
            public void stateChanged(DebugBufferType type, ControlSequenceSettings controlSequenceSettings, int stateIndex) {
                var controlSequencesVisualization = getControlSequencesVisualization(controlSequenceSettings, stateIndex);
                if (controlSequencesVisualization != myLastText) {
                    controlSequenceArea.setText(controlSequencesVisualization);
                    myLastText = controlSequencesVisualization;
                }
                controlSequenceArea.setWrapText(controlSequenceSettings.isWrapLines());
            }
        });
        return controlSequenceArea;
    }

    private Slider createSlider() {
        var slider = new Slider(INITIAL, INITIAL, INITIAL);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(10);
        slider.setMinorTickCount(1);
        slider.setBlockIncrement(1);
        return slider;
    }

    private void update() {
        DebugBufferType type = typeComboBox.getSelectionModel().getSelectedItem();
        var newMinimum = loggingTtyConnector.getLogStart();
        var newMaximum = newMinimum + loggingTtyConnector.getChunks().size();
        var initialize = slider.getValue() == INITIAL;
        slider.setMin(newMinimum);
        slider.setMax(newMaximum);
        if (initialize) {
            slider.setValue(newMaximum);
        }
        syncSliderToSpinner();
        var stateIndex = slider.getValue() - slider.getMin();
        var controlSequenceSettings = controlSequenceSettingsView.get();
        for (var listener : listeners) {
            listener.stateChanged(type, controlSequenceSettings, (int) stateIndex);
        }
    }

    private void syncSliderToSpinner() {
        var spinnerValueFactory = (SpinnerValueFactory.IntegerSpinnerValueFactory) spinner.getValueFactory();
        if (!Objects.equals(spinnerValueFactory.getValue(), (int) slider.getValue())
                || !Objects.equals(spinnerValueFactory.getMin(), (int) slider.getMin())
                || !Objects.equals(spinnerValueFactory.getMin(), (int) slider.getMax())) {
            this.spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory((int) slider.getMin(),
                    (int) slider.getMax(), (int) slider.getValue(), 1));
        }
    }

    private void addListener(StateChangeListener listener) {
        listeners.add(listener);
    }

    private String getControlSequencesVisualization(ControlSequenceSettings settings, int stateIndex) {
        var chunks = loggingTtyConnector.getChunks().subList(0, stateIndex);
        return ControlSequenceVisualizer.getVisualizedString(loggingTtyConnector.getLogStart(), chunks, settings);
    }

    public Pane getPane() {
        return resultPanel;
    }

    public void stop() {
        timeline.stop();
    }

    private static interface StateChangeListener {
        void stateChanged(DebugBufferType type, ControlSequenceSettings controlSequenceSettings, int stateIndex);
    }

    private class ControlSequenceSettingsView {

        @NotNull
        private CheckBox showChunkId = new CheckBox("Show chunk id");

        @NotNull
        private CheckBox useTeseq = new CheckBox("Use teseq");

        @NotNull
        private CheckBox showInvisibleCharacters = new CheckBox("Show invisible characters");

        @NotNull
        private CheckBox wrapLines = new CheckBox("Wrap lines");

        @NotNull
        HBox pane = new HBox(showChunkId, useTeseq, showInvisibleCharacters, wrapLines);

        public ControlSequenceSettingsView() {
            this.showChunkId.setSelected(true);
            this.showChunkId.selectedProperty().addListener((ov, oldV, newV) -> update());
            this.useTeseq.selectedProperty().addListener((ov, oldV, newV) -> update());
            this.showInvisibleCharacters.setSelected(true);
            this.showInvisibleCharacters.selectedProperty().addListener((ov, oldV, newV) -> update());
            this.wrapLines.setSelected(true);
            this.wrapLines.selectedProperty().addListener((ov, oldV, newV) -> update());
            pane.setSpacing(10);
            pane.setPadding(new Insets(0, 10, 0, 10));
            pane.setAlignment(Pos.CENTER_LEFT);
        }

        ControlSequenceSettings get() {
            return new ControlSequenceSettings(showChunkId.isSelected(), useTeseq.isSelected(),
                    showInvisibleCharacters.isSelected(), wrapLines.isSelected());
        }
    }

    private HBox createTopPanel() {
        var panel = new HBox(typeComboBox, controlSequenceSettingsView.pane);
        HBox.setHgrow(typeComboBox, Priority.ALWAYS);
        typeComboBox.setMaxWidth(Double.MAX_VALUE);
        panel.setAlignment(Pos.CENTER_LEFT);
        return panel;
    }

    private VBox createResultPanel(TextArea viewArea, TextArea controlSequencesArea) {
        var splitPane = new SplitPane(viewArea, controlSequencesArea);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        var resultPanel = new VBox(createTopPanel(), splitPane, createBottomPanel(slider, spinner));
        return resultPanel;
    }

    private static HBox createBottomPanel(Slider slider, Spinner spinner) {
        var stateIndexPanel = new HBox(slider, spinner);
        HBox.setHgrow(slider, Priority.ALWAYS);
        stateIndexPanel.setAlignment(Pos.CENTER_LEFT);
        return stateIndexPanel;
    }

    private static TextArea createTextArea() {
        var area = new TextArea();
        area.setEditable(false);
        area.setFont(Font.font("Monospaced", FontWeight.NORMAL, 14));
        return area;
    }

}
