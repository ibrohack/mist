package com.mist.ui;

import com.mist.dao.DaoKeys;
import com.mist.model.Game;
import com.mist.model.ModelKeys;
import com.mist.model.Player;
import com.mist.model.User;
import com.mist.service.DemoData;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The store (STORE-1, STORE-2, STORE-4, STORE-5) and the guest entry (LOGIN-5).
 */
class StoreTest extends MistUiTest {

    @Test
    void playersLandOnTheStoreWithItsThreeLists() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        assertEquals(View.STORE, navigator.current());
        assertEquals(11, list("newList").getItems().size());
        assertEquals("Starlit Drift", list("newList").getItems().getFirst().getName());
        assertEquals("Ashen Lanterns", list("topList").getItems().getFirst().getName());
        assertEquals(List.of("Moss & Marrow", "Paper Kites"), names(list("freeList")));
        assertEquals(5, this.<Pagination>node("featured").getPageCount());
    }

    @Test
    void searchShowsTheMatchesAndClearingItGoesBack() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        search("ashen");
        assertTrue(visible("resultsList"));
        assertFalse(visible("newList"));
        assertEquals(List.of("Ashen Lanterns"), names(list("resultsList")));

        search("");
        assertFalse(visible("resultsList"));
        assertTrue(visible("featured"));
    }

    @Test
    void searchWithoutMatchesSaysSo() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        search("zzz");

        assertTrue(list("resultsList").getItems().isEmpty());
        assertEquals(msg("store.noResults"), ((Label) list("resultsList").getPlaceholder()).getText());
    }

    @Test
    void contextMenuPutsAGameInTheCart() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        select("newList", "Starlit Drift");

        contextItem("newList", "cartItem");

        Player lucia = (Player) db.userByEmail(DemoData.PLAYER_EMAIL);
        assertTrue(lucia.getCartGameIds().contains(db.gameByName("Starlit Drift").getId()));
        assertEquals(I18n.format("store.addedToCart", "Starlit Drift"), text("statusLabel"));
    }

    @Test
    void contextMenuPutsAGameOnTheWishlist() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        select("freeList", "Moss & Marrow");

        contextItem("freeList", "wishlistItem");

        Player lucia = (Player) db.userByEmail(DemoData.PLAYER_EMAIL);
        assertTrue(lucia.getWishlistGameIds().contains(db.gameByName("Moss & Marrow").getId()));
    }

    @Test
    void anOwnedGameCannotBeAddedAgain() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        select("topList", "Ashen Lanterns");
        ContextMenu menu = list("topList").getContextMenu();
        robot.interact(() -> menu.getOnShowing().handle(null));
        assertTrue(item(menu, "cartItem").isDisable());

        // Even if the item were fired, the service refuses it.
        robot.interact(() -> item(menu, "cartItem").fire());
        assertEquals(msg(ModelKeys.PLAYER_ALREADY_OWNED), dialogMessage());
    }

    @Test
    void developersAndGuestsCanOnlyOpenGames() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        assertEquals(List.of("openItem"), itemIds(list("newList").getContextMenu()));

        menu("userMenu", "logoutItem");
        click("guestLink");
        assertEquals(View.STORE, navigator.current());
        assertTrue(context.session().isGuest());
        assertEquals(List.of("openItem"), itemIds(list("newList").getContextMenu()));
        assertEquals(msg("shell.guest"), text("userMenu"));
    }

    @Test
    void aGuestLeavesThroughLogIn() throws Exception {
        robot.interact(() -> navigator.show(View.LOGIN));
        click("guestLink");

        assertEquals(msg("menu.login"), menuItem("userMenu", "logoutItem").orElseThrow().getText());
        menu("userMenu", "logoutItem");

        assertEquals(View.LOGIN, navigator.current());
        assertFalse(context.session().isGuest());
    }

    @Test
    void databaseFailureLoadingTheStoreOffersARetry() throws Exception {
        User lucia = backend.services().auth().login(DemoData.PLAYER_EMAIL, DemoData.PASSWORD.toCharArray());
        db.failNextCall();

        robot.interact(() -> {
            context.session().start(lucia);
            navigator.home();
        });
        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        assertTrue(list("newList").getItems().isEmpty());

        click("dialogRetry");
        assertEquals(11, list("newList").getItems().size());
    }

    /**
     * Types a search and presses Enter.
     *
     * @param text the search text
     */
    private void search(String text) {
        TextField field = node("searchField");
        robot.interact(() -> {
            field.setText(text);
            field.fireEvent(new ActionEvent());
        });
    }

    /**
     * Returns a game list.
     *
     * @param id the list's fx:id
     * @return the list
     */
    private ListView<Game> list(String id) {
        return node(id);
    }

    /**
     * Selects a game of a list by name.
     *
     * @param listId the list's fx:id
     * @param name   the game's name
     */
    private void select(String listId, String name) {
        ListView<Game> list = list(listId);
        Game game = list.getItems().stream().filter(g -> g.getName().equals(name)).findFirst().orElseThrow();
        robot.interact(() -> list.getSelectionModel().select(game));
    }

    /**
     * Fires an item of a list's context menu.
     *
     * @param listId the list's fx:id
     * @param itemId the item's id
     */
    private void contextItem(String listId, String itemId) {
        MenuItem item = item(list(listId).getContextMenu(), itemId);
        robot.interact(item::fire);
    }

    /**
     * Finds a menu item by id.
     *
     * @param menu the menu
     * @param id   the item's id
     * @return the item
     */
    private static MenuItem item(ContextMenu menu, String id) {
        return menu.getItems().stream().filter(i -> id.equals(i.getId())).findFirst().orElseThrow();
    }

    /**
     * Returns the ids of a menu's items.
     *
     * @param menu the menu
     * @return the ids, in order
     */
    private static List<String> itemIds(ContextMenu menu) {
        return menu.getItems().stream().map(MenuItem::getId).toList();
    }

    /**
     * Returns the names of a list's games.
     *
     * @param list the list
     * @return the names, in order
     */
    private static List<String> names(ListView<Game> list) {
        return list.getItems().stream().map(Game::getName).toList();
    }
}
