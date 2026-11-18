package com.mist.ui;

import com.mist.dao.DaoKeys;
import com.mist.model.ModelKeys;
import com.mist.model.Player;
import com.mist.model.User;
import com.mist.service.DemoData;
import com.mist.service.ServiceKeys;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Control;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The profile (PROFILE-1 to PROFILE-12): values by role, inline editors, password, avatar and deactivation.
 */
class ProfileTest extends MistUiTest {

    /** The pseudo-class of a field in error. */
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    /** Where the test images are written. */
    @TempDir
    Path folder;

    @Test
    void playerSeesTheirValuesAndNoDeveloperRows() throws Exception {
        openProfile(DemoData.PLAYER_EMAIL);

        assertEquals(View.PROFILE, navigator.current());
        assertEquals("Lucía Romero", text("nameValue"));
        assertEquals(DemoData.PLAYER_EMAIL, text("emailValue"));
        assertEquals("lucia", text("nicknameValue"));
        assertTrue(text("cardValue").contains("•••• 1111"));
        assertFalse(visible("studioValue"));
    }

    @Test
    void developerSeesStudioAndAMaskedIban() throws Exception {
        openProfile(DemoData.DEVELOPER_EMAIL);

        assertEquals("Nebula Forge", text("studioValue"));
        assertEquals("ES91 •••• 1332", text("bankValue"));
        assertFalse(visible("nicknameValue"));
    }

    @Test
    void nameIsEditedInPlaceAndSaved() throws Exception {
        openProfile(DemoData.PLAYER_EMAIL);

        click("nameEdit");
        assertTrue(visible("nameEditor"));
        assertEquals("Lucía", text("firstNameField"));
        type("firstNameField", "Lucy");
        click("nameSave");

        assertEquals("Lucy Romero", text("nameValue"));
        assertFalse(visible("nameEditor"));
        assertEquals(msg("profile.saved"), text("formMessage"));
        assertEquals("Lucy", db.userByEmail(DemoData.PLAYER_EMAIL).getFirstName());
        assertEquals("Lucy", context.session().requireUser().getFirstName());
    }

    @Test
    void onlyOneEditorIsOpenAndCancelDiscards() throws Exception {
        openProfile(DemoData.PLAYER_EMAIL);

        click("nameEdit");
        type("firstNameField", "Changed");
        click("emailEdit");
        assertFalse(visible("nameEditor"));
        assertTrue(visible("emailEditor"));
        click("emailCancel");

        assertFalse(visible("emailEditor"));
        assertEquals("Lucía Romero", text("nameValue"));
    }

    @Test
    void takenEmailIsMarkedOnItsField() throws Exception {
        openProfile(DemoData.PLAYER_EMAIL);

        click("emailEdit");
        type("emailField", DemoData.SECOND_PLAYER_EMAIL);
        click("emailSave");

        assertTrue(marked("emailField"));
        assertEquals(msg(ServiceKeys.USER_EMAIL_TAKEN), text("formMessage"));
    }

    @Test
    void takenNicknameIsMarkedOnItsField() throws Exception {
        openProfile(DemoData.PLAYER_EMAIL);

        click("nicknameEdit");
        type("nicknameField", "marc_f");
        click("nicknameSave");

        assertTrue(marked("nicknameField"));
        assertEquals(msg(ServiceKeys.USER_NICKNAME_TAKEN), text("formMessage"));
    }

    @Test
    void newCardReplacesTheSavedOne() throws Exception {
        openProfile(DemoData.PLAYER_EMAIL);

        click("cardEdit");
        type("cardNumberField", "5555 5555 5555 4444");
        choose("expiryMonthBox", 6);
        choose("expiryYearBox", Year.now().getValue() + 2);
        click("cardSave");

        assertTrue(text("cardValue").startsWith("Mastercard •••• 4444"));
        assertEquals("4444", ((Player) db.userByEmail(DemoData.PLAYER_EMAIL)).getCard().last4());
    }

    @Test
    void wrongCurrentPasswordIsMarkedAndTheSessionStays() throws Exception {
        openProfile(DemoData.PLAYER_EMAIL);

        changePassword("not-my-password1", "brandnew123");

        assertTrue(marked("currentPasswordField"));
        assertEquals(msg(ServiceKeys.AUTH_WRONG_PASSWORD), text("formMessage"));
        assertTrue(context.session().isLoggedIn());
        assertEquals(View.PROFILE, navigator.current());
    }

