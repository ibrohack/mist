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
import com.mist.dao.DaoKeys;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.WindowEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A game's page (GAME-1 to GAME-10), its copy menus, and how the store opens it (STORE-3).
 */
class GameTest extends MistUiTest {

    @Test
    void theContextMenuOpensTheGamePage() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        select("newList", "Red Dead Redemption 2");

        MenuItem open = list("newList").getContextMenu().getItems().getFirst();
        robot.interact(open::fire);

        assertEquals(View.GAME, navigator.current());
        assertEquals("Red Dead Redemption 2", text("nameTitle"));
        assertEquals("Rockstar Studios", text("studioText"));
        assertEquals(price(db.gameByName("Red Dead Redemption 2")), text("priceLabel"));
        assertEquals(3, this.<FlowPane>node("tagsPane").getChildren().size());
        assertEquals(3, this.<HBox>node("thumbnails").getChildren().size());
        assertEquals("Windows 10 64-bit", text("osText"));
        assertTrue(text("languagesText").contains("japonés"));
        assertEquals("PEGI 18", text("pegiBadge"));
        assertEquals(3, this.<ListView<Achievement>>node("achievementsList").getItems().size());
    }

    @Test
    void enterOnAListAndTheBannerAlsoOpenGames() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        select("topList", "Grand Theft Auto V");
        ListView<Game> top = list("topList");
        robot.interact(() -> top.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                false, false, false, false)));
        assertEquals("Grand Theft Auto V", text("nameTitle"));

        click("backButton");
        assertEquals(View.STORE, navigator.current());
        click("featuredOpen");
        assertEquals("Red Dead Redemption 2", text("nameTitle"));
    }

    @Test
    void anOwnedGameSaysSoInsteadOfOfferingToBuy() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        openGame("Grand Theft Auto V");

        assertTrue(visible("ownedLabel"));
        assertFalse(visible("addToCartButton"));
        assertFalse(visible("wishlistButton"));
    }

    @Test
    void addingToTheCartAndTheWishlistUpdatesTheBuyBox() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        openGame("Tony Hawk's Pro Skater 2");

        click("addToCartButton");
        assertFalse(visible("addToCartButton"));
        assertTrue(visible("goToCartButton"));

        click("wishlistButton");
        assertEquals(msg("game.onWishlist"), text("wishlistButton"));
        Player lucia = (Player) db.userByEmail(DemoData.PLAYER_EMAIL);
        ObjectId pantry = db.gameByName("Tony Hawk's Pro Skater 2").getId();
        assertTrue(lucia.getCartGameIds().contains(pantry));
        assertTrue(lucia.getWishlistGameIds().contains(pantry));
    }

    @Test
    void aGuestIsAskedToLogInToBuy() throws Exception {
        robot.interact(() -> navigator.show(View.LOGIN));
        click("guestLink");
        openGame("Red Dead Redemption 2");

        click("addToCartButton");
        assertEquals(msg("game.loginToBuy"), dialogMessage());
        click("dialogConfirm");

        assertEquals(View.LOGIN, navigator.current());
    }

    @Test
    void developersOnlySeeThePrice() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);

        openGame("Red Dead Redemption 2");

        assertTrue(visible("priceLabel"));
        assertFalse(visible("addToCartButton"));
        assertFalse(visible("wishlistButton"));
    }

    @Test
    void reviewsShowTheirAuthorsByPages() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        openGame("Grand Theft Auto V");

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

    @Test
    void aDatabaseFailureWhileAddingToTheCartOffersARetry() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        openGame("Tony Hawk's Pro Skater 2");
        db.failNextCall();

        click("addToCartButton");
        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        click("dialogRetry");

        assertTrue(visible("goToCartButton"));
        Player lucia = (Player) db.userByEmail(DemoData.PLAYER_EMAIL);
        assertTrue(lucia.getCartGameIds().contains(db.gameByName("Tony Hawk's Pro Skater 2").getId()));
    }

    @Test
    void anAchievementIsCopiedFromItsMenu() throws Exception {
        primeClipboard();
        loginAs(DemoData.PLAYER_EMAIL);
        openGame("Red Dead Redemption 2");
        ListView<Achievement> achievements = node("achievementsList");
        Achievement first = achievements.getItems().getFirst();
        robot.interact(() -> achievements.getSelectionModel().select(first));

        fire(achievements.getContextMenu(), "copyAchievementItem");

        String expected = first.name() + ": " + first.description();
        assertEquals(expected, clipboard(expected));
    }

    @Test
    void aReviewIsCopiedFromItsMenuOnceOneIsSelected() throws Exception {
        primeClipboard();
        loginAs(DemoData.PLAYER_EMAIL);
        openGame("Grand Theft Auto V");
        ListView<GameReview> reviews = node("reviewsList");
        ContextMenu menu = reviews.getContextMenu();
        robot.interact(() -> menu.getOnShowing().handle(new WindowEvent(menu, WindowEvent.WINDOW_SHOWING)));
        assertTrue(item(menu, "copyReviewItem").isDisable());

        // Select, open the menu and copy in one step: the reviews page may reload between pulses.
        GameReview[] copied = new GameReview[1];
        boolean[] disabled = new boolean[1];
        robot.interact(() -> {
            reviews.getSelectionModel().select(0);
            copied[0] = reviews.getSelectionModel().getSelectedItem();
            menu.getOnShowing().handle(new WindowEvent(menu, WindowEvent.WINDOW_SHOWING));
            disabled[0] = item(menu, "copyReviewItem").isDisable();
            item(menu, "copyReviewItem").fire();
        });

        assertFalse(disabled[0]);
        String expected = copied[0].authorNickname() + " (" + copied[0].review().getRating() + "/5): "
                + copied[0].review().getComment();
        assertEquals(expected, clipboard(expected));
    }

    /**
     * Returns an item of a context menu.
     *
     * @param menu the menu
     * @param id   the item's fx:id
     * @return the item
     */
    private static MenuItem item(ContextMenu menu, String id) {
        return menu.getItems().stream().filter(i -> id.equals(i.getId())).findFirst().orElseThrow();
    }

    /**
     * Fires an item of a context menu.
     *
     * @param menu the menu
     * @param id   the item's fx:id
     */
    private void fire(ContextMenu menu, String id) {
        robot.interact(item(menu, id)::fire);
    }

    /**
     * Writes a marker to the system clipboard until it can be read back. On Windows the first clipboard write of a
     * process can be lost while another program holds the clipboard, so a copy test starts from a clipboard known to
     * work.
     *
     * @throws InterruptedException if the test is interrupted while waiting
     */
    private void primeClipboard() throws InterruptedException {
        String marker = "mist-test-" + System.nanoTime();
        ClipboardContent content = new ClipboardContent();
        content.putString(marker);
        String[] text = new String[1];
        for (int attempt = 0; attempt < 40 && !marker.equals(text[0]); attempt++) {
            robot.interact(() -> {
                Clipboard.getSystemClipboard().setContent(content);
                text[0] = Clipboard.getSystemClipboard().getString();
            });
            if (!marker.equals(text[0])) {
                Thread.sleep(50);
            }
        }
    }

    /**
     * Reads the system clipboard on the JavaFX thread, waiting up to two seconds for a text: on Windows another
     * program (clipboard history, an editor) may hold the clipboard for a moment, and a read then sees the old text.
     *
     * @param expected the text the test waits for
     * @return the clipboard's text, the expected one unless it never arrived
     * @throws InterruptedException if the test is interrupted while waiting
     */
    private String clipboard(String expected) throws InterruptedException {
        String[] text = new String[1];
        for (int attempt = 0; attempt < 40; attempt++) {
            robot.interact(() -> text[0] = Clipboard.getSystemClipboard().getString());
            if (expected.equals(text[0])) {
                break;
            }
            Thread.sleep(50);
        }
        return text[0];
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
