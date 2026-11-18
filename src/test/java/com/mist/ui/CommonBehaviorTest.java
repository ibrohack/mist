package com.mist.ui;

import com.mist.dao.DaoKeys;
import com.mist.exception.DataAccessException;
import com.mist.exception.DuplicateEntityException;
import com.mist.exception.UnauthorizedException;
import com.mist.exception.ValidationException;
import com.mist.service.ServiceKeys;
import com.mist.ui.support.FormValidator;
import javafx.css.PseudoClass;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The common behavior of every screen (C-1 to C-3 in {@code docs/ui-design.md}): background calls, the reaction to
 * each kind of error, and form validation.
 */
class CommonBehaviorTest extends MistUiTest {

    /** The pseudo-class of a field in error. */
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    @Test
    void databaseFailureOffersARetryThatRunsTheWorkAgain() {
        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<String> result = new AtomicReference<>();

        robot.interact(() -> context.tasks().run(null, () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new DataAccessException(DaoKeys.FAILURE);
            }
            return "done";
        }, result::set));

        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        assertNull(result.get());
        click("dialogRetry");
        assertEquals("done", result.get());
        assertEquals(2, attempts.get());
    }

    @Test
    void cancellingTheRetryLeavesTheWorkUndone() {
        AtomicInteger attempts = new AtomicInteger();

        robot.interact(() -> context.tasks().run(null, () -> {
            attempts.incrementAndGet();
            throw new DataAccessException(DaoKeys.FAILURE);
        }, ignored -> { }));
        click("dialogCancel");

        assertEquals(1, attempts.get());
        assertFalse(dialogShowing());
    }

    @Test
    void ruleViolationsShowAWarningInTheCurrentLanguage() {
        robot.interact(() -> context.tasks().run(null, () -> {
            throw new ValidationException(ServiceKeys.STORE_EMPTY_CART);
        }, ignored -> { }));
        assertEquals(msg(ServiceKeys.STORE_EMPTY_CART), dialogMessage());
        click("dialogOk");

        I18n.use(I18n.ENGLISH);
        robot.interact(() -> context.tasks().run(null, () -> {
            throw new UnauthorizedException(ServiceKeys.GAME_NOT_OWNER);
        }, ignored -> { }));
        assertEquals("This game belongs to another developer.", dialogMessage());
    }

    @Test
    void unexpectedErrorsShowAGenericMessage() {
        robot.interact(() -> context.tasks().run(null, () -> {
            throw new IllegalStateException("bug");
        }, ignored -> { }));

        assertEquals(msg("error.unexpected"), dialogMessage());
    }

    @Test
    void validatorMarksEveryFailingFieldAndShowsTheFirstMessage() {
        TextField email = new TextField("not an email");
        PasswordField password = new PasswordField();
        PasswordField repeat = new PasswordField();
        Label message = new Label();
        FormValidator form = new FormValidator(message)
                .required(email).email(email, "model.user.email")
                .password(password)
                .sameText(repeat, password);
        robot.interact(() -> {
            password.setText("short1");
            repeat.setText("other");
        });

        robot.interact(() -> assertFalse(form.validate()));

        assertTrue(marked(email));
        assertTrue(marked(password));
        assertTrue(marked(repeat));
        assertEquals(msg("model.user.email"), message.getText());

        robot.interact(() -> {
            email.setText("ana@mist.local");
            password.setText("longer123");
            repeat.setText("longer123");
        });
        assertFalse(marked(email));
        robot.interact(() -> assertTrue(form.validate()));
        assertEquals("", message.getText());
    }

    @Test
    void serviceRejectionsAreMarkedOnTheBoundField() {
        TextField email = new TextField("ana@mist.local");
        Label message = new Label();
        FormValidator form = new FormValidator(message).bind(ServiceKeys.USER_EMAIL_TAKEN, email);

        robot.interact(() -> assertTrue(form.showServiceError(new DuplicateEntityException(ServiceKeys.USER_EMAIL_TAKEN))));
        assertTrue(marked(email));
        assertEquals(msg(ServiceKeys.USER_EMAIL_TAKEN), message.getText());

        robot.interact(() -> assertFalse(form.showServiceError(new DataAccessException(DaoKeys.FAILURE))));
    }

    /**
     * Tells whether a field shows the error mark.
     *
     * @param field the field
     * @return {@code true} if it has the {@code :error} pseudo-class
     */
    private static boolean marked(Control field) {
        return field.getPseudoClassStates().contains(ERROR);
    }
}
