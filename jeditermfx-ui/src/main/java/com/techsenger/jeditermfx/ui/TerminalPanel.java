package com.techsenger.jeditermfx.ui;

import com.techsenger.jeditermfx.core.Color;
import com.techsenger.jeditermfx.core.CursorShape;
import com.techsenger.jeditermfx.core.HyperlinkStyle;
import com.techsenger.jeditermfx.core.RequestOrigin;
import com.techsenger.jeditermfx.core.StyledTextConsumer;
import com.techsenger.jeditermfx.core.TerminalColor;
import com.techsenger.jeditermfx.core.TerminalCoordinates;
import com.techsenger.jeditermfx.core.TerminalDisplay;
import com.techsenger.jeditermfx.core.TerminalOutputStream;
import com.techsenger.jeditermfx.core.TerminalStarter;
import com.techsenger.jeditermfx.core.TextStyle;
import com.techsenger.jeditermfx.core.TextStyle.Option;
import com.techsenger.jeditermfx.core.compatibility.Point;
import com.techsenger.jeditermfx.core.emulator.ColorPalette;
import com.techsenger.jeditermfx.core.emulator.charset.CharacterSets;
import com.techsenger.jeditermfx.core.emulator.mouse.MouseFormat;
import com.techsenger.jeditermfx.core.emulator.mouse.MouseMode;
import com.techsenger.jeditermfx.core.emulator.mouse.TerminalMouseListener;
import com.techsenger.jeditermfx.core.model.CharBuffer;
import com.techsenger.jeditermfx.core.model.JediTerminal;
import com.techsenger.jeditermfx.core.model.LinesBuffer;
import com.techsenger.jeditermfx.core.model.SelectionUtil;
import com.techsenger.jeditermfx.core.model.StyleState;
import com.techsenger.jeditermfx.core.model.TerminalLine;
import com.techsenger.jeditermfx.core.model.TerminalLineIntervalHighlighting;
import com.techsenger.jeditermfx.core.model.TerminalModelListener;
import com.techsenger.jeditermfx.core.model.TerminalSelection;
import com.techsenger.jeditermfx.core.model.TerminalTextBuffer;
import com.techsenger.jeditermfx.core.model.hyperlinks.LinkInfo;
import com.techsenger.jeditermfx.core.typeahead.TerminalTypeAheadManager;
import com.techsenger.jeditermfx.core.util.CharUtils;
import static com.techsenger.jeditermfx.core.util.Platform.isWindows;
import com.techsenger.jeditermfx.core.util.TermSize;
import com.techsenger.jeditermfx.ui.hyperlinks.LinkInfoEx;
import com.techsenger.jeditermfx.ui.input.FxMouseEvent;
import com.techsenger.jeditermfx.ui.input.FxMouseWheelEvent;
import com.techsenger.jeditermfx.ui.settings.SettingsProvider;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.text.AttributedCharacterIterator;
import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Dimension2D;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.InputMethodTextRun;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminalPanel implements TerminalDisplay, TerminalActionProvider {

    private static final Logger logger = LoggerFactory.getLogger(TerminalPanel.class);

    private static final long serialVersionUID = -1048763516632093014L;

    public static final double SCROLL_SPEED = 0.05;

    private static class CanvasPane extends Pane {

        private final Canvas canvas;

        public CanvasPane(Canvas canvas) {
            this.canvas = canvas;
            getChildren().addAll(canvas);
            canvas.widthProperty().bind(this.widthProperty());
            canvas.heightProperty().bind(this.heightProperty());
        }
    }

    private final Canvas canvas = new Canvas();

    private final CanvasPane canvasPane = new CanvasPane(canvas);

    private final GraphicsContext graphicsContext = canvas.getGraphicsContext2D();

    //we scroll a window [0, terminal_height] in the range [-history_lines_count, terminal_height]
    private final ScrollBar scrollBar = new ScrollBar();

    /**
     * From TerminalTextBuffer: scrollOrigin row where a scrolling window starts, should be in the range
     * [-history_lines_count, 0].
     */
    protected int swingClientScrollOrigin;

    private boolean scrollBarThumbVisible = true;

    private final HBox pane = new HBox(canvasPane, scrollBar);

    private ContextMenu popup;

    /*font related*/
    private Font myNormalFont;

    private Font myItalicFont;

    private Font myBoldFont;

    private Font myBoldItalicFont;

    private double myDescent = 0;

    private int mySpaceBetweenLines = 0;

    protected Dimension2D myCharSize;

    private TermSize myTermSize;

    private boolean myInitialSizeSyncDone = false;

    private TerminalStarter myTerminalStarter = null;

    private MouseMode myMouseMode = MouseMode.MOUSE_REPORTING_NONE;

    private Point mySelectionStartPoint = null;

    private final ObjectProperty<TerminalSelection> mySelection = new SimpleObjectProperty<>(null);

    private final ReadOnlyStringWrapper selectedText = new ReadOnlyStringWrapper(null);

    /**
     * The value of the selection property has two types: 1)The user selects text with the mouse. 2) The value is set
     * programmatically by a client of this class. In the first case, selectedText is updated only when the user
     * has finished selecting text with the mouse. In the second case, selectedText changes immediately after the
     * selection property is modified. The value of this variable is used to control the timing of the selectedText
     * update.
     */
    private boolean updateSelectedText = true;

    private final TerminalCopyPasteHandler myCopyPasteHandler;

    private final SettingsProvider mySettingsProvider;

    private final TerminalTextBuffer myTerminalTextBuffer;

    final private StyleState myStyleState;

    /*scroll and cursor*/
    final private TerminalCursor myCursor = new TerminalCursor();

    private final BlinkingTextTracker myTextBlinkingTracker = new BlinkingTextTracker();

    private boolean myScrollingEnabled = true;

    private String myWindowTitle = "Terminal";

    private TerminalActionProvider myNextActionProvider;

    private String myInputMethodUncommittedChars;

    private Timeline myRepaintTimeLine;

    private final AtomicInteger scrollDy = new AtomicInteger(0);

    private final AtomicBoolean myHistoryBufferLineCountChanged = new AtomicBoolean(false);

    private final AtomicBoolean needRepaint = new AtomicBoolean(true);

    private int myMaxFPS = 50;

    private int myBlinkingPeriod = 500;

    private TerminalCoordinates myCoordsAccessor;

    private FindResult myFindResult;

    private final BooleanProperty findResultHighlighted = new SimpleBooleanProperty(true);

    private LinkInfo myHoveredHyperlink = null;

    private Cursor myCursorType = Cursor.DEFAULT;

    private LinkInfoEx.HoverConsumer myLinkHoverConsumer;

    private TerminalTypeAheadManager myTypeAheadManager;

    private volatile boolean myBracketedPasteMode;

    private boolean myUsingAlternateBuffer = false;

    private boolean myFillCharacterBackgroundIncludingLineSpacing;

    private @Nullable TextStyle myCachedSelectionColor;

    private @Nullable TextStyle myCachedFoundPatternColor;

    private boolean myIgnoreNextKeyTypedEvent;

    public TerminalPanel(@NotNull SettingsProvider settingsProvider, @NotNull TerminalTextBuffer terminalTextBuffer,
                         @NotNull StyleState styleState) {
        mySettingsProvider = settingsProvider;
        myTerminalTextBuffer = terminalTextBuffer;
        myStyleState = styleState;
        myTermSize = new TermSize(terminalTextBuffer.getWidth(), terminalTextBuffer.getHeight());
        myMaxFPS = mySettingsProvider.maxRefreshRate();
        myCopyPasteHandler = createCopyPasteHandler();
        var css = TerminalPanel.class.getResource("terminal-panel.css").toExternalForm();
        this.pane.getStylesheets().add(css);
        setScrollBarRangeProperties(0, 80, 0, 80);
        updateScrolling(true);
        terminalTextBuffer.addModelListener(() -> repaint());
        terminalTextBuffer.addTypeAheadModelListener(() -> repaint());
        terminalTextBuffer.addHistoryBufferListener(() -> myHistoryBufferLineCountChanged.set(true));
        mySelection.addListener((ov, oldV, newV) -> updateSelectedText());
    }

    public ObjectProperty<TerminalSelection> selectionProperty() {
        return mySelection;
    }

    /**
     * Returns selected text property. The text is determined only when the user has finished making a selection.
     * This is done for performance reasons.
     *
     * @return
     */
    public ReadOnlyStringProperty selectedTextProperty() {
        return selectedText.getReadOnlyProperty();
    }

    public BooleanProperty findResultHighlightedProperty() {
        return findResultHighlighted;
    }

    public Pane getPane() {
        return pane;
    }

    public ScrollBar getScrollBar() {
        return this.scrollBar;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    void setTypeAheadManager(@NotNull TerminalTypeAheadManager typeAheadManager) {
        myTypeAheadManager = typeAheadManager;
    }

    @NotNull
    protected TerminalCopyPasteHandler createCopyPasteHandler() {
        return new DefaultTerminalCopyPasteHandler();
    }

    protected void reinitFontAndResize() {
        initFont();
        sizeTerminalFromComponent();
    }

    protected void initFont() {
        myNormalFont = createFont();
        myBoldFont = Font.font(myNormalFont.getFamily(), FontWeight.BOLD, myNormalFont.getSize());
        myItalicFont = Font.font(myNormalFont.getFamily(), FontPosture.ITALIC, myNormalFont.getSize());
        myBoldItalicFont = Font.font(myNormalFont.getFamily(), FontWeight.BOLD, FontPosture.ITALIC, myNormalFont.getSize());
        establishFontMetrics();
    }

    public void init() {
        initFont();
        HBox.setHgrow(canvasPane, Priority.ALWAYS);
        this.canvasPane.setPrefHeight(getPixelHeight());
        this.canvasPane.setPrefWidth(getPixelWidth());
        this.canvas.setFocusTraversable(true);
        this.canvas.setCache(true);
        this.scrollBar.setOrientation(Orientation.VERTICAL);
        if (mySettingsProvider.useAntialiasing()) {
            //Important! FontSmoothingType.LCD is very slow
            graphicsContext.setFontSmoothingType(FontSmoothingType.GRAY);
            graphicsContext.setImageSmoothing(true);
        } else {
            graphicsContext.setImageSmoothing(false);
        }
        this.canvas.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isConsumed()) {
                return;
            }
            myIgnoreNextKeyTypedEvent = false;
            if (TerminalAction.processEvent(TerminalPanel.this, e) || processTerminalKeyPressed(e)) {
                e.consume();
                myIgnoreNextKeyTypedEvent = true;
            }
        });
        this.canvas.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (e.isConsumed()) {
                return;
            }
            if (myIgnoreNextKeyTypedEvent || processTerminalKeyTyped(e)) {
                e.consume();
            }
        });
        this.canvas.addEventHandler(MouseEvent.MOUSE_MOVED, (e) -> {
            handleHyperlinks(createPoint(e));
        });
        this.canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            doOnMouseDragged(e);
        });
        this.canvas.addEventHandler(ScrollEvent.SCROLL, e -> {
            if (isLocalMouseAction(e)) {
                handleMouseWheelEvent(e);
            }
        });
        this.canvas.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            if (myLinkHoverConsumer != null) {
                myLinkHoverConsumer.onMouseExited();
                myLinkHoverConsumer = null;
            }
            updateHoveredHyperlink(null);
        });
        this.canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (e.getClickCount() == 1) {
                    mySelectionStartPoint = panelToCharCoords(createPoint(e));
                    updateSelection(null, true);
                    repaint();
                }
            }
        });
        this.canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            this.canvas.requestFocus();
            if (mySelection.get() != null) {
                updateSelectedText();
            }
            repaint();
        });
        this.canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            doOnMouseClicked(e);
        });
        this.canvas.inputMethodRequestsProperty().set(new MyInputMethodRequests());
        this.canvas.setOnInputMethodTextChanged(e -> processInputMethodEvent(e));
        this.canvasPane.widthProperty().addListener((ov, oldV, newV) -> {
            sizeTerminalFromComponent();
        });
        this.canvasPane.heightProperty().addListener((ov, oldV, newV) -> {
            sizeTerminalFromComponent();
        });
        myFillCharacterBackgroundIncludingLineSpacing
                = mySettingsProvider.shouldFillCharacterBackgroundIncludingLineSpacing();
        this.canvas.focusedProperty().addListener((ov, oldV, newV) -> {
            if (newV) {
                myFillCharacterBackgroundIncludingLineSpacing =
                        mySettingsProvider.shouldFillCharacterBackgroundIncludingLineSpacing();
                myCursor.cursorChanged();
            } else {
                myCursor.cursorChanged();
                //TODO
                //handleHyperlinks(new Point2D(?, ?));
            }
        });
        this.scrollBar.valueProperty().addListener((ov, oldV, newV) -> {
            this.swingClientScrollOrigin = resolveSwingScrollBarValue();
            repaint();
        });
        createRepaintTimer();
    }

    private void doOnMouseDragged(MouseEvent e) {
        if (!isLocalMouseAction(e)) {
            return;
        }
        final Point charCoords = panelToCharCoords(createPoint(e));
        if (mySelection.get() == null) {
            // prevent unlikely case where drag started outside terminal panel
            if (mySelectionStartPoint == null) {
                mySelectionStartPoint = charCoords;
            }
            updateSelection(new TerminalSelection(new Point(mySelectionStartPoint)), false);
        }
        repaint();
        mySelection.get().updateEnd(charCoords);
        if (mySettingsProvider.copyOnSelect()) {
            handleCopyOnSelect();
        }
        if (e.getY() < 0) {
            moveScrollBar((int) ((e.getY()) * SCROLL_SPEED));
        }
        if (e.getY() > getPixelHeight()) {
            moveScrollBar((int) ((e.getY() - getPixelHeight()) * SCROLL_SPEED));
        }
    }

    private void doOnMouseClicked(MouseEvent e) {
        this.canvas.requestFocus();
        if (this.popup != null && e.getButton() == MouseButton.PRIMARY) {
            this.popup.hide();
            return;
        }
        var point = createPoint(e);
        HyperlinkStyle hyperlink = isFollowLinkEvent(e) ? findHyperlink(point) : null;
        if (hyperlink != null) {
            hyperlink.getLinkInfo().navigate();
        } else if (e.getButton() == MouseButton.PRIMARY && isLocalMouseAction(e)) {
            int count = e.getClickCount();
            if (count == 1) {
                // do nothing
            } else if (count == 2) {
                // select word
                final Point charCoords = panelToCharCoords(point);
                Point start = SelectionUtil.getPreviousSeparator(charCoords, myTerminalTextBuffer);
                Point stop = SelectionUtil.getNextSeparator(charCoords, myTerminalTextBuffer);
                var sel = new TerminalSelection(start);
                sel.updateEnd(stop);
                updateSelection(sel, true);
                if (mySettingsProvider.copyOnSelect()) {
                    handleCopyOnSelect();
                }
            } else if (count == 3) {
                // select line
                final Point charCoords = panelToCharCoords(point);
                int startLine = charCoords.y;
                while (startLine > -getScrollBuffer().getLineCount()
                        && myTerminalTextBuffer.getLine(startLine - 1).isWrapped()) {
                    startLine--;
                }
                int endLine = charCoords.y;
                while (endLine < myTerminalTextBuffer.getHeight()
                        && myTerminalTextBuffer.getLine(endLine).isWrapped()) {
                    endLine++;
                }
                var sel = new TerminalSelection(new Point(0, startLine));
                sel.updateEnd(new Point(myTermSize.getColumns(), endLine));
                updateSelection(sel, true);
                if (mySettingsProvider.copyOnSelect()) {
                    handleCopyOnSelect();
                }
            }
        } else if (e.getButton() == MouseButton.MIDDLE && mySettingsProvider.pasteOnMiddleMouseClick()
                && isLocalMouseAction(e)) {
            handlePasteSelection();
        } else if (e.getButton() == MouseButton.SECONDARY) {
            HyperlinkStyle contextHyperlink = findHyperlink(point);
            TerminalActionProvider provider =
                    getTerminalActionProvider(contextHyperlink != null ? contextHyperlink.getLinkInfo() : null, e);
            popup = createPopupMenu(provider);
            popup.setOnHidden(popupEvent -> {
                popup = null;
            });
            popup.show(this.canvasPane, e.getScreenX(), e.getScreenY());
        }
        repaint();
    }

    /**
     * There is a difference between JavaFX ScrollBar.value Swing JScrollBar.value. In JavaFX value is calculated
     * in range [min, max], but in Swing in range [min, max - extent]
     */
    private int resolveSwingScrollBarValue() {
        var normalizedValue = (scrollBar.getValue() - scrollBar.getMin())
                / (scrollBar.getMax() - scrollBar.getMin());
        var swingValue = scrollBar.getMin()
                + normalizedValue * (scrollBar.getMax() - scrollBar.getVisibleAmount() - scrollBar.getMin());
        return (int) Math.round(swingValue);
    }

    private double resolveJavaFxScrollBarValue(int swingValue) {
        var normalizedValue = (swingValue - scrollBar.getMin())
                / ((scrollBar.getMax() - scrollBar.getVisibleAmount()) - scrollBar.getMin());
        var fxValue = scrollBar.getMin() + normalizedValue * (scrollBar.getMax() - scrollBar.getMin());
        return fxValue;
    }

    private boolean isFollowLinkEvent(@NotNull MouseEvent e) {
        return myCursorType == Cursor.HAND && e.getButton() == MouseButton.PRIMARY;
    }

    private Point2D createPoint(MouseEvent e) {
        return new Point2D(e.getX(), e.getY());
    }

    private Point2D createPoint(ScrollEvent e) {
        return new Point2D(e.getX(), e.getY());
    }

    protected void handleMouseWheelEvent(@NotNull ScrollEvent e) {
        var unitsToScroll = getUnitsToScroll(e);
        if (e.isShiftDown() || unitsToScroll == 0 || Math.abs(e.getDeltaY()) < 0.01) {
            return;
        }
        moveScrollBar(unitsToScroll);
        e.consume();
    }

    private void handleHyperlinks(@NotNull Point2D panelPoint) {
        Cell cell = panelPointToCell(panelPoint);
        HyperlinkStyle linkStyle = findHyperlink(cell);
        LinkInfo linkInfo = linkStyle != null ? linkStyle.getLinkInfo() : null;
        LinkInfoEx.HoverConsumer linkHoverConsumer = LinkInfoEx.getHoverConsumer(linkInfo);
        if (linkHoverConsumer != myLinkHoverConsumer) {
            if (myLinkHoverConsumer != null) {
                myLinkHoverConsumer.onMouseExited();
            }
            if (linkHoverConsumer != null) {
                LineCellInterval lineCellInterval = findIntervalWithStyle(cell, linkStyle);
                linkHoverConsumer.onMouseEntered(this.canvas, getBounds(lineCellInterval));
            }
        }
        myLinkHoverConsumer = linkHoverConsumer;
        if (linkStyle != null
                && linkStyle.getHighlightMode() != HyperlinkStyle.HighlightMode.NEVER_WITH_ORIGINAL_COLOR
                && linkStyle.getHighlightMode() != HyperlinkStyle.HighlightMode.NEVER_WITH_CUSTOM_COLOR) {
            updateHoveredHyperlink(linkStyle.getLinkInfo());
        } else {
            updateHoveredHyperlink(null);
        }
    }

    private void updateHoveredHyperlink(@Nullable LinkInfo hoveredHyperlink) {
        if (myHoveredHyperlink != hoveredHyperlink) {
            updateCursor(hoveredHyperlink != null ? Cursor.HAND : Cursor.DEFAULT);
            myHoveredHyperlink = hoveredHyperlink;
            repaint();
        }
    }

    private @NotNull LineCellInterval findIntervalWithStyle(@NotNull Cell initialCell, @NotNull HyperlinkStyle style) {
        int startColumn = initialCell.getColumn();
        while (startColumn > 0 && style == myTerminalTextBuffer.getStyleAt(startColumn - 1, initialCell.getLine())) {
            startColumn--;
        }
        int endColumn = initialCell.getColumn();
        while (endColumn < myTerminalTextBuffer.getWidth() - 1 && style ==
                myTerminalTextBuffer.getStyleAt(endColumn + 1, initialCell.getLine())) {
            endColumn++;
        }
        return new LineCellInterval(initialCell.getLine(), startColumn, endColumn);
    }

    private @Nullable HyperlinkStyle findHyperlink(@NotNull Point2D p) {
        return findHyperlink(panelPointToCell(p));
    }

    private @Nullable HyperlinkStyle findHyperlink(@Nullable Cell cell) {
        if (cell != null && cell.getColumn() >= 0 && cell.getColumn() < myTerminalTextBuffer.getWidth()
                && cell.getLine() >= -myTerminalTextBuffer.getHistoryLinesCount()
                && cell.getLine() <= myTerminalTextBuffer.getHeight()) {
            TextStyle style = myTerminalTextBuffer.getStyleAt(cell.getColumn(), cell.getLine());
            if (style instanceof HyperlinkStyle) {
                return (HyperlinkStyle) style;
            }
        }
        return null;
    }

    public void repaint() {
        this.needRepaint.set(true);
    }

    private void updateCursor(Cursor cursorType) {
        if (cursorType != myCursorType) {
            myCursorType = cursorType;
            this.canvas.setCursor(myCursorType);
        }
    }

    private void createRepaintTimer() {
        if (myRepaintTimeLine != null) {
            myRepaintTimeLine.stop();
        }
        myRepaintTimeLine = new Timeline(new KeyFrame(Duration.millis(1000 / myMaxFPS), new WeakRedrawTimer(this)));
        myRepaintTimeLine.setCycleCount(Timeline.INDEFINITE);
        myRepaintTimeLine.play();
    }

    public boolean isLocalMouseAction(MouseEvent e) {
        return mySettingsProvider.forceActionOnMouseReporting() || (isMouseReporting() == e.isShiftDown());
    }

    public boolean isLocalMouseAction(ScrollEvent e) {
        return mySettingsProvider.forceActionOnMouseReporting() || (isMouseReporting() == e.isShiftDown());
    }

    public boolean isRemoteMouseAction(MouseEvent e) {
        return isMouseReporting() && !e.isShiftDown();
    }

    public boolean isRemoteMouseAction(ScrollEvent e) {
        return isMouseReporting() && !e.isShiftDown();
    }

    public void setBlinkingPeriod(int blinkingPeriod) {
        myBlinkingPeriod = blinkingPeriod;
    }

    public void setCoordAccessor(TerminalCoordinates coordAccessor) {
        myCoordsAccessor = coordAccessor;
    }

    public void setFindResult(@Nullable FindResult findResult) {
        myFindResult = findResult;
        if (myFindResult != null && !myFindResult.getMatches().isEmpty()) {
            selectFindResultItem(myFindResult.selectedMatch());
        }
        repaint();
    }

    public FindResult getFindResult() {
        return myFindResult;
    }

    public @Nullable FindResult selectPrevFindResultItem() {
        return selectPrevOrNextFindResultItem(false);
    }

    public @Nullable FindResult selectNextFindResultItem() {
        return selectPrevOrNextFindResultItem(true);
    }

    protected @Nullable FindResult selectPrevOrNextFindResultItem(boolean next) {
        if (myFindResult != null && !myFindResult.getMatches().isEmpty()) {
            FindResult.Match item = next ? myFindResult.nextMatch() : myFindResult.prevMatch();
            selectFindResultItem(item);
            repaint();
            return myFindResult;
        }
        return null;
    }

    protected void selectFindResultItem(FindResult.Match item) {
        int historyLineCount = getTerminalTextBuffer().getHistoryLinesCount();
        int screenLineCount = getTerminalTextBuffer().getScreenLinesCount();
        var selection = new TerminalSelection(new Point(item.getStart().x,
                    item.getStart().y - myTerminalTextBuffer.getHistoryLinesCount()),
                    new Point(item.getEnd().x, item.getEnd().y - myTerminalTextBuffer.getHistoryLinesCount()));
        updateSelection(selection, true);
        logger.debug("Find selection start: {} / {}, end: {} / {}", item.getStart().x, item.getStart().y,
                item.getEnd().x, item.getEnd().y);
        if (mySelection.get().getStart().y < getTerminalTextBuffer().getHeight() / 2) {
            var value = FxScrollBarUtils.getValueFor(item.getStart().y, historyLineCount + screenLineCount,
                    scrollBar.getMin(), scrollBar.getMax());
            this.scrollBar.setValue(value);
        } else {
            this.scrollBar.setValue(this.scrollBar.getMax());
        }
    }

    static class WeakRedrawTimer implements EventHandler<ActionEvent> {

        private WeakReference<TerminalPanel> ref;

        public WeakRedrawTimer(TerminalPanel terminalPanel) {
            this.ref = new WeakReference<TerminalPanel>(terminalPanel);
        }

        @Override
        public void handle(ActionEvent e) {
            TerminalPanel terminalPanel = ref.get();
            if (terminalPanel != null) {
                terminalPanel.myCursor.changeStateIfNeeded();
                terminalPanel.myTextBlinkingTracker.updateState(terminalPanel.mySettingsProvider, terminalPanel);
                terminalPanel.updateScrolling(false);
                if (terminalPanel.needRepaint.getAndSet(false)) {
                    try {
                        terminalPanel.doRepaint();
                    } catch (Exception ex) {
                        logger.error("Error while terminal panel redraw", ex);
                    }
                }
            } else { // terminalPanel was garbage collected
                Timeline timeline = (Timeline) e.getSource();
                //TODO???
                //timeline.removeActionListener(this);
                timeline.stop();
            }
        }
    }

    @Override
    public void terminalMouseModeSet(@NotNull MouseMode mouseMode) {
        myMouseMode = mouseMode;
    }

    @Override
    public void setMouseFormat(@NotNull MouseFormat mouseFormat) {
    }

    private boolean isMouseReporting() {
        return myMouseMode != MouseMode.MOUSE_REPORTING_NONE;
    }

    /**
     * Scroll to bottom to ensure the cursor will be visible.
     */
    private void scrollToBottom() {
        // Scroll to bottom even if the cursor is on the last line, i.e. it's currently visible.
        // This will address the cases when the scroll is fixed to show some history lines, Enter is hit and after
        // Enter processing, the cursor will be pushed out of visible area unless scroll is reset to screen buffer.
        int delta = 1;
        int zeroBasedCursorY = myCursor.myCursorCoordinates.y - 1;
        if (zeroBasedCursorY + delta >= swingClientScrollOrigin + scrollBar.getVisibleAmount()) {
            scrollBar.setValue(scrollBar.getMax());
        }
    }

    public void pageUp() {
        moveScrollBar(-myTermSize.getRows());
    }

    public void pageDown() {
        moveScrollBar(myTermSize.getRows());
    }

    public void scrollUp() {
        moveScrollBar(-1);
    }

    public void scrollDown() {
        moveScrollBar(1);
    }

    private void moveScrollBar(int k) {
        var newValue = resolveJavaFxScrollBarValue(swingClientScrollOrigin + k);
        if (newValue < scrollBar.getMin()) {
            scrollBar.setValue(scrollBar.getMin());
        } else if (newValue > scrollBar.getMax()) {
            scrollBar.setValue(scrollBar.getMax());
        } else {
            scrollBar.setValue(newValue);
        }
    }

    protected Font createFont() {
        return mySettingsProvider.getTerminalFont();
    }

    private @NotNull Point panelToCharCoords(final Point2D p) {
        Cell cell = panelPointToCell(p);
        return new Point(cell.getColumn(), cell.getLine());
    }

    private @NotNull Cell panelPointToCell(@NotNull Point2D p) {
        int xDiff = (int) Math.round(p.getX()) - getInsetX();
        int x = Math.min(xDiff / (int) Math.round(myCharSize.getWidth()), getColumnCount() - 1);
        x = Math.max(0, x);
        int y = Math.min((int) Math.round(p.getY()) / (int) Math.round(myCharSize.getHeight()), getRowCount() - 1)
                + swingClientScrollOrigin;
        return new Cell(y, x);
    }

    private void copySelection(@Nullable Point selectionStart,
                               @Nullable Point selectionEnd,
                               boolean useSystemSelectionClipboardIfAvailable) {
        if (selectionStart == null || selectionEnd == null) {
            return;
        }
        String selectedText = SelectionUtil.getSelectedText(selectionStart, selectionEnd, myTerminalTextBuffer);
        if (selectedText.length() != 0) {
            myCopyPasteHandler.setContents(selectedText, useSystemSelectionClipboardIfAvailable);
        }
    }

    private void pasteFromClipboard(boolean useSystemSelectionClipboardIfAvailable) {
        String text = myCopyPasteHandler.getContents(useSystemSelectionClipboardIfAvailable);
        if (text == null) {
            return;
        }
        try {
            // Sanitize clipboard text to use CR as the line separator.
            // See https://github.com/JetBrains/jediterm/issues/136.
            if (!isWindows()) {
                // On Windows, Java automatically does this CRLF->LF sanitization, but
                // other terminals on Unix typically also do this sanitization, so
                // maybe JediTerm also should.
                text = text.replace("\r\n", "\n");
            }
            text = text.replace('\n', '\r');
            if (myBracketedPasteMode) {
                text = "\u001b[200~" + text + "\u001b[201~";
            }
            myTerminalStarter.sendString(text, true);
        } catch (RuntimeException e) {
            logger.info("", e);
        }
    }

    @Nullable
    private String getClipboardString() {
        return myCopyPasteHandler.getContents(false);
    }

    public @Nullable TermSize getTerminalSizeFromComponent() {
        int columns = ((int) Math.round(this.canvasPane.getWidth()) - getInsetX())
                / (int) Math.round(myCharSize.getWidth());
        int rows = (int) Math.round(this.canvasPane.getHeight()) / (int) Math.round(myCharSize.getHeight());
        return rows > 0 && columns > 0 ? new TermSize(columns, rows) : null;
    }

    private void sizeTerminalFromComponent() {
        if (myTerminalStarter != null) {
            TermSize newSize = getTerminalSizeFromComponent();
            if (newSize != null) {
                newSize = JediTerminal.ensureTermMinimumSize(newSize);
                if (!myTermSize.equals(newSize) || !myInitialSizeSyncDone) {
                    myTermSize = newSize;
                    myInitialSizeSyncDone = true;
                    myTypeAheadManager.onResize();
                    myTerminalStarter.postResize(newSize, RequestOrigin.User);
                }
            }
        }
    }

    public void setTerminalStarter(final TerminalStarter terminalStarter) {
        myTerminalStarter = terminalStarter;
        sizeTerminalFromComponent();
    }

    @Override
    public void onResize(@NotNull TermSize newTermSize, @NotNull RequestOrigin origin) {
        myTermSize = newTermSize;
        this.canvasPane.setPrefHeight(getPixelHeight());
        this.canvasPane.setPrefWidth(getPixelWidth());
        Platform.runLater(() -> updateScrolling(true));
    }

    private void establishFontMetrics() {
        var fontMetrics = FxFontMetrics.create(myNormalFont, "W");
        final float lineSpacing = getLineSpacing();
        double fontMetricsHeight = fontMetrics.getHeight();
        myCharSize = new Dimension2D(Math.round(fontMetrics.getWidth()), Math.round(Math.ceil(fontMetricsHeight * lineSpacing)));
        mySpaceBetweenLines = Math.max(0, (int) Math.round(((myCharSize.getHeight() - fontMetricsHeight) / 2) * 2));
        fontMetrics = FxFontMetrics.create(myNormalFont, "qpjg");
        myDescent = fontMetrics.getDescent();
        if (logger.isDebugEnabled()) {
            // The magic +2 here is to give lines a tiny bit of extra height to avoid clipping when rendering some Apple
            // emoji, which are slightly higher than the font metrics reported character height :(
            double oldCharHeight = fontMetricsHeight + (int) (lineSpacing * 2) + 2;
            double oldDescent = fontMetrics.getDescent() + (int) lineSpacing;
            logger.debug("charHeight=" + oldCharHeight + "->" + myCharSize.getHeight() +
                    ", descent=" + oldDescent + "->" + myDescent);
        }
//TODO
//    var myMonospaced = isMonospaced(fo);
//    if (!myMonospaced) {
//      logger.info("WARNING: Font " + myNormalFont.getName() + " is non-monospaced");
//    }
    }

    private float getLineSpacing() {
        if (myTerminalTextBuffer.isUsingAlternateBuffer()
                && mySettingsProvider.shouldDisableLineSpacingForAlternateScreenBuffer()) {
            return 1.0f;
        }
        return mySettingsProvider.getLineSpacing();
    }

