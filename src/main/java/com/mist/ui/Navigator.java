package com.mist.ui;

import com.mist.model.Theme;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Moves the main window between screens.
 *
 * <p>Every screen change goes through here: the navigator loads the screen's view with {@link SceneRouter}, puts it
 * on the window, and then tells a {@link NavigationAware} controller which parameter it was opened with. Screens that
 * fill the window replace its content; screens of a session are shown inside the shell, which is loaded the first
 * time one of them opens. The navigator remembers the in-shell screens visited, for {@link #back()}, and applies the
 * theme to the window.</p>
 */
public final class Navigator {

    /**
     * A screen visited inside the shell.
     *
     * @param view      the screen
     * @param parameter the parameter it was opened with
     */
    private record Visit(View view, Object parameter) {
    }

    /** The main window. */
    private final Stage stage;
    /** What the controllers of the loaded views get. */
    private final UiContext context;
    /** The in-shell screens visited before the current one, latest first. */
    private final Deque<Visit> history = new ArrayDeque<>();
    /** The shell on display, or {@code null} while a window-filling screen shows. */
    private ShellHost shell;
    /** The screen on display. */
    private View current;
    /** The parameter the current screen was opened with. */
    private Object currentParameter;
    /** The theme of the window. */
    private Theme theme = Theme.DARK;

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
        if (view.inShell()) {
            if (current != null && current.inShell()) {
                history.push(new Visit(current, currentParameter));
            }
            showInShell(view, parameter);
        } else {
            showInWindow(view, parameter);
        }
    }

    /** Goes back to the previous in-shell screen, if any. */
    public void back() {
        if (!history.isEmpty()) {
            Visit previous = history.pop();
            showInShell(previous.view(), previous.parameter());
        }
    }

    /**
     * Tells whether {@link #back()} has a screen to go back to.
     *
     * @return {@code true} if an earlier in-shell screen was visited
     */
    public boolean canGoBack() {
        return !history.isEmpty();
    }

    /** Rebuilds the screen on display, and the shell around it, after a language change. */
    public void reload() {
        if (current == null) {
            return;
        }
        if (current.inShell()) {
            shell = null;
            showInShell(current, currentParameter);
        } else {
            showInWindow(current, currentParameter);
        }
    }

    /** Opens the shell of a session that just started, on its first screen. */
    public void home() {
        history.clear();
        show(View.SHELL);
    }

    /**
     * Changes the theme of the window at once.
     *
     * @param newTheme dark or light
     */
    public void applyTheme(Theme newTheme) {
        this.theme = Objects.requireNonNull(newTheme, "newTheme");
        if (stage.getScene() != null) {
            Styles.applyTheme(stage.getScene(), newTheme);
        }
    }

    /**
     * Returns the theme of the window.
     *
     * @return dark or light
     */
    public Theme theme() {
        return theme;
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
     * Shows a screen that fills the window, leaving any shell.
     *
     * @param view      the screen
     * @param parameter its parameter
     */
    private void showInWindow(View view, Object parameter) {
        SceneRouter.Loaded loaded = load(view);
        setRoot(loaded.root());
        shell = loaded.controller() instanceof ShellHost host ? host : null;
        if (shell == null) {
            history.clear();
        }
        current = view;
        currentParameter = parameter;
        notifyShown(loaded.controller(), parameter);
    }

    /**
     * Shows a screen inside the shell, loading the shell first if it is not on display.
     *
     * @param view      the screen
     * @param parameter its parameter
     */
    private void showInShell(View view, Object parameter) {
        if (shell == null) {
            SceneRouter.Loaded frame = load(View.SHELL);
            setRoot(frame.root());
            shell = (ShellHost) frame.controller();
            notifyShown(frame.controller(), null);
        }
        SceneRouter.Loaded loaded = load(view);
        shell.showContent(view, loaded.root());
        current = view;
        currentParameter = parameter;
        notifyShown(loaded.controller(), parameter);
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
            Styles.applyTheme(scene, theme);
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
