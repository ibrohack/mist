package com.mist.ui.controller;

import com.mist.exception.UnauthorizedException;
import com.mist.model.Language;
import com.mist.model.ModelKeys;
import com.mist.model.Preferences;
import com.mist.model.User;
import com.mist.ui.I18n;
import com.mist.ui.LoginNotice;
import com.mist.ui.NavigationAware;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import com.mist.ui.support.FormValidator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller of the login screen: email and password, the language toggle, and the way to registration.
 *
 * <p>Implements the behaviors LOGIN-1 to LOGIN-4 of {@code docs/ui-design.md}.</p>
 */
public class LoginController implements NavigationAware {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");

    /** What the controller works with. */
    private final UiContext context;
    /** Checks the form before the service call. */
    private FormValidator form;

    /** The two language toggles. */
    @FXML
    private ToggleGroup languageGroup;
    /** Shows the screen in Spanish. */
    @FXML
    private ToggleButton esToggle;
    /** Shows the screen in English. */
    @FXML
    private ToggleButton enToggle;
    /** The account's email. */
    @FXML
    private TextField emailField;
    /** The account's password. */
    @FXML
    private PasswordField passwordField;
    /** Logs in; the form's default button. */
    @FXML
    private Button loginButton;
    /** Shows the result of the last attempt, or a notice from another screen. */
    @FXML
    private Label formMessage;
    /** Opens the registration screen. */
    @FXML
    private Hyperlink registerLink;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public LoginController(UiContext context) {
        this.context = context;
    }

    /** Marks the current language and declares the form's checks. */
    @FXML
    private void initialize() {
        esToggle.setSelected(I18n.language() == Language.ES);
        enToggle.setSelected(I18n.language() == Language.EN);
        form = new FormValidator(formMessage)
                .required(emailField).email(emailField, ModelKeys.USER_EMAIL)
                .required(passwordField);
    }

    @Override
    public void onNavigatedTo(Object parameter) {
        // LOGIN-4: fill in the email and show the notice another screen sent.
        if (parameter instanceof LoginNotice notice) {
            if (notice.email() != null) {
                emailField.setText(notice.email());
                passwordField.requestFocus();
            }
            if (notice.messageKey() != null) {
                form.showMessage(notice.messageKey(), notice.success());
            }
        }
    }

    /** LOGIN-1: checks the form and logs in. */
    @FXML
    private void onLogin() {
        if (!form.validate()) {
            return;
        }
        String email = emailField.getText().strip();
        char[] password = passwordField.getText().toCharArray();
        LOG.info("Login attempt");
        context.tasks().run(loginButton, () -> context.services().auth().login(email, password), this::loggedIn,
                this::loginFailed);
    }

    /**
     * LOGIN-1.R1: starts the session, applies the user's saved language and theme, and opens the home screen.
     *
     * @param user the user who logged in
     */
    private void loggedIn(User user) {
        LOG.info("Logged in as {} ({})", user.getEmail(), user.role());
        Preferences preferences = user.getPreferences();
        context.session().start(user);
        I18n.use(I18n.localeOf(preferences.language()));
        context.navigator().applyTheme(preferences.theme());
        context.navigator().home();
    }

    /**
     * LOGIN-1.R2 to R4: shows a refused login on the form; other failures go to the error reporter.
     *
     * @param error why the login failed
     */
    private void loginFailed(Throwable error) {
        if (error instanceof UnauthorizedException refused) {
            // R2 (wrong email or password) and R3 (deactivated account): same place, without saying which account
            // exists; the password is cleared for the next attempt.
            form.showMessage(refused.getMessage(), false);
            passwordField.clear();
            passwordField.requestFocus();
            return;
        }
        // R4: a database failure offers to try again.
        context.errors().report(context.navigator().window(), error, this::onLogin);
    }

    /** LOGIN-2: opens the registration screen. */
    @FXML
    private void onRegister() {
        context.navigator().show(View.REGISTER);
    }

    /**
     * LOGIN-3: shows the screen in the chosen language, keeping the email typed so far.
     *
     * @param event the action of {@code esToggle} or {@code enToggle}
     */
    @FXML
    private void onLanguage(ActionEvent event) {
        ToggleButton chosen = (ToggleButton) event.getSource();
        if (!chosen.isSelected()) {
            // Clicking the selected toggle again keeps the language.
            chosen.setSelected(true);
            return;
        }
        I18n.use(I18n.localeOf(chosen == enToggle ? Language.EN : Language.ES));
        context.navigator().show(View.LOGIN, new LoginNotice(emailField.getText(), null, true));
    }
}
