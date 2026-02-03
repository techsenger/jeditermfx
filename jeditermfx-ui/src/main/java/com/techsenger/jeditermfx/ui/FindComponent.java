package com.techsenger.jeditermfx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class FindComponent {

    private final HBox pane = new HBox();

    private final TextField textField = new TextField();

    private final Label label = new Label();

    private final CheckBox ignoreCaseCheckBox = new CheckBox("Ignore Case");

    public FindComponent(JediTermFxWidget jediTermWidget) {
        this.ignoreCaseCheckBox.setSelected(true);
        Button next = new Button("\u25BC");
        next.setOnAction(e -> onResultUpdated(jediTermWidget.getTerminalPanel().selectNextFindResultItem()));
        Button prev = new Button("\u25B2");
        prev.setOnAction(e -> onResultUpdated(jediTermWidget.getTerminalPanel().selectPrevFindResultItem()));
        var charSize = jediTermWidget.myTerminalPanel.myCharSize;
        HBox.setHgrow(textField, Priority.ALWAYS);
        pane.setMaxSize(
                charSize.getWidth() * 60,
                charSize.getHeight() + 14);
        pane.setAlignment(Pos.CENTER_LEFT);
        pane.setPadding(new Insets(0, charSize.getWidth() / 2, 0, charSize.getWidth() / 2));
        pane.setStyle("-fx-background-color: -fx-background");
        textField.setEditable(true);
        updateLabel(null);
        this.pane.getChildren().add(textField);
        this.pane.getChildren().add(ignoreCaseCheckBox);
        HBox.setMargin(ignoreCaseCheckBox, new Insets(0, charSize.getWidth(), 0, charSize.getWidth()));
        this.pane.getChildren().add(label);
        this.pane.getChildren().add(next);
        this.pane.getChildren().add(prev);
        this.pane.focusedProperty().addListener((ov, oldV, newV) -> {
            if (newV) {
                this.textField.requestFocus();
            }
        });
    }

    TextField getTextField() {
        return textField;
    }

    CheckBox getIgnoreCaseCheckBox() {
        return ignoreCaseCheckBox;
    }

    private void updateLabel(@Nullable FindResult result) {
        if (result == null) {
            label.setText("");
        } else if (!result.getMatches().isEmpty()) {
            FindResult.Match selectedItem = result.selectedMatch();
            label.setText(selectedItem.getIndex() + " of " + result.getMatches().size());
        }
    }

    void onResultUpdated(FindResult results) {
        updateLabel(results);
    }

    @NotNull Pane getPane() {
        return this.pane;
    }

    void requestFocus() {
        textField.requestFocus();
    }
}
