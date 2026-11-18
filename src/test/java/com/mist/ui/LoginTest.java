package com.mist.ui;

import com.mist.dao.DaoKeys;
import com.mist.model.Language;
import com.mist.model.Theme;
import com.mist.model.User;
import com.mist.service.DemoData;
import com.mist.service.ServiceKeys;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The login screen (LOGIN-1 to LOGIN-4), logout (SHELL-7) and the forced sign-out of a deactivated account (C-2).
 */
class LoginTest extends MistUiTest {

    @Override
    protected void open() {
        navigator.show(View.LOGIN);
    }

    @Test
    void playerLogsInAndReachesTheShell() {
        logIn(DemoData.PLAYER_EMAIL, DemoData.PASSWORD);

        assertTrue(navigator.current().inShell());
        assertTrue(context.session().isLoggedIn());
        assertEquals("lucia", text("userMenu"));
    }

    @Test
    void emailIsAcceptedInAnyCaseAndWithSpaces() {
        logIn("  Lucia@MIST.com ", DemoData.PASSWORD);

        assertTrue(context.session().isLoggedIn());
    }

    @Test
    void savedLanguageAndThemeApplyAfterLogin() throws Exception {
        User lucia = backend.services().auth().login(DemoData.PLAYER_EMAIL, DemoData.PASSWORD.toCharArray());
        backend.services().users().changePreferences(lucia.getId(), Language.EN, Theme.LIGHT);

        logIn(DemoData.PLAYER_EMAIL, DemoData.PASSWORD);

        assertEquals(Language.EN, I18n.language());
        assertEquals(Theme.LIGHT, navigator.theme());
        assertTrue(stage.getScene().getStylesheets().getLast().endsWith("mist-theme-light.css"));
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameMessage() {
        logIn(DemoData.PLAYER_EMAIL, "wrong-password1");
        assertEquals(msg(ServiceKeys.AUTH_BAD_CREDENTIALS), text("formMessage"));
        assertEquals("", text("passwordField"));
        assertFalse(context.session().isLoggedIn());

        logIn("nobody@mist.com", DemoData.PASSWORD);
        assertEquals(msg(ServiceKeys.AUTH_BAD_CREDENTIALS), text("formMessage"));
    }

    @Test
    void deactivatedAccountIsRefused() throws Exception {
        User marc = backend.services().auth().login(DemoData.SECOND_PLAYER_EMAIL, DemoData.PASSWORD.toCharArray());
        backend.services().users().deactivate(marc.getId());

        logIn(DemoData.SECOND_PLAYER_EMAIL, DemoData.PASSWORD);

        assertEquals(msg(ServiceKeys.ACCOUNT_DEACTIVATED), text("formMessage"));
        assertFalse(context.session().isLoggedIn());
    }

    @Test
    void blankFieldsAreMarkedWithoutCallingTheService() {
        int calls = db.calls();

        click("loginButton");

        assertEquals(msg("validation.required"), text("formMessage"));
        assertEquals(calls, db.calls());
    }

    @Test
    void databaseFailureOffersARetryThatLogsIn() {
        db.failNextCall();

        logIn(DemoData.PLAYER_EMAIL, DemoData.PASSWORD);
        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        assertFalse(context.session().isLoggedIn());

        click("dialogRetry");
        assertTrue(context.session().isLoggedIn());
    }

    @Test
    void languageToggleTranslatesTheScreenAndKeepsTheEmail() {
        type("emailField", "lucia@mist.com");

        click("enToggle");

        assertEquals("Log in", text("loginTitle"));
        assertEquals("lucia@mist.com", text("emailField"));
        click("esToggle");
        assertEquals("Inicia sesión", text("loginTitle"));
    }

    @Test
    void registerLinkOpensTheRegistrationScreen() {
        click("registerLink");

        assertEquals(View.REGISTER, navigator.current());
    }

    @Test
    void logoutReturnsToLogin() {
        logIn(DemoData.DEVELOPER_EMAIL, DemoData.PASSWORD);

        menu("userMenu", "logoutItem");

        assertEquals(View.LOGIN, navigator.current());
        assertFalse(context.session().isLoggedIn());
    }

    @Test
    void logoutHasAKeyboardShortcut() {
        logIn(DemoData.DEVELOPER_EMAIL, DemoData.PASSWORD);

        shortcut(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));

        assertEquals(View.LOGIN, navigator.current());
    }

    @Test
    void accountDeactivatedDuringTheSessionSignsOut() throws Exception {
        logIn(DemoData.PLAYER_EMAIL, DemoData.PASSWORD);
        backend.services().users().deactivate(context.session().userId());

        menu("userMenu", "englishItem");

        assertEquals(View.LOGIN, navigator.current());
        assertFalse(context.session().isLoggedIn());
        assertEquals(msg(ServiceKeys.ACCOUNT_DEACTIVATED), text("formMessage"));
    }

    /**
     * Fills in the form and presses the login button.
     *
     * @param email    the email to type
     * @param password the password to type
     */
    private void logIn(String email, String password) {
        type("emailField", email);
        type("passwordField", password);
        click("loginButton");
    }
}
