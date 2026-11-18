package com.mist.ui;

import com.mist.model.Achievement;
import com.mist.model.Game;
import com.mist.model.Player;
import com.mist.service.DemoData;
import com.mist.service.GameReview;
import com.mist.service.ServiceKeys;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A game's page (GAME-1 to GAME-7) and how the store opens it (STORE-3).
 */
class GameTest extends MistUiTest {

    @Test
    void theContextMenuOpensTheGamePage() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        select("newList", "Starlit Drift");

        MenuItem open = list("newList").getContextMenu().getItems().getFirst();
        robot.interact(open::fire);

        assertEquals(View.GAME, navigator.current());
        assertEquals("Starlit Drift", text("nameTitle"));
        assertEquals("Lumen Works", text("studioText"));
        assertEquals(price(db.gameByName("Starlit Drift")), text("priceLabel"));
        assertEquals(3, this.<FlowPane>node("tagsPane").getChildren().size());
        assertEquals(3, this.<HBox>node("thumbnails").getChildren().size());
        assertEquals("Windows 10 64-bit", text("osText"));
        assertTrue(text("languagesText").contains("japonés"));
        assertEquals("PEGI 12", text("pegiBadge"));
        assertEquals(3, this.<ListView<Achievement>>node("achievementsList").getItems().size());
    }

    @Test
    void enterOnAListAndTheBannerAlsoOpenGames() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        select("topList", "Ashen Lanterns");
        ListView<Game> top = list("topList");
        robot.interact(() -> top.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                false, false, false, false)));
        assertEquals("Ashen Lanterns", text("nameTitle"));

        click("backButton");
        assertEquals(View.STORE, navigator.current());
        click("featuredOpen");
        assertEquals("Starlit Drift", text("nameTitle"));
    }

    @Test
    void anOwnedGameSaysSoInsteadOfOfferingToBuy() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        openGame("Ashen Lanterns");

        assertTrue(visible("ownedLabel"));
        assertFalse(visible("addToCartButton"));
        assertFalse(visible("wishlistButton"));
    }

    @Test
    void addingToTheCartAndTheWishlistUpdatesTheBuyBox() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        openGame("Pixel Pantry");

        click("addToCartButton");
        assertFalse(visible("addToCartButton"));
        assertTrue(visible("goToCartButton"));

        click("wishlistButton");
        assertEquals(msg("game.onWishlist"), text("wishlistButton"));
        Player lucia = (Player) db.userByEmail(DemoData.PLAYER_EMAIL);
        ObjectId pantry = db.gameByName("Pixel Pantry").getId();
        assertTrue(lucia.getCartGameIds().contains(pantry));
        assertTrue(lucia.getWishlistGameIds().contains(pantry));
    }

    @Test
    void aGuestIsAskedToLogInToBuy() throws Exception {
        robot.interact(() -> navigator.show(View.LOGIN));
        click("guestLink");
        openGame("Starlit Drift");

        click("addToCartButton");
        assertEquals(msg("game.loginToBuy"), dialogMessage());
        click("dialogConfirm");

        assertEquals(View.LOGIN, navigator.current());
    }

    @Test
    void developersOnlySeeThePrice() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);

        openGame("Starlit Drift");

        assertTrue(visible("priceLabel"));
        assertFalse(visible("addToCartButton"));
        assertFalse(visible("wishlistButton"));
    }

    @Test
    void reviewsShowTheirAuthorsByPages() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        openGame("Ashen Lanterns");

        ListView<GameReview> reviews = node("reviewsList");
        assertEquals(2, reviews.getItems().size());
        assertTrue(reviews.getItems().stream().anyMatch(review -> "marc_f".equals(review.authorNickname())));
        assertEquals(1, this.<Pagination>node("reviewsPages").getPageCount());
        assertEquals(I18n.format("game.rating", "4,5", 2), text("ratingText"));
    }

    @Test
    void aGameThatNoLongerExistsWarnsAndGoesBack() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        robot.interact(() -> navigator.show(View.GAME, new ObjectId()));

        assertEquals(msg(ServiceKeys.GAME_NOT_FOUND), dialogMessage());
        assertEquals(View.STORE, navigator.current());
    }

    /**
     * Opens a game's page from the navigator.
     *
     * @param name the game's name
     */
    private void openGame(String name) {
        ObjectId id = db.gameByName(name).getId();
        robot.interact(() -> navigator.show(View.GAME, id));
    }

    /**
     * Returns a game list of the store.
     *
     * @param id the list's fx:id
     * @return the list
     */
    private ListView<Game> list(String id) {
        return node(id);
    }

    /**
     * Selects a game of a store list by name.
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
     * Formats a game's price as the page does.
     *
     * @param game the game
     * @return the price text
     */
    private static String price(Game game) {
        return com.mist.ui.support.Formats.price(game.getPrice());
    }
}