    @Test
    void passwordIsChanged() throws Exception {
        openProfile(DemoData.PLAYER_EMAIL);

        changePassword(DemoData.PASSWORD, "brandnew123");

        assertEquals(msg("profile.passwordChanged"), text("formMessage"));
        assertNotNull(backend.services().auth().login(DemoData.PLAYER_EMAIL, "brandnew123".toCharArray()));
    }

    @Test
    void avatarIsUploadedAndShownInTheUserMenu() throws Exception {
        openProfile(DemoData.PLAYER_EMAIL);
        files.answerImage(png("me.png"));

        click("changeAvatarButton");

        assertNotNull(db.userByEmail(DemoData.PLAYER_EMAIL).getAvatarId());
        assertEquals(2, robot.lookup("#avatarImage").queryAll().stream().filter(Node::isVisible).count());

        click("removeAvatarButton");
        click("dialogConfirm");
        assertNull(db.userByEmail(DemoData.PLAYER_EMAIL).getAvatarId());
    }

    @Test
    void oversizedAvatarIsRefused() throws Exception {
        openProfile(DemoData.PLAYER_EMAIL);
        Path big = folder.resolve("big.png");
        Files.write(big, new byte[3 * 1024 * 1024]);
        files.answerImage(big);

        click("changeAvatarButton");

        assertEquals(msg(ModelKeys.MEDIA_SIZE), dialogMessage());
        assertNull(db.userByEmail(DemoData.PLAYER_EMAIL).getAvatarId());
    }

    @Test
    void unreadableFileIsReported() throws Exception {
        openProfile(DemoData.PLAYER_EMAIL);
        files.answerImage(folder.resolve("gone.png"));

        click("changeAvatarButton");

        assertEquals(msg("error.fileUnreadable"), dialogMessage());
    }

    @Test
    void cancelledFileChooserDoesNothing() throws Exception {
        openProfile(DemoData.PLAYER_EMAIL);
        int calls = db.calls();

        click("changeAvatarButton");

        assertFalse(dialogShowing());
        assertEquals(calls, db.calls());
    }

    @Test
    void deactivationAsksFirstThenSignsOut() throws Exception {
        openProfile(DemoData.DEVELOPER_EMAIL);

        click("deactivateButton");
        assertEquals(msg("profile.deactivate.confirmDeveloper"), dialogMessage());
        click("dialogCancel");
        assertTrue(context.session().isLoggedIn());

        click("deactivateButton");
        click("dialogConfirm");

        assertEquals(View.LOGIN, navigator.current());
        assertEquals(msg("profile.deactivated"), text("formMessage"));
        User developer = db.userByEmail(DemoData.DEVELOPER_EMAIL);
        assertFalse(developer.isActive());
        assertTrue(backend.services().store().newReleases(20).stream()
                .noneMatch(game -> game.getDeveloperId().equals(developer.getId())));
    }

    @Test
    void databaseFailureWhileSavingOffersARetry() throws Exception {
        openProfile(DemoData.PLAYER_EMAIL);
        click("nameEdit");
        type("lastNameField", "Romera");
        db.failNextCall();

        click("nameSave");
        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        click("dialogRetry");

        assertEquals("Lucía Romera", text("nameValue"));
    }

    /**
     * Logs in and opens the profile from the user menu.
     *
     * @param email the demo account's email
     * @throws Exception if the login fails
     */
    private void openProfile(String email) throws Exception {
        loginAs(email);
        menu("userMenu", "profileItem");
    }

    /**
     * Fills in and saves the password editor.
     *
     * @param current  the current password to type
     * @param password the new password, typed twice
     */
    private void changePassword(String current, String password) {
        click("passwordEdit");
        type("currentPasswordField", current);
        type("newPasswordField", password);
        type("repeatPasswordField", password);
        click("passwordSave");
    }

    /**
     * Writes a small PNG image.
     *
     * @param name the file name
     * @return the file
     * @throws Exception if writing fails
     */
    private Path png(String name) throws Exception {
        Path file = folder.resolve(name);
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", file.toFile());
        return file;
    }

    /**
     * Tells whether a field shows the error mark.
     *
     * @param id the field's fx:id
     * @return {@code true} if it has the {@code :error} pseudo-class
     */
    private boolean marked(String id) {
        Control field = node(id);
        return field.getPseudoClassStates().contains(ERROR);
    }
}
