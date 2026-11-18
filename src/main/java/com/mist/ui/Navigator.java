package com.mist.ui;

import com.mist.model.Role;
import com.mist.model.Theme;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
 * time one of them opens. The navigator remembers the in-shell screens visited, for {@link #back()}, applies the
 * theme to the window, and opens the help window on the current screen's topic (F1 anywhere).</p>
 */
public final class Navigator {

    /** The FXML of the help window. */
    private static final String HELP_VIEW = "view/help.fxml";
    /** The smallest width of the help window, so the topics and the text both stay readable. */
    private static final double HELP_MIN_WIDTH = 560;
    /** The smallest height of the help window. */
    private static final double HELP_MIN_HEIGHT = 400;
    /** Opens the help window. */
    private static final KeyCombination HELP_KEY = new KeyCodeCombination(KeyCode.F1);

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
    /** The help window, created the first time it opens. */
    private Stage helpStage;
    /** The content of the help window. */
    private HelpPanel helpPanel;

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
        context.errors().onAccountClosed(this::signOut);
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
        closeHelp();
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

    /**
     * Ends the session and returns to the login screen (SHELL-7), with a message when the session did not end by
     * choice, such as a deactivated account.
     *
     * @param noticeKey the bundle key of the message to show on the login screen, or {@code null}
     */
    public void signOut(String noticeKey) {
        if (context.isConnected()) {
            context.services().auth().logout();
        }
        context.session().end();
        closeHelp();
        show(View.LOGIN, noticeKey == null ? null : new LoginNotice(null, noticeKey, false));
    }

    /**
     * HELP-1: opens the help window, or brings it to the front, on the topic of the current screen.
     *
     * @throws UncheckedIOException if the help view is broken, which is a programming error
     */
    public void showHelp() {
        if (helpStage == null) {
            SceneRouter.Loaded loaded;
            try {
                loaded = SceneRouter.load(HELP_VIEW, context);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot load " + HELP_VIEW, e);
            }
            helpPanel = (HelpPanel) loaded.controller();
            Scene scene = new Scene(loaded.root());
            Styles.install(scene);
            Styles.applyTheme(scene, theme);
            helpStage = new Stage();
            helpStage.initOwner(stage);
            helpStage.setTitle(I18n.text("help.title"));
            helpStage.getIcons().setAll(stage.getIcons());
            helpStage.setScene(scene);
            helpStage.setMinWidth(HELP_MIN_WIDTH);
            helpStage.setMinHeight(HELP_MIN_HEIGHT);
        }
        helpPanel.select(current == null ? View.SHELL : current);
        helpStage.show();
        helpStage.toFront();
    }

    /**
     * Tells whether the help window is open.
     *
     * @return {@code true} while it shows
     */
    public boolean isHelpShowing() {
        return helpStage != null && helpStage.isShowing();
    }

    /** Closes the help window, so it is rebuilt in the new language or for the next user. */
    private void closeHelp() {
        if (helpStage != null) {
            helpStage.hide();
            helpStage = null;
            helpPanel = null;
        }
    }

    /** Opens the shell of a session that just started, on the first screen of the user's role. */
    public void home() {
        history.clear();
        current = null;
        View first = View.STORE;
        if (context.session().hasRole(Role.ADMIN)) {
            first = View.ADMIN_LOG;
        } else if (context.session().hasRole(Role.DEVELOPER)) {
            first = View.STUDIO;
        }
        show(first);
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
        if (helpStage != null) {
            Styles.applyTheme(helpStage.getScene(), newTheme);
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
        // F1 opens the help on every screen; set again after each change, as menus also register shortcuts.
        scene.getAccelerators().put(HELP_KEY, this::showHelp);
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
