package com.mist.ui;

import com.mist.model.Game;
import com.mist.model.Player;
import com.mist.model.User;
import com.mist.service.DemoData;
import com.mist.service.ServiceKeys;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import com.mist.dao.DaoKeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wishlist (WISH-1 to WISH-4) and its link in the header (HEADER-1).
 */
class WishlistTest extends MistUiTest {

    @Test
    void theWishlistListsTheWishedGames() throws Exception {
        openWishlist();

        assertEquals(View.WISHLIST, navigator.current());
        assertEquals(1, list().getItems().size());
        assertEquals("Red Dead Redemption 2", list().getItems().getFirst().getName());
        assertTrue(this.<Button>node("addToCartButton").isDisable());
    }

    @Test
    void aGameMovesToTheCart() throws Exception {
        openWishlist();
        robot.interact(() -> list().getSelectionModel().select(0));

        click("addToCartButton");

        Player lucia = (Player) db.userByEmail(DemoData.PLAYER_EMAIL);
        ObjectId starlit = db.gameByName("Red Dead Redemption 2").getId();
        assertTrue(lucia.getCartGameIds().contains(starlit));
        assertFalse(lucia.getWishlistGameIds().contains(starlit));
        assertTrue(list().getItems().isEmpty());
        assertEquals("2", text("cartBadge"));
    }

    @Test
    void aGameIsRemovedFromTheContextMenu() throws Exception {
        openWishlist();
        robot.interact(() -> list().getSelectionModel().select(0));

        contextItem("removeItem");

        assertTrue(((Player) db.userByEmail(DemoData.PLAYER_EMAIL)).getWishlistGameIds().isEmpty());
        assertEquals(I18n.format("wishlist.removed", "Red Dead Redemption 2"), text("statusLabel"));
    }

    @Test
    void openingAGameShowsItsPage() throws Exception {
        openWishlist();
        robot.interact(() -> list().getSelectionModel().select(0));

        contextItem("openItem");

        assertEquals(View.GAME, navigator.current());
        assertEquals("Red Dead Redemption 2", text("nameTitle"));
    }

    @Test
    void aGameTakenOffSaleCannotMoveToTheCart() throws Exception {
        openWishlist();
        User rockstarStudios = backend.services().auth().login(DemoData.SECOND_DEVELOPER_EMAIL, DemoData.PASSWORD.toCharArray());
        backend.services().publishing().delist(rockstarStudios.getId(), db.gameByName("Red Dead Redemption 2").getId());
        robot.interact(() -> list().getSelectionModel().select(0));

        click("addToCartButton");

        assertEquals(msg(ServiceKeys.GAME_NOT_PURCHASABLE), dialogMessage());
        assertEquals(1, list().getItems().size());
    }

    @Test
    void anEmptyWishlistSaysSo() throws Exception {
        loginAs(DemoData.SECOND_PLAYER_EMAIL);
        click("wishlistLink");

        assertTrue(list().getItems().isEmpty());
    }

    @Test
    void aDatabaseFailureWhileMovingAGameOffersARetry() throws Exception {
        openWishlist();
        robot.interact(() -> list().getSelectionModel().select(0));
        db.failNextCall();

        click("addToCartButton");
        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        click("dialogRetry");

        assertTrue(list().getItems().isEmpty());
        assertEquals("2", text("cartBadge"));
    }

    /**
     * Logs in as the demo player and opens the wishlist from the header.
     *
     * @throws Exception if the login fails
     */
    private void openWishlist() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        click("wishlistLink");
    }

    /**
     * Returns the wishlist's list.
     *
     * @return the list
     */
    private ListView<Game> list() {
        return node("wishlistList");
    }

    /**
     * Fires an item of the wishlist's context menu.
     *
     * @param id the item's id
     */
    private void contextItem(String id) {
        MenuItem item = list().getContextMenu().getItems().stream().filter(i -> id.equals(i.getId()))
                .findFirst().orElseThrow();
        robot.interact(item::fire);
    }
}
