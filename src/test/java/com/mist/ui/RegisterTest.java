package com.mist.ui;

import com.mist.model.Developer;
import com.mist.model.ModelKeys;
import com.mist.model.Player;
import com.mist.model.User;
import com.mist.service.DemoData;
import com.mist.service.ServiceKeys;
import javafx.css.PseudoClass;
import javafx.scene.control.Control;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The registration screen (REGISTER-1 to REGISTER-3).
 */
class RegisterTest extends MistUiTest {

    /** The pseudo-class of a field in error. */
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    @Override
    protected void open() {
        navigator.show(View.REGISTER);
    }

    @Test
    void playerRegistersAndLandsOnLoginWithTheEmail() {
        fillAccount("ana@mist.local");
        fillPlayer("ana_b", "4111 1111 1111 1111");

        click("registerButton");

        assertEquals(View.LOGIN, navigator.current());
        assertEquals("ana@mist.local", text("emailField"));
        assertEquals(msg("register.done"), text("formMessage"));
        User ana = db.userByEmail("ana@mist.local");
        Player player = assertInstanceOf(Player.class, ana);
        assertEquals("ana_b", player.getNickname());
        assertEquals("1111", player.getCard().last4());
        assertEquals("+34", ana.getPhone().countryCode());
    }

    @Test
    void developerRegistersWithStudioAndIban() {
        click("developerRadio");
        assertFalse(visible("nicknameField"));
        assertTrue(visible("studioNameField"));
        fillAccount("studio@mist.local");
        type("studioNameField", "Moonlit Bytes");
        type("bankAccountField", "GB29 NWBK 6016 1331 9268 19");

        click("registerButton");

        assertEquals(View.LOGIN, navigator.current());
        Developer developer = assertInstanceOf(Developer.class, db.userByEmail("studio@mist.local"));
        assertEquals("Moonlit Bytes", developer.getStudioName());
        assertEquals("GB29NWBK60161331926819", developer.getBankAccount());
    }

    @Test
    void passwordsAreCheckedBeforeCallingTheService() {
        fillAccount("ana@mist.local");
        fillPlayer("ana_b", "4111111111111111");
        type("passwordField", "short");
        type("repeatField", "other");
        int calls = db.calls();

        click("registerButton");

        assertEquals(View.REGISTER, navigator.current());
        assertTrue(marked("passwordField"));
        assertTrue(marked("repeatField"));
        assertEquals(msg(ServiceKeys.AUTH_WEAK_PASSWORD), text("formMessage"));
        assertEquals(calls, db.calls());
    }

    @Test
    void takenEmailIsMarkedOnTheEmailField() {
        fillAccount(DemoData.PLAYER_EMAIL);
        fillPlayer("someone_new", "4111111111111111");

        click("registerButton");

        assertEquals(View.REGISTER, navigator.current());
        assertTrue(marked("emailField"));
        assertEquals(msg(ServiceKeys.USER_EMAIL_TAKEN), text("formMessage"));
    }

    @Test
    void takenNicknameIsMarkedOnTheNicknameField() {
        fillAccount("ana@mist.local");
        fillPlayer("lucia", "4111111111111111");

        click("registerButton");

        assertTrue(marked("nicknameField"));
        assertEquals(msg(ServiceKeys.USER_NICKNAME_TAKEN), text("formMessage"));
    }

    @Test
    void cardNumberFailingItsChecksumIsMarked() {
        fillAccount("ana@mist.local");
        fillPlayer("ana_b", "4111 1111 1111 1112");

        click("registerButton");

        assertTrue(marked("cardNumberField"));
        assertEquals(msg(ModelKeys.CARD_NUMBER), text("formMessage"));
        assertNull(db.userByEmail("ana@mist.local"));
    }

    @Test
    void expiredCardIsMarkedBeforeCallingTheService() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        fillAccount("ana@mist.local");
        fillPlayer("ana_b", "4111111111111111");
        choose("expiryYearBox", Year.now().getValue());
        choose("expiryMonthBox", lastMonth.getYear() == Year.now().getValue() ? lastMonth.getMonthValue() : 1);

        click("registerButton");

        if (lastMonth.getYear() == Year.now().getValue()) {
            assertTrue(marked("expiryMonthBox"));
            assertEquals(msg(ModelKeys.CARD_EXPIRED), text("formMessage"));
        }
    }

    @Test
    void halfAPhoneIsRefused() {
        fillAccount("ana@mist.local");
        fillPlayer("ana_b", "4111111111111111");
        type("phoneNumberField", "");

        click("registerButton");

        assertTrue(marked("phoneNumberField"));
        assertEquals(View.REGISTER, navigator.current());
    }

    @Test
    void backLinkReturnsToLogin() {
        click("backLink");

        assertEquals(View.LOGIN, navigator.current());
    }

    /**
     * Fills in the account and contact groups.
     *
     * @param email the email to register
     */
    private void fillAccount(String email) {
        type("firstNameField", "Ana");
        type("lastNameField", "Blanco");
        type("emailField", email);
        type("passwordField", "secret123");
        type("repeatField", "secret123");
        type("phoneCodeField", "+34");
        type("phoneNumberField", "612 345 678");
        type("streetField", "Gran Vía 1");
        type("postalCodeField", "28013");
        type("cityField", "Madrid");
    }

    /**
     * Fills in the player group.
     *
     * @param nickname   the nickname
     * @param cardNumber the card number
     */
    private void fillPlayer(String nickname, String cardNumber) {
        type("nicknameField", nickname);
        type("cardHolderField", "Ana Blanco");
        type("cardNumberField", cardNumber);
        choose("expiryMonthBox", 12);
        choose("expiryYearBox", Year.now().getValue() + 3);
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
