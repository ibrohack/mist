package com.mist.ui.controller;

import com.mist.model.Language;
import com.mist.model.Player;
import com.mist.model.Theme;
import com.mist.model.User;
import com.mist.ui.I18n;
import com.mist.ui.NavigationAware;
import com.mist.ui.ShellHost;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Controller of the shell: the header of a session with the brand, the navigation links and the user menu, around
 * the screen on display.
 *
 * <p>Implements the behaviors SHELL-1 to SHELL-7 of {@code docs/ui-design.md}.</p>
 */
public class ShellController implements ShellHost, NavigationAware {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");
    /** Size the avatar image is decoded at. */
    private static final double AVATAR_SIZE = 64;

    /** What the controller works with. */
    private final UiContext context;
    /** Refreshes the user menu when the session changes; kept here because the session holds it weakly. */
    private final ChangeListener<Number> sessionListener = (observable, before, after) -> showUser();

    /** The whole shell. */
    @FXML
    private BorderPane root;
    /** The header bar. */
    @FXML
    private HBox topBar;
    /** The navigation links, by role. */
    @FXML
    private HBox navBar;
    /** The user menu: avatar, name and the session's options. */
    @FXML
    private MenuButton userMenu;
    /** The avatar circle, filled with the user's image. */
    @FXML
    private Circle avatarCircle;
    /** The user's initials, shown while there is no avatar image. */
    @FXML
    private Label avatarInitials;
    /** Switches the UI to Spanish. */
    @FXML
    private RadioMenuItem spanishItem;
    /** Switches the UI to English. */
    @FXML
    private RadioMenuItem englishItem;
    /** Switches to the dark theme. */
    @FXML
    private RadioMenuItem darkItem;
    /** Switches to the light theme. */
    @FXML
    private RadioMenuItem lightItem;
    /** Where the screen on display goes. */
    @FXML
    private StackPane content;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public ShellController(UiContext context) {
        this.context = context;
    }

    /** Marks the current language and theme in the user menu. */
    @FXML
    private void initialize() {
        spanishItem.setSelected(I18n.language() == Language.ES);
        englishItem.setSelected(I18n.language() == Language.EN);
        darkItem.setSelected(context.navigator().theme() == Theme.DARK);
        lightItem.setSelected(context.navigator().theme() == Theme.LIGHT);
        context.session().versionProperty().addListener(new WeakChangeListener<>(sessionListener));
    }

    @Override
    public void onNavigatedTo(Object parameter) {
        // SHELL-1: show who is using MIST.
        showUser();
    }

    @Override
    public void showContent(View view, Node screen) {
        content.getChildren().setAll(screen);
    }

    /**
     * SHELL-3: switches the language, saves it for a logged-in user and rebuilds the screen.
     *
     * @param event the action of {@code spanishItem} or {@code englishItem}
     */
    @FXML
    private void onLanguage(ActionEvent event) {
        Language language = event.getSource() == englishItem ? Language.EN : Language.ES;
        LOG.info("Language changed to {}", language);
        I18n.use(I18n.localeOf(language));
        savePreferences();
    }

    /**
     * SHELL-4: switches the theme at once and saves it for a logged-in user.
     *
     * @param event the action of {@code darkItem} or {@code lightItem}
     */
    @FXML
    private void onTheme(ActionEvent event) {
        Theme theme = event.getSource() == lightItem ? Theme.LIGHT : Theme.DARK;
        LOG.info("Theme changed to {}", theme);
        context.navigator().applyTheme(theme);
        savePreferences();
    }

    /**
     * Saves the language and theme of a logged-in user, then rebuilds the screen in the language (SHELL-3.R1). If
     * the save fails, the choice still applies to this session (SHELL-3.R2) and the error is reported.
     */
    private void savePreferences() {
        if (!context.session().isLoggedIn()) {
            context.navigator().reload();
            return;
        }
        Language language = I18n.language();
        Theme theme = context.navigator().theme();
        context.tasks().run(userMenu,
                () -> context.services().users().changePreferences(context.session().userId(), language, theme),
                saved -> {
                    context.session().update(saved);
                    context.navigator().reload();
                },
                error -> {
                    context.navigator().reload();
                    context.errors().report(context.navigator().window(), error);
                });
    }

    /** SHELL-1: shows the user's name, initials and avatar in the user menu. */
    private void showUser() {
        User user = context.session().user().orElse(null);
        if (user == null) {
            userMenu.setText(I18n.text("shell.guest"));
            avatarInitials.setText("?");
            return;
        }
        userMenu.setText(user instanceof Player player ? player.getNickname() : user.fullName());
        avatarInitials.setText(initials(user));
        avatarCircle.setFill(null);
        avatarInitials.setVisible(true);
        context.images().load(user.getAvatarId(), AVATAR_SIZE, AVATAR_SIZE, this::showAvatar);
    }

    /**
     * Fills the avatar circle with the user's image.
     *
     * @param image the decoded avatar
     */
    private void showAvatar(Image image) {
        avatarCircle.setFill(new ImagePattern(image));
        avatarInitials.setVisible(false);
    }

    /**
     * Returns a user's initials, shown while there is no avatar.
     *
     * @param user the user
     * @return the first letters of the first and last name
     */
    private static String initials(User user) {
        return (user.getFirstName().substring(0, 1) + user.getLastName().substring(0, 1)).toUpperCase(Locale.ROOT);
    }
}