//TODO
//  private static boolean isMonospaced(FontMetrics fontMetrics) {
//    boolean isMonospaced = true;
//    int charWidth = -1;
//    for (int codePoint = 0; codePoint < 128; codePoint++) {
//      if (Character.isValidCodePoint(codePoint)) {
//        char character = (char) codePoint;
//        if (isWordCharacter(character)) {
//          int w = fontMetrics.charWidth(character);
//          if (charWidth != -1) {
//            if (w != charWidth) {
//              isMonospaced = false;
//              break;
//            }
//          } else {
//            charWidth = w;
//          }
//        }
//      }
//    }
//    return isMonospaced;
//  }

    private static boolean isWordCharacter(char character) {
        return Character.isLetterOrDigit(character);
    }

    public @NotNull javafx.scene.paint.Color getBackground() {
        return FxTransformers.toFxColor(getWindowBackground());
    }

    public @NotNull javafx.scene.paint.Color getForeground() {
        return FxTransformers.toFxColor(getWindowForeground());
    }

    private void doRepaint() {
        resetColorCache();
        graphicsContext.setFill(getBackground());
        graphicsContext.fillRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
        this.fixScrollBarThumbVisibility();
        try {
            myTerminalTextBuffer.lock();
            // update myClientScrollOrigin as scrollArea might have been invoked after last WeakRedrawTimer action
            updateScrolling(false);
            myTerminalTextBuffer.processHistoryAndScreenLines(swingClientScrollOrigin, myTermSize.getRows(),
                    new StyledTextConsumer() {

                        final int columnCount = getColumnCount();

                        @Override
                        public void consume(int x, int y, @NotNull TextStyle style, @NotNull CharBuffer characters, int startRow) {
                            int row = y - startRow;
                            drawCharacters(x, row, style, characters, myFillCharacterBackgroundIncludingLineSpacing);
                            if (myFindResult != null && findResultHighlighted.get()) {
                                List<Pair<Integer, Integer>> ranges = myFindResult.getRanges(characters);
                                if (ranges != null && !ranges.isEmpty()) {
                                    TextStyle foundPatternStyle = getFoundPattern(style);
                                    for (Pair<Integer, Integer> range : ranges) {
                                        CharBuffer foundPatternChars = characters.subBuffer(range);
                                        drawCharacters(x + range.getFirst(), row, foundPatternStyle, foundPatternChars);
                                    }
                                }
                            }
                            if (mySelection.get() != null) {
                                Pair<Integer, Integer> interval = mySelection.get()
                                        .intersect(x, row + swingClientScrollOrigin, characters.length());
                                if (interval != null) {
                                    TextStyle selectionStyle = getSelectionStyle(style);
                                    CharBuffer selectionChars = characters.subBuffer(interval.getFirst() - x, interval.getSecond());

                                    drawCharacters(interval.getFirst(), row, selectionStyle, selectionChars);
                                }
                            }
                        }

                        @Override
                        public void consumeNul(int x, int y, int nulIndex, TextStyle style, CharBuffer characters, int startRow) {
                            int row = y - startRow;
                            if (mySelection.get() != null) {
                                // compute intersection with all NUL areas, non-breaking
                                Pair<Integer, Integer> interval = mySelection.get()
                                        .intersect(nulIndex, row + swingClientScrollOrigin, columnCount - nulIndex);
                                if (interval != null) {
                                    TextStyle selectionStyle = getSelectionStyle(style);
                                    drawCharacters(x, row, selectionStyle, characters);
                                    return;
                                }
                            }
                            drawCharacters(x, row, style, characters);
                        }

                        @Override
                        public void consumeQueue(int x, int y, int nulIndex, int startRow) {
                            if (x < columnCount) {
                                consumeNul(x, y, nulIndex, TextStyle.EMPTY, new CharBuffer(CharUtils.EMPTY_CHAR, columnCount - x), startRow);
                            }
                        }
                    });
            int cursorY = myCursor.getCoordY();
            if (cursorY < getRowCount() && !hasUncommittedChars()) {
                int cursorX = myCursor.getCoordX();
                Pair<Character, TextStyle> sc = myTerminalTextBuffer.getStyledCharAt(cursorX, cursorY);
                String cursorChar = "" + sc.getFirst();
                if (Character.isHighSurrogate(sc.getFirst())) {
                    cursorChar += myTerminalTextBuffer.getStyledCharAt(cursorX + 1, cursorY).getFirst();
                }
                TextStyle normalStyle = sc.getSecond() != null ? sc.getSecond() : myStyleState.getCurrent();
                TextStyle cursorStyle;
                if (inSelection(cursorX, cursorY)) {
                    cursorStyle = getSelectionStyle(normalStyle);
                } else {
                    cursorStyle = normalStyle;
                }
                myCursor.drawCursor(cursorChar, cursorStyle);
            }
        } finally {
            myTerminalTextBuffer.unlock();
        }
        resetColorCache();
        drawInputMethodUncommitedChars();
        drawMargins(this.canvas.getWidth(), this.canvas.getHeight());
    }

    /**
     * Hides/shows thumb in scroll bar.
     */
    private void fixScrollBarThumbVisibility() {
        if (scrollBarThumbVisible && myTerminalTextBuffer.getHistoryLinesCount() == 0) {
            this.scrollBar.getStyleClass().add("no-thumb");
            scrollBarThumbVisible = false;
        } else if (!scrollBarThumbVisible && myTerminalTextBuffer.getHistoryLinesCount() != 0) {
            this.scrollBar.getStyleClass().remove("no-thumb");
            scrollBarThumbVisible = true;
        }
    }

    private void resetColorCache() {
        myCachedSelectionColor = null;
        myCachedFoundPatternColor = null;
    }

    @NotNull
    private TextStyle getSelectionStyle(@NotNull TextStyle style) {
        if (mySettingsProvider.useInverseSelectionColor()) {
            return getInversedStyle(style);
        }
        TextStyle.Builder builder = style.toBuilder();
        TextStyle selectionStyle = getSelectionColor();
        builder.setBackground(selectionStyle.getBackground());
        builder.setForeground(selectionStyle.getForeground());
        if (builder instanceof HyperlinkStyle.Builder) {
            return ((HyperlinkStyle.Builder) builder).build(true);
        }
        return builder.build();
    }

    private @NotNull TextStyle getSelectionColor() {
        TextStyle selectionColor = myCachedSelectionColor;
        if (selectionColor == null) {
            selectionColor = mySettingsProvider.getSelectionColor();
            myCachedSelectionColor = selectionColor;
        }
        return selectionColor;
    }

    private @NotNull TextStyle getFoundPatternColor() {
        TextStyle foundPatternColor = myCachedFoundPatternColor;
        if (foundPatternColor == null) {
            foundPatternColor = mySettingsProvider.getFoundPatternColor();
            myCachedFoundPatternColor = foundPatternColor;
        }
        return foundPatternColor;
    }

    @NotNull
    private TextStyle getFoundPattern(@NotNull TextStyle style) {
        TextStyle.Builder builder = style.toBuilder();
        TextStyle foundPattern = getFoundPatternColor();
        builder.setBackground(foundPattern.getBackground());
        builder.setForeground(foundPattern.getForeground());
        return builder.build();
    }

    private void drawInputMethodUncommitedChars() {
        if (hasUncommittedChars()) {
            double xCoord = (myCursor.getCoordX() + 1) * myCharSize.getWidth() + getInsetX();
            double y = myCursor.getCoordY() + 1;
            double yCoord = y * myCharSize.getHeight() - 3;
            double len = myInputMethodUncommittedChars.length() * myCharSize.getWidth();
            graphicsContext.setFill(getBackground());
            graphicsContext.fillRect(xCoord, (y - 1) * myCharSize.getHeight() - 3, len, myCharSize.getHeight());
            graphicsContext.setFill(getForeground());
            graphicsContext.setFont(myNormalFont);
            graphicsContext.fillText(myInputMethodUncommittedChars, xCoord, yCoord);
            graphicsContext.save();
            graphicsContext.setLineWidth(1);
            graphicsContext.setLineCap(StrokeLineCap.ROUND);
            graphicsContext.setLineJoin(StrokeLineJoin.ROUND);
            graphicsContext.setMiterLimit(0);
            graphicsContext.setLineDashes(0, 2, 0, 2);
            graphicsContext.setLineDashOffset(0);
            graphicsContext.strokeLine(xCoord, yCoord, xCoord + len, yCoord);
            graphicsContext.restore();
        }
    }

    private boolean hasUncommittedChars() {
        return myInputMethodUncommittedChars != null && myInputMethodUncommittedChars.length() > 0;
    }

    private boolean inSelection(int x, int y) {
        return mySelection.get() != null && mySelection.get().contains(new Point(x, y));
    }

    public int getPixelWidth() {
        return (int) Math.round(myCharSize.getWidth() * myTermSize.getColumns() + getInsetX());
    }

    public int getPixelHeight() {
        return (int) Math.round(myCharSize.getHeight() * myTermSize.getRows());
    }

    private int getColumnCount() {
        return myTermSize.getColumns();
    }

    private int getRowCount() {
        return myTermSize.getRows();
    }

    public String getWindowTitle() {
        return myWindowTitle;
    }

    @Override
    public @NotNull Color getWindowForeground() {
        return toForeground(mySettingsProvider.getDefaultForeground());
    }

    @Override
    public @NotNull Color getWindowBackground() {
        return toBackground(mySettingsProvider.getDefaultBackground());
    }

    private @NotNull javafx.scene.paint.Color getEffectiveForeground(@NotNull TextStyle style) {
        Color color = style.hasOption(Option.INVERSE) ? getBackground(style) : getForeground(style);
        return FxTransformers.toFxColor(color);
    }

    private @NotNull javafx.scene.paint.Color getEffectiveBackground(@NotNull TextStyle style) {
        Color color = style.hasOption(Option.INVERSE) ? getForeground(style) : getBackground(style);
        return FxTransformers.toFxColor(color);
    }

    private @NotNull Color getForeground(@NotNull TextStyle style) {
        TerminalColor foreground = style.getForeground();
        return foreground != null ? toForeground(foreground) : getWindowForeground();
    }

    private @NotNull Color toForeground(@NotNull TerminalColor terminalColor) {
        if (terminalColor.isIndexed()) {
            return getPalette().getForeground(terminalColor);
        }
        return terminalColor.toColor();
    }

    private @NotNull Color getBackground(@NotNull TextStyle style) {
        TerminalColor background = style.getBackground();
        return background != null ? toBackground(background) : getWindowBackground();
    }

    private @NotNull Color toBackground(@NotNull TerminalColor terminalColor) {
        if (terminalColor.isIndexed()) {
            return getPalette().getBackground(terminalColor);
        }
        return terminalColor.toColor();
    }

    protected int getInsetX() {
        return 4;
    }

    public void addTerminalMouseListener(final TerminalMouseListener listener) {
        this.canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (mySettingsProvider.enableMouseReporting() && isRemoteMouseAction(e)) {
                Point p = panelToCharCoords(createPoint(e));
                listener.mousePressed(p.x, p.y, new FxMouseEvent(e));
            }
        });
        this.canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (mySettingsProvider.enableMouseReporting() && isRemoteMouseAction(e)) {
                Point p = panelToCharCoords(createPoint(e));
                listener.mouseReleased(p.x, p.y, new FxMouseEvent(e));
            }
        });
        this.canvas.addEventHandler(ScrollEvent.SCROLL, e -> {
            if (mySettingsProvider.enableMouseReporting() && isRemoteMouseAction(e)) {
                updateSelection(null, true);
                Point p = panelToCharCoords(createPoint(e));
                listener.mouseWheelMoved(p.x, p.y, new FxMouseWheelEvent(e));
            }
            if (myTerminalTextBuffer.isUsingAlternateBuffer() && mySettingsProvider.sendArrowKeysInAlternativeMode()) {
                //Send Arrow keys instead
                final byte[] arrowKeys;
                if (e.getDeltaY() > 0) {
                    arrowKeys = myTerminalStarter.getTerminal().getCodeForKey(KeyCode.UP.getCode(), 0);
                } else {
                    arrowKeys = myTerminalStarter.getTerminal().getCodeForKey(KeyCode.DOWN.getCode(), 0);
                }
                for (int i = 0; i < Math.abs(getUnitsToScroll(e)); i++) {
                    myTerminalStarter.sendBytes(arrowKeys, false);
                }
                e.consume();
            }
        });
        this.canvas.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            if (mySettingsProvider.enableMouseReporting() && isRemoteMouseAction(e)) {
                Point p = panelToCharCoords(createPoint(e));
                listener.mouseMoved(p.x, p.y, new FxMouseEvent(e));
            }
        });
        this.canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (mySettingsProvider.enableMouseReporting() && isRemoteMouseAction(e)) {
                Point p = panelToCharCoords(createPoint(e));
                listener.mouseDragged(p.x, p.y, new FxMouseEvent(e));
            }
        });
    }

    private enum TerminalCursorState {
        SHOWING, HIDDEN, NO_FOCUS;
    }

    private int getUnitsToScroll(ScrollEvent event) {
        // Assume that each scroll unit corresponds to 40.0 pixels, which is a typical value.
        int unitsToScroll = (int) Math.round(event.getDeltaY() / 40.0);
        return unitsToScroll * -1;
    }

    private class TerminalCursor {

        // cursor state
        private boolean myCursorIsShown; // blinking state

        private final Point myCursorCoordinates = new Point();

        private @Nullable CursorShape myShape;

        // terminal modes
        private boolean myShouldDrawCursor = true;

        private long myLastCursorChange;

        private boolean myCursorHasChanged;

        public void setX(int x) {
            myCursorCoordinates.x = x;
            cursorChanged();
        }

        public void setY(int y) {
            myCursorCoordinates.y = y;
            cursorChanged();
        }

        public int getCoordX() {
            if (myTypeAheadManager != null) {
                return myTypeAheadManager.getCursorX() - 1;
            }
            return myCursorCoordinates.x;
        }

        public int getCoordY() {
            return myCursorCoordinates.y - 1 - swingClientScrollOrigin;
        }

        public void setShouldDrawCursor(boolean shouldDrawCursor) {
            myShouldDrawCursor = shouldDrawCursor;
        }

        public boolean isBlinking() {
            return getEffectiveShape().isBlinking() && (getBlinkingPeriod() > 0);
        }

        public void cursorChanged() {
            myCursorHasChanged = true;
            myLastCursorChange = System.currentTimeMillis();
            repaint();
        }

        private boolean cursorShouldChangeBlinkState(long currentTime) {
            return currentTime - myLastCursorChange > getBlinkingPeriod();
        }

        public void changeStateIfNeeded() {
            if (!canvas.isFocused()) {
                return;
            }
            long currentTime = System.currentTimeMillis();
            if (cursorShouldChangeBlinkState(currentTime)) {
                myCursorIsShown = !myCursorIsShown;
                myLastCursorChange = currentTime;
                myCursorHasChanged = false;
                repaint();
            }
        }

        private TerminalCursorState computeBlinkingState() {
            if (!isBlinking() || myCursorHasChanged || myCursorIsShown) {
                return TerminalCursorState.SHOWING;
            }
            return TerminalCursorState.HIDDEN;
        }

        private TerminalCursorState computeCursorState() {
            if (!myShouldDrawCursor) {
                return TerminalCursorState.HIDDEN;
            }
            if (!canvas.isFocused()) {
                return TerminalCursorState.NO_FOCUS;
            }
            return computeBlinkingState();
        }

        void drawCursor(String c, TextStyle style) {
            TerminalCursorState state = computeCursorState();
            // hidden: do nothing
            if (state == TerminalCursorState.HIDDEN) {
                return;
            }
            final int x = getCoordX();
            final int y = getCoordY();
            // Outside bounds of window: do nothing
            if (y < 0 || y >= myTermSize.getRows()) {
                return;
            }
            CharBuffer buf = new CharBuffer(c);
            double xCoord = x * myCharSize.getWidth() + getInsetX();
            double yCoord = y * myCharSize.getHeight();
            double textLength = CharUtils.getTextLengthDoubleWidthAware(buf.getBuf(), buf.getStart(), buf.length(),
                    mySettingsProvider.ambiguousCharsAreDoubleWidth());
            double height = Math.min(myCharSize.getHeight(), canvas.getHeight() - yCoord);
            double width = Math.min(textLength * TerminalPanel.this.myCharSize.getWidth(), canvas.getWidth() - xCoord);
            int lineStrokeSize = 2;
            var fgColor = getEffectiveForeground(style);
            TextStyle inversedStyle = getInversedStyle(style);
            var inverseBg = getEffectiveBackground(inversedStyle);
            switch (getEffectiveShape()) {
                case BLINK_BLOCK:
                case STEADY_BLOCK:
                    if (state == TerminalCursorState.SHOWING) {
                        graphicsContext.setFill(inverseBg);
                        graphicsContext.fillRect(xCoord, yCoord, width, height);
                        drawCharacters(x, y, inversedStyle, buf);
                    } else {
                        graphicsContext.setStroke(fgColor);
                        graphicsContext.setLineWidth(1.0);
                        graphicsContext.strokeRect(xCoord, yCoord, width, height);
                    }
                    break;
                case BLINK_UNDERLINE:
                case STEADY_UNDERLINE:
                    graphicsContext.setFill(fgColor);
                    graphicsContext.fillRect(xCoord, yCoord + height, width, lineStrokeSize);
                    break;
                case BLINK_VERTICAL_BAR:
                case STEADY_VERTICAL_BAR:
                    graphicsContext.setFill(fgColor);
                    graphicsContext.fillRect(xCoord, yCoord, lineStrokeSize, height);
                    break;
            }
        }

        void setShape(@Nullable CursorShape shape) {
            myShape = shape;
        }

        @NotNull
        CursorShape getEffectiveShape() {
            return Objects.requireNonNullElse(myShape, CursorShape.BLINK_BLOCK);
        }
    }

    private int getBlinkingPeriod() {
        if (myBlinkingPeriod != mySettingsProvider.caretBlinkingMs()) {
            setBlinkingPeriod(mySettingsProvider.caretBlinkingMs());
        }
        return myBlinkingPeriod;
    }

    @NotNull
    private TextStyle getInversedStyle(@NotNull TextStyle style) {
        TextStyle.Builder builder = new TextStyle.Builder(style);
        builder.setOption(Option.INVERSE, !style.hasOption(Option.INVERSE));
        if (style.getForeground() == null) {
            builder.setForeground(myStyleState.getDefaultForeground());
        }
        if (style.getBackground() == null) {
            builder.setBackground(myStyleState.getDefaultBackground());
        }
        return builder.build();
    }

    private void drawCharacters(int x, int y, TextStyle style, CharBuffer buf) {
        drawCharacters(x, y, style, buf, true);
    }

    private void drawCharacters(int x, int y, TextStyle style, CharBuffer buf, boolean includeSpaceBetweenLines) {
        if (myTextBlinkingTracker.shouldBlinkNow(style)) {
            style = getInversedStyle(style);
        }
        double xCoord = x * myCharSize.getWidth() + getInsetX();
        double yCoord = y * myCharSize.getHeight() + (includeSpaceBetweenLines ? 0 : mySpaceBetweenLines / 2);
        if (xCoord < 0 || xCoord > this.canvas.getWidth() || yCoord < 0 || yCoord > this.canvas.getHeight()) {
            return;
        }
        int textLength = CharUtils.getTextLengthDoubleWidthAware(buf.getBuf(), buf.getStart(), buf.length(),
                mySettingsProvider.ambiguousCharsAreDoubleWidth());
        double height = Math.min(myCharSize.getHeight() - (includeSpaceBetweenLines ? 0 : mySpaceBetweenLines),
                this.canvas.getHeight() - yCoord);
        double width = Math.min(textLength * this.myCharSize.getWidth(), this.canvas.getWidth() - xCoord);
        boolean shouldUnderline = style.hasOption(Option.UNDERLINED);
        if (style instanceof HyperlinkStyle) {
            HyperlinkStyle hyperlinkStyle = (HyperlinkStyle) style;
            switch (mySettingsProvider.getHyperlinkHighlightingMode()) {
                case ALWAYS_WITH_ORIGINAL_COLOR:
                    style = provideOriginalStyle(hyperlinkStyle);
                    shouldUnderline = true;
                    break;
                case ALWAYS_WITH_CUSTOM_COLOR:
                    style = hyperlinkStyle.getCustomStyle();
                    shouldUnderline = true;
                    break;
                case NEVER_WITH_ORIGINAL_COLOR:
                    style = provideOriginalStyle(hyperlinkStyle);
                    shouldUnderline = style.hasOption(Option.UNDERLINED);
                    break;
                case NEVER_WITH_CUSTOM_COLOR:
                    style = hyperlinkStyle.getCustomStyle();
                    shouldUnderline = hyperlinkStyle.getPrevTextStyle().hasOption(Option.UNDERLINED);
                    break;
                case HOVER_WITH_ORIGINAL_COLOR:
                    style = provideOriginalStyle(hyperlinkStyle);
                    shouldUnderline = (isHoveredHyperlink(hyperlinkStyle)) ? true : false;
                    break;
                case HOVER_WITH_CUSTOM_COLOR:
                    style = hyperlinkStyle.getCustomStyle();
                    shouldUnderline = (isHoveredHyperlink(hyperlinkStyle)) ? true : false;
                    break;
                case HOVER_WITH_BOTH_COLORS:
                    if (isHoveredHyperlink(hyperlinkStyle)) {
                        style = hyperlinkStyle.getCustomStyle();
                        shouldUnderline = true;
                    } else {
                        style = provideOriginalStyle(hyperlinkStyle);
                        shouldUnderline = false;
                    }
                    break;
                default:
                    throw new AssertionError();
            }
        }
        javafx.scene.paint.Color backgroundColor = getEffectiveBackground(style);
        graphicsContext.setFill(backgroundColor);
        graphicsContext.fillRect(xCoord, yCoord, width, height);
        if (buf.isNul()) {
            return; // nothing more to do
        }
        var color = getStyleForeground(style);
        graphicsContext.setFill(color);
        graphicsContext.setStroke(color);
        drawChars(x, y, buf, style);
        if (shouldUnderline) {
            double baseLine = (y + 1) * myCharSize.getHeight() - mySpaceBetweenLines / 2 - myDescent;
            double lineY = baseLine + 3;
            graphicsContext.setLineWidth(1.0);
            graphicsContext.strokeLine(xCoord, lineY, (x + textLength) * myCharSize.getWidth() + getInsetX(), lineY);
        }
    }

    /**
     * When selection original style = null.
     * @param link
     * @return
     */
    private @NotNull TextStyle provideOriginalStyle(@NotNull HyperlinkStyle link) {
        if (link.getOriginalStyle() != null) {
            return link.getOriginalStyle();
        } else {
            return link;
        }
    }

    private boolean isHoveredHyperlink(@NotNull HyperlinkStyle link) {
        return myHoveredHyperlink == link.getLinkInfo();
    }

    /**
     * Draw every char in separate terminal cell to guaranty equal width for different lines.
     * Nevertheless, to improve kerning we draw word characters as one block for monospaced fonts.
     */
    private void drawChars(int x, int y, @NotNull CharBuffer buf, @NotNull TextStyle style) {
        // workaround to fix Swing bad rendering of bold special chars on Linux
        // TODO required for italic?
        CharBuffer renderingBuffer;
        if (mySettingsProvider.DECCompatibilityMode() && style.hasOption(TextStyle.Option.BOLD)) {
            renderingBuffer = CharUtils.heavyDecCompatibleBuffer(buf);
        } else {
            renderingBuffer = buf;
        }
        BreakIterator iterator = BreakIterator.getCharacterInstance();
        char[] text = renderingBuffer.clone().getBuf();
        iterator.setText(new String(text));
        int endOffset;
        int startOffset = 0;
        while ((endOffset = iterator.next()) != BreakIterator.DONE) {
            endOffset = extendEndOffset(text, iterator, startOffset, endOffset);
            int effectiveEndOffset = shiftDwcToEnd(text, startOffset, endOffset);
            if (effectiveEndOffset == startOffset) {
                startOffset = endOffset;
                continue; // nothing to draw
            }
            Font font = getFontToDisplay(text, startOffset, effectiveEndOffset, style);
            graphicsContext.setFont(font);
            var str = new String(text, startOffset, effectiveEndOffset - startOffset);
            //var fontMetrics = FxFontMetrics.create(font, str);
            //double descent = fontMetrics.getDescent();
            double descent = myDescent;
            double baseLine = (y + 1) * myCharSize.getHeight() - mySpaceBetweenLines / 2 - descent;
            double charWidth = myCharSize.getWidth();
            double xCoord = (x + startOffset) * charWidth + getInsetX();
            double yCoord = y * myCharSize.getHeight() + mySpaceBetweenLines / 2;
            graphicsContext.save();
            graphicsContext.beginPath();
            //Important! JavaFX clip bug. It is necessary to use integers for setting clip shape otherwise clip will be slow
            //see https://bugs.openjdk.org/browse/JDK-8335184
            graphicsContext.rect(
                    Math.round(xCoord),
                    Math.round(yCoord),
                    Math.round(this.canvas.getWidth() - xCoord),
                    Math.round(this.canvas.getHeight() - yCoord));
            graphicsContext.closePath();
            graphicsContext.clip();
            int emptyCells = endOffset - startOffset;
            if (emptyCells >= 2) {
                //int drawnWidth = gfx.getFontMetrics(font).charsWidth(text, startOffset, effectiveEndOffset - startOffset);
                //double drawnWidth = fontMetrics.getWidth();
                double drawnWidth = myCharSize.getWidth();
                double emptySpace = Math.max(0, emptyCells * charWidth - drawnWidth);
                // paint a Unicode symbol closer to the center
                xCoord += emptySpace / 2;
            }
            xCoord = Math.round(xCoord);
            baseLine = Math.round(baseLine);
            //logger.debug("Drawing {} at {}:{}", str, xCoord, baseLine);
            graphicsContext.fillText(str, xCoord, baseLine);
            graphicsContext.restore();
            startOffset = endOffset;
        }

    }

    private static int shiftDwcToEnd(char[] text, int startOffset, int endOffset) {
        int ind = startOffset;
        for (int i = startOffset; i < endOffset; i++) {
            if (text[i] != CharUtils.DWC) {
                text[ind++] = text[i];
            }
        }
        Arrays.fill(text, ind, endOffset, CharUtils.DWC);
        return ind;
    }

    private static int extendEndOffset(char[] text, @NotNull BreakIterator iterator, int startOffset, int endOffset) {
        while (shouldExtend(text, startOffset, endOffset)) {
            int newEndOffset = iterator.next();
            if (newEndOffset == BreakIterator.DONE) {
                break;
            }
            if (newEndOffset - endOffset == 1 && !isUnicodePart(text, endOffset)) {
                iterator.previous(); // do not eat a plain char following Unicode symbol
                break;
            }
            startOffset = endOffset;
            endOffset = newEndOffset;
        }
        return endOffset;
    }

    private static boolean shouldExtend(char[] text, int startOffset, int endOffset) {
        if (endOffset - startOffset > 1) {
            return true;
        }
        if (isFormatChar(text, startOffset, endOffset)) {
            return true;
        }
        return endOffset < text.length && text[endOffset] == CharUtils.DWC;
    }

    private static boolean isUnicodePart(char[] text, int ind) {
        if (isFormatChar(text, ind, ind + 1)) {
            return true;
        }
        if (text[ind] == CharUtils.DWC) {
            return true;
        }
        return Character.UnicodeBlock.of(text[ind]) == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_ARROWS;
    }

    private static boolean isFormatChar(char[] text, int start, int end) {
        if (end - start == 1) {
            int charCode = text[start];
            // From CMap#getFormatCharGlyph
            if (charCode >= 0x200c) {
                //noinspection RedundantIfStatement
                if ((charCode <= 0x200f) ||
                        (charCode >= 0x2028 && charCode <= 0x202e) ||
                        (charCode >= 0x206a && charCode <= 0x206f)) {
                    return true;
                }
            }
        }
        return false;
    }

    private @NotNull javafx.scene.paint.Color getStyleForeground(@NotNull TextStyle style) {
        javafx.scene.paint.Color foreground = getEffectiveForeground(style);
        if (style.hasOption(Option.DIM)) {
            javafx.scene.paint.Color background = getEffectiveBackground(style);
            foreground = new javafx.scene.paint.Color((foreground.getRed() + background.getRed()) / 2,
                    (foreground.getGreen() + background.getGreen()) / 2,
                    (foreground.getBlue() + background.getBlue()) / 2,
                    foreground.getOpacity());
        }
        return foreground;
    }

    protected @NotNull Font getFontToDisplay(char[] text, int start, int end, @NotNull TextStyle style) {
        boolean bold = style.hasOption(TextStyle.Option.BOLD);
        boolean italic = style.hasOption(TextStyle.Option.ITALIC);
        // workaround to fix Swing bad rendering of bold special chars on Linux
        if (bold && mySettingsProvider.DECCompatibilityMode() && CharacterSets.isDecBoxChar(text[start])) {
            return myNormalFont;
        }
        return bold ? (italic ? myBoldItalicFont : myBoldFont)
                : (italic ? myItalicFont : myNormalFont);
    }

    private ColorPalette getPalette() {
        return mySettingsProvider.getTerminalColorPalette();
    }

    private void drawMargins(double width, double height) {
        //TODO
        graphicsContext.setFill(getBackground());
        graphicsContext.fillRect(0, height, this.canvas.getWidth(), this.canvas.getHeight() - height);
        graphicsContext.fillRect(width, 0, this.canvas.getWidth() - width, this.canvas.getHeight());
    }

    // Called in a background thread with myTerminalTextBuffer.lock() acquired
    @Override
    public void scrollArea(final int scrollRegionTop, final int scrollRegionSize, int dy) {
        scrollDy.addAndGet(dy);
        updateSelection(null, true);
    }

    // should be called on EDT
    public void scrollToShowAllOutput() {
        myTerminalTextBuffer.lock();
        try {
            int historyLines = myTerminalTextBuffer.getHistoryLinesCount();
            if (historyLines > 0) {
                int termHeight = myTermSize.getRows();
                setScrollBarRangeProperties(-historyLines, historyLines + termHeight, -historyLines, termHeight);
                TerminalModelListener modelListener = new TerminalModelListener() {
                    @Override
                    public void modelChanged() {
                        int zeroBasedCursorY = myCursor.myCursorCoordinates.y - 1;
                        if (zeroBasedCursorY + historyLines >= termHeight) {
                            myTerminalTextBuffer.removeModelListener(this);
                            Platform.runLater(() -> {
                                myTerminalTextBuffer.lock();
                                try {
                                    setScrollBarRangeProperties(0, myTermSize.getRows(),
                                            -myTerminalTextBuffer.getHistoryLinesCount(), myTermSize.getRows());
                                } finally {
                                    myTerminalTextBuffer.unlock();
                                }
                            });
                        }
                    }
                };
                myTerminalTextBuffer.addModelListener(modelListener);
                //we use listener only for value, because it is changed always (when min, max, visibleAmount) change
                scrollBar.valueProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                        scrollBar.valueProperty().removeListener(this);
                        myTerminalTextBuffer.removeModelListener(modelListener);
                    }
                });
            }
        } finally {
            myTerminalTextBuffer.unlock();
        }
    }

    private void updateScrolling(boolean forceUpdate) {
        int dy = scrollDy.getAndSet(0);
        boolean historyBufferLineCountChanged = myHistoryBufferLineCountChanged.getAndSet(false);
        if (dy == 0 && !forceUpdate && !historyBufferLineCountChanged) {
            return;
        }
        if (myScrollingEnabled) {
            int value = swingClientScrollOrigin;
            int historyLineCount = myTerminalTextBuffer.getHistoryLinesCount();
            if (value == 0) {
                setScrollBarRangeProperties(myTermSize.getRows(), myTermSize.getRows(), -historyLineCount, myTermSize.getRows());
            } else {
                // if scrolled to a specific area, update scroll to keep showing this area
                setScrollBarRangeProperties(
                        Math.min(Math.max(value + dy, -historyLineCount), myTermSize.getRows()),
                        myTermSize.getRows(),
                        -historyLineCount,
                        myTermSize.getRows());
            }
        } else {
            setScrollBarRangeProperties(myTermSize.getRows(), myTermSize.getRows(), 0, myTermSize.getRows());
        }
    }

    private void setScrollBarRangeProperties(int value, int extent, int min, int max) {
        this.scrollBar.setVisibleAmount(extent);
        this.scrollBar.setMin(min);
        this.scrollBar.setMax(max);
        //value is updated in the end, because we have listener on value.
        this.scrollBar.setValue(value);
    }

    public void setCursor(final int x, final int y) {
        myCursor.setX(x);
        myCursor.setY(y);
    }

    @Override
    public void setCursorShape(@Nullable CursorShape cursorShape) {
        myCursor.setShape(cursorShape);
    }

    public void beep() {
        if (mySettingsProvider.audibleBell()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public @Nullable Rectangle2D getBounds(@NotNull TerminalLineIntervalHighlighting highlighting) {
        TerminalLine line = highlighting.getLine();
        int index = myTerminalTextBuffer.findScreenLineIndex(line);
        if (index >= 0 && !highlighting.isDisposed()) {
            return getBounds(new LineCellInterval(index, highlighting.getStartOffset(), highlighting.getEndOffset() + 1));
        }
        return null;
    }

    private @NotNull Rectangle2D getBounds(@NotNull LineCellInterval cellInterval) {
        var x = cellInterval.getStartColumn() * myCharSize.getWidth() + getInsetX();
        var y = cellInterval.getLine() * myCharSize.getHeight();
        return new Rectangle2D(x, y, myCharSize.getWidth() * cellInterval.getCellCount(), myCharSize.getHeight());
    }

    public TerminalTextBuffer getTerminalTextBuffer() {
        return myTerminalTextBuffer;
    }

    @Override
    public @Nullable TerminalSelection getSelection() {
        return mySelection.get();
    }

    @Override
    public boolean ambiguousCharsAreDoubleWidth() {
        return mySettingsProvider.ambiguousCharsAreDoubleWidth();
    }

    @Override
    public void setBracketedPasteMode(boolean bracketedPasteModeEnabled) {
        myBracketedPasteMode = bracketedPasteModeEnabled;
    }

    public LinesBuffer getScrollBuffer() {
        return myTerminalTextBuffer.getHistoryBuffer();
    }

    @Override
    public void setCursorVisible(boolean isCursorVisible) {
        myCursor.setShouldDrawCursor(isCursorVisible);
    }

    protected @NotNull ContextMenu createPopupMenu(@NotNull TerminalActionProvider actionProvider) {
        ContextMenu menu = new ContextMenu();
        TerminalAction.fillMenu(menu, actionProvider);
        return menu;
    }

    private @NotNull TerminalActionProvider getTerminalActionProvider(@Nullable LinkInfo linkInfo, @NotNull MouseEvent e) {
        LinkInfoEx.PopupMenuGroupProvider popupMenuGroupProvider = LinkInfoEx.getPopupMenuGroupProvider(linkInfo);
        if (popupMenuGroupProvider != null) {
            return new TerminalActionProvider() {

                @Override
                public List<TerminalAction> getActions() {
                    return new ArrayList<>();//TEMP TEMP TEMP
                    //TODO
                    //return popupMenuGroupProvider.getPopupMenuGroup(e);
                }

                @Override
                public TerminalActionProvider getNextProvider() {
                    return TerminalPanel.this;
                }

                @Override
                public void setNextProvider(TerminalActionProvider provider) {
                }
            };
        }
        return this;
    }

    @Override
    public void useAlternateScreenBuffer(boolean useAlternateScreenBuffer) {
        myScrollingEnabled = !useAlternateScreenBuffer;
        Platform.runLater(() -> {
            updateScrolling(true);
            if (myUsingAlternateBuffer != myTerminalTextBuffer.isUsingAlternateBuffer()) {
                myUsingAlternateBuffer = myTerminalTextBuffer.isUsingAlternateBuffer();
                if (mySettingsProvider.shouldDisableLineSpacingForAlternateScreenBuffer()) {
                    Timeline timeline = new Timeline(new KeyFrame(Duration.millis(10), e -> {
                        reinitFontAndResize();
                    }));
                    timeline.setCycleCount(1);
                    timeline.play();
                }
            }
        });
    }

    public TerminalOutputStream getTerminalOutputStream() {
        return myTerminalStarter;
    }

    @Override
    public void setWindowTitle(@NotNull String windowTitle) {
        myWindowTitle = windowTitle;
    }

    @Override
    public List<TerminalAction> getActions() {
        return List.of(
                new TerminalAction(mySettingsProvider.getOpenUrlActionPresentation(), input -> {
                    return openSelectedTextAsURL();
                }).withEnabledSupplier(this::isSelectedTextUrl),
                new TerminalAction(mySettingsProvider.getCopyActionPresentation(), this::handleCopy) {
                    @Override
                    public boolean isEnabled(@Nullable KeyEvent e) {
                        return e != null || mySelection.get() != null;
                    }
                }.withMnemonicKey(KeyCode.C),
                new TerminalAction(mySettingsProvider.getPasteActionPresentation(), input -> {
                    handlePaste();
                    return true;
                }).withMnemonicKey(KeyCode.P).withEnabledSupplier(() -> getClipboardString() != null),
                new TerminalAction(mySettingsProvider.getSelectAllActionPresentation(), input -> {
                    selectAll();
                    return true;
                }),
                new TerminalAction(mySettingsProvider.getClearBufferActionPresentation(), input -> {
                    clearBuffer();
                    return true;
                }).withMnemonicKey(KeyCode.K).withEnabledSupplier(() ->
                        !myTerminalTextBuffer.isUsingAlternateBuffer()).separatorBefore(true),
                new TerminalAction(mySettingsProvider.getPageUpActionPresentation(), input -> {
                    pageUp();
                    return true;
                }).withEnabledSupplier(() -> !myTerminalTextBuffer.isUsingAlternateBuffer()).separatorBefore(true),
                new TerminalAction(mySettingsProvider.getPageDownActionPresentation(), input -> {
                    pageDown();
                    return true;
                }).withEnabledSupplier(() -> !myTerminalTextBuffer.isUsingAlternateBuffer()),
                new TerminalAction(mySettingsProvider.getLineUpActionPresentation(), input -> {
                    scrollUp();
                    return true;
                }).withEnabledSupplier(() -> !myTerminalTextBuffer.isUsingAlternateBuffer()).separatorBefore(true),
                new TerminalAction(mySettingsProvider.getLineDownActionPresentation(), input -> {
                    scrollDown();
                    return true;
                }));
    }

    public void selectAll() {
        var selection = new TerminalSelection(new Point(0, -myTerminalTextBuffer.getHistoryLinesCount()),
                new Point(myTermSize.getColumns(), myTerminalTextBuffer.getScreenLinesCount()));
        updateSelection(selection, true);
    }

    @NotNull
    public boolean isSelectedTextUrl() {
        String selectedText = getSelectedText();
        if (selectedText != null) {
            try {
                URI uri = new URI(selectedText);
                //noinspection ResultOfMethodCallIgnored
                uri.toURL();
                return true;
            } catch (Exception e) {
                //pass
            }
        }
        return false;
    }

    @Nullable
    private String getSelectedText() {
        if (mySelection.get() != null) {
            Pair<Point, Point> points = mySelection.get().pointsForRun(myTermSize.getColumns());
            if (points.getFirst() != null || points.getSecond() != null) {
                return SelectionUtil
                        .getSelectedText(points.getFirst(), points.getSecond(), myTerminalTextBuffer);
            }
        }
        return null;
    }

    public boolean openSelectedTextAsURL() {
        if (Desktop.isDesktopSupported()) {
            try {
                String selectedText = getSelectedText();
                if (selectedText != null) {
                    EventQueue.invokeLater(() -> {
                        try {
                            Desktop.getDesktop().browse(new URI(selectedText));
                        } catch (Exception ex) {
                            logger.error("Error opening url: {}", selectedText, ex);
                        }
                    });
                }
            } catch (Exception e) {
                //ok then
            }
        }
        return false;
    }

    public void clearBuffer() {
        clearBuffer(true);
    }

    /**
     * @param keepLastLine true to keep last line (e.g. to keep terminal prompt)
     *                     false to clear entire terminal panel (relevant for terminal console)
     */
    protected void clearBuffer(boolean keepLastLine) {
        if (!myTerminalTextBuffer.isUsingAlternateBuffer()) {
            myTerminalTextBuffer.clearHistory();
            if (myCoordsAccessor != null) {
                if (keepLastLine) {
                    if (myCoordsAccessor.getY() > 0) {
                        TerminalLine lastLine = myTerminalTextBuffer.getLine(myCoordsAccessor.getY() - 1);
                        myTerminalTextBuffer.clearScreenBuffer();
                        myCoordsAccessor.setY(0);
                        myCursor.setY(1);
                        myTerminalTextBuffer.addLine(lastLine);
                    }
                } else {
                    myTerminalTextBuffer.clearScreenBuffer();
                    myCoordsAccessor.setX(0);
                    myCoordsAccessor.setY(1);
                    myCursor.setX(0);
                    myCursor.setY(1);
                }
            }
            scrollBar.setValue(scrollBar.getMin());
            updateScrolling(true);
            swingClientScrollOrigin = resolveSwingScrollBarValue();
        }
    }

    @Override
    public TerminalActionProvider getNextProvider() {
        return myNextActionProvider;
    }

    @Override
    public void setNextProvider(TerminalActionProvider provider) {
        myNextActionProvider = provider;
    }

    private static final byte ASCII_NUL = 0;

    private static final byte ASCII_ESC = 27;

    private static final byte ASCII_ETX = 3;

    private boolean processTerminalKeyPressed(KeyEvent e) {
        if (hasUncommittedChars()) {
            return false;
        }
        try {
            final KeyCode keyCode = e.getCode();
            // numLock does not change the code sent by keypad VK_DELETE
            // although it send the char '.'
//            if (keycode == KeyCode.DELETE && keychar == '.') {
//                myTerminalStarter.sendBytes(new byte[]{'.'}, true);
//                return true;
//            }
            if (e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown()) {
                // CTRL + Space is not handled in KeyEvent; handle it manually
                if (keyCode == KeyCode.SPACE) {
                    myTerminalStarter.sendBytes(new byte[]{ASCII_NUL}, true);
                    return true;
                }
                //Ctrl+C sends ASCII 3 (ETX) in Linux but gets intercepted as copy
                //command in Windows, sending just 'c' (code 99) instead
                if (keyCode == KeyCode.C) {
                    myTerminalStarter.sendBytes(new byte[]{ASCII_ETX}, true);
                    return true;
                }
            }
            final byte[] code = myTerminalStarter.getTerminal().getCodeForKey(e.getCode().getCode(), getModifiersEx(e));
            if (code != null) {
                myTerminalStarter.sendBytes(code, true);
                if (mySettingsProvider.scrollToBottomOnTyping() && isCodeThatScrolls(keyCode)) {
                    scrollToBottom();
                }
                return true;
            }
            final char keyChar;
            if (!e.getText().isEmpty()) {
                keyChar = e.getText().charAt(0);
            } else {
                keyChar = '\uffff';
            }
            if (isAltPressedOnly(e) && Character.isDefined(keyChar) && mySettingsProvider.altSendsEscape()) {
                // Cannot use e.getKeyChar() on macOS:
                //  Option+f produces e.getKeyChar()='ƒ' (402), but 'f' (102) is needed.
                //  Option+b produces e.getKeyChar()='∫' (8747), but 'b' (98) is needed.
                myTerminalStarter.sendString(new String(new char[]{ASCII_ESC, simpleMapKeyCodeToChar(e)}), true);
                return true;
            }
            if (e.getText().length() > 0 && Character.isISOControl(e.getText().codePointAt(0))) {
                // keys filtered out here will be processed in processTerminalKeyTyped
                return processCharacter(e, keyChar);
            }
        } catch (Exception ex) {
            logger.error("Error sending pressed key to emulator", ex);
        }
        return false;
    }

    private static char simpleMapKeyCodeToChar(@NotNull KeyEvent e) {
        // zsh requires proper case of letter
        if (e.isShiftDown()) {
            return Character.toUpperCase(e.getText().charAt(0));
        }
        return Character.toLowerCase(e.getText().charAt(0));
    }

    private static boolean isAltPressedOnly(@NotNull KeyEvent e) {
        return e.isAltDown() && !e.isControlDown() && !e.isShiftDown();
    }

    private boolean processCharacter(@NotNull KeyEvent e, @NotNull char keyChar) {
        if (isAltPressedOnly(e) && mySettingsProvider.altSendsEscape()) {
            return false;
        }
        final char[] obuffer;
        obuffer = new char[]{keyChar};
        if (keyChar == '`' && e.isMetaDown()) {
            // Command + backtick is a short-cut on Mac OSX, so we shouldn't type anything
            return false;
        }
        myTerminalStarter.sendString(new String(obuffer), true);
        if (mySettingsProvider.scrollToBottomOnTyping()) {
            scrollToBottom();
        }
        return true;
    }

    private static boolean isCodeThatScrolls(KeyCode keycode) {
        return keycode == KeyCode.UP
                || keycode == KeyCode.DOWN
                || keycode == KeyCode.LEFT
                || keycode == KeyCode.RIGHT
                || keycode == KeyCode.BACK_SPACE
                || keycode == KeyCode.INSERT
                || keycode == KeyCode.DELETE
                || keycode == KeyCode.ENTER
                || keycode == KeyCode.HOME
                || keycode == KeyCode.END
                || keycode == KeyCode.PAGE_UP
                || keycode == KeyCode.PAGE_DOWN;
    }

    private boolean processTerminalKeyTyped(KeyEvent e) {
        if (hasUncommittedChars()) {
            return false;
        }
        var keyChar = e.getCharacter().charAt(0);
        if (e.getCharacter().length() == 0 || !Character.isISOControl((e.getCharacter().codePointAt(0)))) {
            // keys filtered out here will be processed in processTerminalKeyPressed
            try {
                return processCharacter(e, keyChar);
            } catch (Exception ex) {
                logger.error("Error sending typed key to emulator", ex);
            }
        }
        return false;
    }

    public void handlePaste() {
        pasteFromClipboard(false);
    }

    private void handlePasteSelection() {
        pasteFromClipboard(true);
    }

    /**
     * Copies selected text to clipboard.
     *
     * @param unselect                               true to unselect currently selected text
     * @param useSystemSelectionClipboardIfAvailable true to use {@link Toolkit#getSystemSelection()} if available
     */
    public void handleCopy(boolean unselect, boolean useSystemSelectionClipboardIfAvailable) {
        if (mySelection.get() != null) {
            Pair<Point, Point> points = mySelection.get().pointsForRun(myTermSize.getColumns());
            copySelection(points.getFirst(), points.getSecond(), useSystemSelectionClipboardIfAvailable);
            if (unselect) {
                updateSelection(null, true);
                repaint();
            }
        }
    }

    private boolean handleCopy(@Nullable KeyEvent e) {
        boolean ctrlC = e != null && e.getCode() == KeyCode.C && e.isControlDown()
                && !e.isAltDown() && !e.isMetaDown() && !e.isShiftDown();
        boolean sendCtrlC = ctrlC && mySelection.get() == null;
        handleCopy(ctrlC, false);
        return !sendCtrlC;
    }

    private void handleCopyOnSelect() {
        handleCopy(false, true);
    }

    /**
     * InputMethod implementation
     * For details read http://docs.oracle.com/javase/7/docs/technotes/guides/imf/api-tutorial.html
     */
    private void processInputMethodEvent(InputMethodEvent e) {
        if (e.getCommitted() == null) {
            return;
        }
        String committedText = e.getCommitted();
        if (!committedText.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (char c : committedText.toCharArray()) {
                if (c >= 0x20 && c != 0x7F) { // Filtering characters
                    sb.append(c);
                }
            }
            if (sb.length() > 0) {
                // Sending the committed text to myTerminalStarter
                myTerminalStarter.sendString(sb.toString(), true);
            }
        }

        // Handling uncommitted text (in-progress input)
        ObservableList<InputMethodTextRun> composedTextRuns = e.getComposed();
        if (!composedTextRuns.isEmpty()) {
            StringBuilder uncommittedTextBuilder = new StringBuilder();
            for (InputMethodTextRun run : composedTextRuns) {
                uncommittedTextBuilder.append(run.getText()); // Extracting text from the run
            }
            myInputMethodUncommittedChars = uncommittedTextBuilder.toString();
        } else {
            // Reset if there is no uncommitted text
            myInputMethodUncommittedChars = null;
        }
    }

    private static String uncommittedChars(@Nullable AttributedCharacterIterator text) {
        if (text == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char c = text.first(); c != CharacterIterator.DONE; c = text.next()) {
            if (c >= 0x20 && c != 0x7F) { // Hack just like in javax.swing.text.DefaultEditorKit.DefaultKeyTypedAction
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private class MyInputMethodRequests implements InputMethodRequests {

        @Override
        public Point2D getTextLocation(int i) {
            var x = myCursor.getCoordX() * myCharSize.getWidth() + getInsetX();
            var y = (myCursor.getCoordY() + 1) * myCharSize.getHeight();
            var screenBounds = canvas.localToScreen(canvas.getBoundsInLocal());
            double screenX = screenBounds.getMinX();
            double screenY = screenBounds.getMinY();
            //if user enables screen scaling in his operating system we must correct x and y
            Screen screen = resolveScreen();
            var point = new Point2D((x + screenX) * screen.getOutputScaleX(), (y + screenY) * screen.getOutputScaleY());
            return point;
        }

        @Override
        public int getLocationOffset(int i, int i1) {
            return 0;
        }

        @Override
        public void cancelLatestCommittedText() {

        }

        @Override
        public String getSelectedText() {
            return null;
        }

        /**
         * Returns the screen that shows the top left corner of the window.
         *
         * @return
         */
        private Screen resolveScreen() {
            Stage stage = (Stage) canvas.getScene().getWindow();
            Rectangle2D stageBounds = new Rectangle2D(stage.getX(), stage.getY(), 0, 0);
            var screens = Screen.getScreensForRectangle(stageBounds);
            if (!screens.isEmpty()) {
                return screens.get(0);
            } else {
                return Screen.getPrimary();
            }
        }
    }

    public void dispose() {
        myRepaintTimeLine.stop();
    }

    void logScrollBar() {
        logger.info("ScrollBar value: {}, visibleAmount: {}, min: {}, max: {}", this.scrollBar.getValue(),
                this.scrollBar.getVisibleAmount(), this.scrollBar.getMin(), this.scrollBar.getMax());
    }

    private static int getModifiersEx(KeyEvent event) {
        int modifiers = 0;
        if (event.isShiftDown()) {
            modifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        if (event.isControlDown()) {
            modifiers |= InputEvent.CTRL_DOWN_MASK;
        }
        if (event.isAltDown()) {
            modifiers |= InputEvent.ALT_DOWN_MASK;
        }
        if (event.isMetaDown()) {
            modifiers |= InputEvent.META_DOWN_MASK;
        }
        return modifiers;
    }

    private void updateSelection(TerminalSelection selection, boolean updateSelectedText) {
        this.updateSelectedText = updateSelectedText;
        //change listener -> updateSelectedText()
        mySelection.set(selection);
    }

    private void updateSelectedText() {
        if (this.updateSelectedText || mySelection.get() == null) {
            selectedText.set(getSelectedText());
        }
        this.updateSelectedText = true;
    }

//    private void doOnTextSelected() {
//        selectedText.set(getSelectedText());
//    }
//
//    private void doOnTextUnselected() {
//        mySelection.set(null);
//        selectedText.set(null);
//    }
}
