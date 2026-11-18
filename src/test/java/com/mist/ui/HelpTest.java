package com.mist.ui;

import com.mist.service.DemoData;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The help window (HELP-1 to HELP-3, SHELL-5): F1, the "?" buttons and the user menu open it on the current
 * screen's topic.
 */
class HelpTest extends MistUiTest {

    @Override
    protected void open() {
        navigator.show(View.LOGIN);
    }

    @Test
    void f1OnLoginOpensTheLoginTopic() {
        shortcut(new KeyCodeCombination(KeyCode.F1));

        assertTrue(navigator.isHelpShowing());
        assertEquals(msg("help.login.title"), text("topicTitle"));
        VBox body = node("topicBody");
        assertEquals(3, body.getChildren().size());
    }

    @Test
    void questionMarkOnRegisterOpensTheRegisterTopic() {
        click("registerLink");

        click("helpButton");

        assertEquals(msg("help.register.title"), text("topicTitle"));
    }

    @Test
    void userMenuOpensTheTopicOfTheScreenOnDisplay() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        menu("userMenu", "helpItem");

        assertEquals(msg("help.store.title"), text("topicTitle"));
        ListView<View> topics = node("topicList");
        assertFalse(topics.getItems().contains(View.ADMIN_LOG));
    }

    @Test
    void anotherTopicIsShownWhenPicked() {
        shortcut(new KeyCodeCombination(KeyCode.F1));
        ListView<View> topics = node("topicList");

        robot.interact(() -> topics.getSelectionModel().select(View.SHELL));

        assertEquals(msg("help.shell.title"), text("topicTitle"));
    }

    @Test
    void adminsGetTheOperationLogTopic() throws Exception {
        loginAs(DemoData.ADMIN_EMAIL);

        shortcut(new KeyCodeCombination(KeyCode.F1));

        assertEquals(msg("help.admin-log.title"), text("topicTitle"));
    }

    @Test
    void closeButtonClosesTheWindow() {
        shortcut(new KeyCodeCombination(KeyCode.F1));

        click("closeButton");

        assertFalse(navigator.isHelpShowing());
    }

    @Test
    void languageChangeRebuildsTheHelpInTheNewLanguage() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        shortcut(new KeyCodeCombination(KeyCode.F1));

        menu("userMenu", "englishItem");
        assertFalse(navigator.isHelpShowing());

        shortcut(new KeyCodeCombination(KeyCode.F1));
        assertEquals("The store", text("topicTitle"));
    }
}
