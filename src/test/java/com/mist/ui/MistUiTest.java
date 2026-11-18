package com.mist.ui;

import com.mist.fake.FakeBackend;
import com.mist.fake.FakeDatabase;
import com.mist.fake.MutableClock;
import com.mist.fake.StubFilePicker;
import com.mist.logging.DbOperationLogger;
import com.mist.ui.support.Dialogs;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Labeled;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Base of the UI tests: MIST's real views, controllers and services on an in-memory data layer.
 *
 * <p>Each test gets a fresh {@link FakeDatabase}, a clock stopped at {@link #NOW}, and a {@link UiContext} whose
 * service calls run on the JavaFX thread, so the screen is up to date as soon as an interaction returns. Interactions
 * fire the controls' own events on the JavaFX thread ({@link #click}, {@link #type}, {@link #choose}) instead of
 * moving the real mouse, so the tests do not take over the desktop.</p>
 */
public abstract class MistUiTest {

    /** The instant the test clock starts at. */
    protected static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");

    /** Drives the JavaFX thread and looks nodes up. */
    protected final FxRobot robot = new FxRobot();
    /** The in-memory database. */
    protected FakeDatabase db;
    /** The services' clock. */
    protected MutableClock clock;
    /** The backend the UI connects to. */
    protected FakeBackend backend;
    /** Answers the file dialogs. */
    protected StubFilePicker files;
    /** The UI context of the window under test. */
    protected UiContext context;
    /** The navigator of the window under test. */
    protected Navigator navigator;
    /** The window under test. */
    protected Stage stage;

    /**
     * Starts the app on the in-memory backend in TestFX's primary window, on the JavaFX thread.
     *
     * @throws Exception if JavaFX does not start in time
     */
    @BeforeEach
    void launch() throws Exception {
        Stage primary = FxToolkit.registerPrimaryStage();
        FxToolkit.setupFixture(() -> {
            try {
                start(primary);
            } catch (Exception e) {
                throw new IllegalStateException("The UI under test did not start", e);
            }
        });
    }

    /**
     * Builds the app on the in-memory backend and opens the first screen.
     *
     * @param stage the window TestFX provides
     * @throws Exception if preparing the data fails
     */
    private void start(Stage stage) throws Exception {
        I18n.use(I18n.DEFAULT_LOCALE);
        this.stage = stage;
        stage.setScene(null);
        db = new FakeDatabase();
        clock = new MutableClock(NOW);
        backend = new FakeBackend(db, clock);
        files = new StubFilePicker();
        prepare();
        context = new UiContext(backend, files, Runnable::run);
        navigator = new Navigator(stage, context);
        stage.setWidth(1280);
        stage.setHeight(800);
        open();
        stage.show();
    }

    /**
     * Prepares data before the UI starts; the default does nothing.
     *
     * @throws Exception if preparing fails
     */
    protected void prepare() throws Exception {
        // Nothing by default.
    }

    /** Opens the first screen; the default shows the splash. */
    protected void open() {
        navigator.show(View.SPLASH);
    }

    /** Closes the dialogs and windows a test left open and restores the language. */
    @AfterEach
    void closeWindows() {
        robot.interact(() -> {
            for (Window window : List.copyOf(Window.getWindows())) {
                if (window != stage) {
                    window.hide();
                }
            }
        });
        I18n.use(I18n.DEFAULT_LOCALE);
        DbOperationLogger.clearUser();
    }

    /**
     * Finds a node by id in any open window.
     *
     * @param id  the fx:id
     * @param <T> the node type
     * @return the node
     */
    protected <T extends Node> T node(String id) {
        return robot.lookup("#" + id).query();
    }

    /**
     * Tells whether a node with that id is in an open window and visible.
     *
     * @param id the fx:id
     * @return {@code true} if the user could see it
     */
    protected boolean visible(String id) {
        Optional<Node> found = robot.lookup("#" + id).tryQuery();
        if (found.isEmpty()) {
            return false;
        }
        for (Node node = found.get(); node != null; node = node.getParent()) {
            if (!node.isVisible()) {
                return false;
            }
        }
        return found.get().getScene() != null && found.get().getScene().getWindow().isShowing();
    }

    /**
     * Types a text into a field, replacing what it had.
     *
     * @param id   the field's fx:id
     * @param text the text
     */
    protected void type(String id, String text) {
        TextInputControl field = node(id);
        robot.interact(() -> field.setText(text));
    }

    /**
     * Clicks a button, link, toggle, radio button or check box by firing its action.
     *
     * @param id the control's fx:id
     */
    protected void click(String id) {
        ButtonBase button = node(id);
        robot.interact(button::fire);
    }

    /**
     * Chooses a value in a combo box or date picker.
     *
     * @param id    the control's fx:id
     * @param value the value
     * @param <T>   the value type
     */
    protected <T> void choose(String id, T value) {
        ComboBoxBase<T> box = node(id);
        robot.interact(() -> box.setValue(value));
    }

    /**
     * Fires a menu item of a menu button, looking inside its sub-menus.
     *
     * @param menuButtonId the menu button's fx:id
     * @param itemId       the item's fx:id
     */
    protected void menu(String menuButtonId, String itemId) {
        MenuButton button = node(menuButtonId);
        MenuItem item = find(button.getItems(), itemId)
                .orElseThrow(() -> new AssertionError("No menu item " + itemId));
        robot.interact(item::fire);
    }

    /**
     * Returns a menu item of a menu button.
     *
     * @param menuButtonId the menu button's fx:id
     * @param itemId       the item's fx:id
     * @return the item, or empty
     */
    protected Optional<MenuItem> menuItem(String menuButtonId, String itemId) {
        MenuButton button = node(menuButtonId);
        return find(button.getItems(), itemId);
    }

    /**
     * Runs the action of a keyboard shortcut of the window, such as F1.
     *
     * @param combination the shortcut
     */
    protected void shortcut(KeyCombination combination) {
        Runnable action = stage.getScene().getAccelerators().get(combination);
        if (action == null) {
            throw new AssertionError("No shortcut " + combination.getDisplayText());
        }
        robot.interact(action);
    }

    /**
     * Returns the text of a label, button or field.
     *
     * @param id the node's fx:id
     * @return its text
     */
    protected String text(String id) {
        Node node = node(id);
        return switch (node) {
            case Labeled labeled -> labeled.getText();
            case TextInputControl field -> field.getText();
            default -> throw new AssertionError(id + " has no text");
        };
    }

    /**
     * Returns the message of the open dialog.
     *
     * @return the dialog's text
     */
    protected String dialogMessage() {
        return text(Dialogs.MESSAGE_ID);
    }

    /**
     * Tells whether a dialog is open.
     *
     * @return {@code true} while a MIST dialog shows
     */
    protected boolean dialogShowing() {
        return visible(Dialogs.MESSAGE_ID);
    }

    /**
     * Returns a message of the current language.
     *
     * @param key the bundle key
     * @return the message
     */
    protected static String msg(String key) {
        return I18n.text(key);
    }

    /**
     * Searches menu items, including sub-menus, by id.
     *
     * @param items the items
     * @param id    the id
     * @return the item, or empty
     */
    private static Optional<MenuItem> find(List<MenuItem> items, String id) {
        for (MenuItem item : items) {
            if (id.equals(item.getId())) {
                return Optional.of(item);
            }
            if (item instanceof Menu menu) {
                Optional<MenuItem> inner = find(menu.getItems(), id);
                if (inner.isPresent()) {
                    return inner;
                }
            }
        }
        return Optional.empty();
    }
}
