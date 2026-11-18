package com.mist.ui;

import com.mist.dao.DaoKeys;
import com.mist.model.Game;
import com.mist.model.ModelKeys;
import com.mist.model.Player;
import com.mist.model.User;
import com.mist.service.DemoData;
import com.mist.service.ServiceKeys;
import com.mist.ui.support.Formats;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cart and the checkout (CART-1 to CART-5) and the header's cart badge (HEADER-1).
 */
class CartTest extends MistUiTest {

    @Test
    void theCartListsItsGamesWithTheTotalAndTheCard() throws Exception {
        openCart(DemoData.PLAYER_EMAIL);

        assertEquals(View.CART, navigator.current());
        assertEquals(1, cart().getItems().size());
        assertEquals("Gravewind Tactics", cart().getItems().getFirst().getName());
        assertEquals(I18n.format("cart.total", Formats.money(new BigDecimal("34.99"))), text("totalLabel"));
        assertTrue(text("cardLabel").contains("•••• 1111"));
        assertEquals("1", text("cartBadge"));
    }

    @Test
    void removingAGameUpdatesTheTotalAndTheBadge() throws Exception {
        openCart(DemoData.PLAYER_EMAIL);

        Button remove = robot.lookup("#removeButton").queryAs(Button.class);
        robot.interact(remove::fire);

        assertTrue(cart().getItems().isEmpty());
        assertEquals(I18n.format("cart.total", Formats.money(BigDecimal.ZERO)), text("totalLabel"));
        assertFalse(visible("cartBadge"));
        assertTrue(((Player) db.userByEmail(DemoData.PLAYER_EMAIL)).getCartGameIds().isEmpty());
    }

    @Test
    void aGameCanMoveToTheWishlist() throws Exception {
        openCart(DemoData.PLAYER_EMAIL);
        robot.interact(() -> cart().getSelectionModel().select(0));

        contextItem("moveItem");

        Player lucia = (Player) db.userByEmail(DemoData.PLAYER_EMAIL);
        ObjectId gravewind = db.gameByName("Gravewind Tactics").getId();
        assertTrue(lucia.getWishlistGameIds().contains(gravewind));
        assertFalse(lucia.getCartGameIds().contains(gravewind));
    }

    @Test
    void checkoutNeedsTheTermsAndBuysTheGames() throws Exception {
        openCart(DemoData.PLAYER_EMAIL);
        assertTrue(this.<Button>node("checkoutButton").isDisable());

        click("termsCheck");
        assertFalse(this.<Button>node("checkoutButton").isDisable());
        click("checkoutButton");
        assertTrue(dialogMessage().contains(Formats.money(new BigDecimal("34.99"))));
        click("dialogConfirm");

        assertEquals(I18n.format("cart.done", 1, Formats.money(new BigDecimal("34.99"))), dialogMessage());
        assertTrue(cart().getItems().isEmpty());
        Player lucia = (Player) db.userByEmail(DemoData.PLAYER_EMAIL);
        assertTrue(lucia.owns(db.gameByName("Gravewind Tactics").getId()));
        assertTrue(((Player) context.session().requireUser()).owns(db.gameByName("Gravewind Tactics").getId()));
        assertFalse(visible("cartBadge"));
    }

    @Test
    void cancellingTheConfirmationBuysNothing() throws Exception {
        openCart(DemoData.PLAYER_EMAIL);
        click("termsCheck");

        click("checkoutButton");
        click("dialogCancel");

        assertEquals(1, cart().getItems().size());
        assertFalse(((Player) db.userByEmail(DemoData.PLAYER_EMAIL)).owns(db.gameByName("Gravewind Tactics").getId()));
    }

    @Test
    void anExpiredCardIsRefused() throws Exception {
        openCart(DemoData.PLAYER_EMAIL);
        clock.advance(Duration.ofDays(5 * 366));

        checkOut();

        assertEquals(msg(ModelKeys.CARD_EXPIRED), dialogMessage());
        assertEquals(1, cart().getItems().size());
    }

    @Test
    void aGameTakenOffSaleMeanwhileIsReported() throws Exception {
        openCart(DemoData.PLAYER_EMAIL);
        User nebula = backend.services().auth().login(DemoData.DEVELOPER_EMAIL, DemoData.PASSWORD.toCharArray());
        backend.services().publishing().delist(nebula.getId(), db.gameByName("Gravewind Tactics").getId());

        checkOut();

        assertEquals(msg(ServiceKeys.GAME_NOT_PURCHASABLE), dialogMessage());
    }

    @Test
    void aDatabaseFailureDuringCheckoutOffersARetry() throws Exception {
        openCart(DemoData.PLAYER_EMAIL);
        db.failNextCall();

        checkOut();
        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        click("dialogRetry");

        assertTrue(cart().getItems().isEmpty());
    }

    @Test
    void theTermsCanBeRead() throws Exception {
        openCart(DemoData.PLAYER_EMAIL);

        click("termsLink");

        assertEquals(msg("cart.terms.text"), dialogMessage());
    }

    @Test
    void anEmptyCartLeadsBackToTheStore() throws Exception {
        openCart(DemoData.SECOND_PLAYER_EMAIL);

        assertTrue(cart().getItems().isEmpty());
        assertFalse(visible("cartBadge"));
        click("browseLink");
        assertEquals(View.STORE, navigator.current());
    }

    @Test
    void theGamePageLeadsToTheCart() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        ObjectId gravewind = db.gameByName("Gravewind Tactics").getId();
        robot.interact(() -> navigator.show(View.GAME, gravewind));

        click("goToCartButton");

        assertEquals(View.CART, navigator.current());
    }

    /**
     * Logs in and opens the cart from the header.
     *
     * @param email the demo player's email
     * @throws Exception if the login fails
     */
    private void openCart(String email) throws Exception {
        loginAs(email);
        click("cartLink");
    }

    /** Accepts the terms, checks out and confirms. */
    private void checkOut() {
        click("termsCheck");
        click("checkoutButton");
        click("dialogConfirm");
    }

    /**
     * Returns the cart's list.
     *
     * @return the list
     */
    private ListView<Game> cart() {
        return node("cartList");
    }

    /**
     * Fires an item of the cart's context menu.
     *
     * @param id the item's id
     */
    private void contextItem(String id) {
        MenuItem item = cart().getContextMenu().getItems().stream().filter(i -> id.equals(i.getId()))
                .findFirst().orElseThrow();
        robot.interact(item::fire);
    }
}
