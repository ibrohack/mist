package com.mist.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Moves the main window between screens.
 *
 * <p>Every screen change goes through here: the navigator loads the screen's view with {@link SceneRouter}, puts it
 * on the window, and then tells a {@link NavigationAware} controller which parameter it was opened with.</p>
 */
public final class Navigator {

    /** The main window. */
    private final Stage stage;
    /** What the controllers of the loaded views get. */
    private final UiContext context;
    /** The screen on display. */
    private View current;
    /** The parameter the current screen was opened with. */
    private Object currentParameter;

    /**
     * Creates the navigator of the main window and attaches it to the context.
     *
     * @param stage   the main window
     * @param context the context the controllers get
     */
    public Navigator(Stage stage, UiContext context) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.context = Objects.requireNonNull(context, "context");
        context.attach(this);
    }

    /**
     * Shows a screen without a parameter.
     *
     * @param view the screen to show
     */
    public void show(View view) {
        show(view, null);
    }

    /**
     * Shows a screen and passes it a parameter, such as the game a game page shows.
     *
     * @param view      the screen to show
     * @param parameter what the screen's controller gets in {@link NavigationAware#onNavigatedTo(Object)}
     * @throws UncheckedIOException if the screen's FXML file is broken, which is a programming error
     */
    public void show(View view, Object parameter) {
        SceneRouter.Loaded loaded = load(view);
        setRoot(loaded.root());
        current = view;
        currentParameter = parameter;
        notifyShown(loaded.controller(), parameter);
    }

    /** Rebuilds the screen on display, after a language change, keeping its parameter. */
    public void reload() {
        if (current != null) {
            show(current, currentParameter);
        }
    }

    /**
     * Returns the screen on display.
     *
     * @return the current screen, or {@code null} before the first one
     */
    public View current() {
        return current;
    }

    /**
     * Returns the main window, the owner of dialogs and file choosers.
     *
     * @return the main window
     */
    public Window window() {
        return stage;
    }

    /**
     * Loads a screen's view with the context.
     *
     * @param view the screen
     * @return the root and the controller
     */
    private SceneRouter.Loaded load(View view) {
        try {
            return SceneRouter.load(view, context);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load " + view.fxml(), e);
        }
    }

    /**
     * Puts a root node on the window, creating the styled scene the first time.
     *
     * @param root the new content of the window
     */
    private void setRoot(Parent root) {
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            Styles.install(scene);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
    }

    /**
     * Tells a controller its screen is shown.
     *
     * @param controller the controller of the shown view
     * @param parameter  the navigation parameter
     */
    private static void notifyShown(Object controller, Object parameter) {
        if (controller instanceof NavigationAware aware) {
            aware.onNavigatedTo(parameter);
        }
    }
}
