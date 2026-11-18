package com.mist.ui;

import com.mist.service.DemoData;
import javafx.scene.control.ToggleButton;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The header's navigation links (HEADER-1 to HEADER-3).
 */
class HeaderTest extends MistUiTest {

    @Test
    void playersSeeTheStoreButNotTheLog() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        assertTrue(visible("storeLink"));
        assertFalse(visible("logLink"));
        assertTrue(link("storeLink").isSelected());
    }

    @Test
    void adminsSeeTheLogLinkHighlighted() throws Exception {
        loginAs(DemoData.ADMIN_EMAIL);

        assertTrue(visible("logLink"));
        assertTrue(link("logLink").isSelected());

        click("storeLink");
        assertEquals(View.STORE, navigator.current());
        assertTrue(link("storeLink").isSelected());
        assertFalse(link("logLink").isSelected());
    }

    @Test
    void theStoreLinkLeavesTheProfile() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        menu("userMenu", "profileItem");
        assertFalse(link("storeLink").isSelected());

        click("storeLink");

        assertEquals(View.STORE, navigator.current());
        assertTrue(link("storeLink").isSelected());
    }

    @Test
    void clickingTheCurrentLinkKeepsItHighlighted() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        click("storeLink");

        assertTrue(link("storeLink").isSelected());
        assertEquals(View.STORE, navigator.current());
    }

    /**
     * Returns a navigation link.
     *
     * @param id the link's fx:id
     * @return the link
     */
    private ToggleButton link(String id) {
        return node(id);
    }
}
