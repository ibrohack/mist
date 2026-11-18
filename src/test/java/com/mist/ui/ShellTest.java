package com.mist.ui;

import com.mist.dao.DaoKeys;
import com.mist.model.Language;
import com.mist.model.Theme;
import com.mist.service.DemoData;
import javafx.scene.control.MenuButton;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shell's user menu (SHELL-1, SHELL-3 and SHELL-4 in {@code docs/ui-design.md}).
 */
class ShellTest extends MistUiTest {

    @Test
    void userMenuShowsThePlayersNicknameAndInitials() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        assertEquals("lucia", text("userMenu"));
        assertEquals("LR", text("avatarInitials"));
    }

    @Test
    void englishTranslatesTheScreenAndIsSavedForTheUser() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        menu("userMenu", "englishItem");

        assertEquals(Language.EN, I18n.language());
        MenuButton userMenu = node("userMenu");
        assertEquals("Language", userMenu.getItems().getFirst().getText());
        assertEquals(Language.EN, db.userByEmail(DemoData.PLAYER_EMAIL).getPreferences().language());
    }

    @Test
    void lightThemeAppliesAtOnceAndIsSavedForTheUser() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);

        menu("userMenu", "lightItem");

        assertTrue(stage.getScene().getStylesheets().getLast().endsWith("css/mist-theme-light.css"));
        assertEquals(Theme.LIGHT, db.userByEmail(DemoData.DEVELOPER_EMAIL).getPreferences().theme());

        menu("userMenu", "darkItem");
        assertTrue(stage.getScene().getStylesheets().getLast().endsWith("css/mist-theme.css"));
    }

    @Test
    void languageStillChangesForTheSessionWhenSavingFails() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        db.failNextCall();

        menu("userMenu", "englishItem");

        assertEquals(Language.EN, I18n.language());
        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        assertEquals(Language.ES, db.userByEmail(DemoData.PLAYER_EMAIL).getPreferences().language());
    }
}
