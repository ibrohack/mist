package com.mist.ui;

import com.mist.dao.DaoKeys;
import com.mist.model.User;
import com.mist.service.AddressForm;
import com.mist.service.CardForm;
import com.mist.service.DemoData;
import com.mist.service.OwnedGame;
import com.mist.service.PhoneForm;
import com.mist.service.PlayerSignUp;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The library (LIB-1 to LIB-3) and its header link (HEADER-1).
 */
class LibraryTest extends MistUiTest {

    @Test
    void theLibraryListsTheOwnedGamesNewestFirst() throws Exception {
        openLibrary(DemoData.PLAYER_EMAIL);

        assertEquals(View.LIBRARY, navigator.current());
        assertEquals(List.of("Super Mario Odyssey", "Metroid Prime", "Grand Theft Auto V", "Super Mario Galaxy"), names());
        assertEquals(4, this.<TilePane>node("gameGrid").getChildren().size());
        assertFalse(visible("emptyState"));
    }

    @Test
    void recentPurchasesSayUntilWhenTheyCanBeReturned() throws Exception {
        openLibrary(DemoData.PLAYER_EMAIL);

        assertTrue(caption("Super Mario Odyssey").startsWith(msg("library.returnableUntil").substring(0, 10)));
        assertEquals("", caption("Grand Theft Auto V"));
    }

    @Test
    void searchSortAndTagFilterTheListAndTheGrid() throws Exception {
        openLibrary(DemoData.PLAYER_EMAIL);

        type("searchField", "auto");
        assertEquals(List.of("Grand Theft Auto V"), names());
        assertEquals(1, this.<TilePane>node("gameGrid").getChildren().size());

        type("searchField", "");
        choose("sortBox", "library.sort.name");
        assertEquals("Grand Theft Auto V", names().getFirst());

        choose("tagFilter", "space");
        assertEquals(List.of("Super Mario Galaxy"), names());
    }

    @Test
    void theContextMenuOpensAGame() throws Exception {
        openLibrary(DemoData.PLAYER_EMAIL);
        robot.interact(() -> list().getSelectionModel().select(2));

        MenuItem open = list().getContextMenu().getItems().getFirst();
        robot.interact(open::fire);

        assertEquals(View.GAME, navigator.current());
        assertEquals("Grand Theft Auto V", text("nameTitle"));
    }

    @Test
    void anEmptyLibraryShowsTheBrandEmptyState() throws Exception {
        backend.services().auth().registerPlayer(new PlayerSignUp("Eva", "Sanz", "eva@mist.com",
                DemoData.PASSWORD.toCharArray(), new PhoneForm(null, null),
                new AddressForm("Calle Sol 2", null, "41001", "Sevilla", null, "ES"), "eva_s",
                new CardForm("Eva Sanz", "4111111111111111", 12, Year.now().getValue() + 2)));

        openLibrary("eva@mist.com");

        assertTrue(visible("emptyState"));
        assertEquals(msg("library.empty.title"), text("emptyTitle"));
        click("browseButton");
        assertEquals(View.STORE, navigator.current());
    }

    @Test
    void developersHaveNoLibraryLink() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);

        assertFalse(visible("libraryLink"));
    }

    @Test
    void aDatabaseFailureLoadingTheLibraryOffersARetry() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        db.failNextCall();

        click("libraryLink");
        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        click("dialogRetry");

        assertEquals(4, list().getItems().size());
    }

    /**
     * Logs in and opens the library from the header.
     *
     * @param email the player's email
     * @throws Exception if the login fails
     */
    private void openLibrary(String email) throws Exception {
        User user = loginAs(email);
        assertTrue(user.getId() != null);
        click("libraryLink");
    }

    /**
     * Returns the list of owned games.
     *
     * @return the list
     */
    private ListView<OwnedGame> list() {
        return node("gameList");
    }

    /**
     * Returns the names in the list, in order.
     *
     * @return the names
     */
    private List<String> names() {
        return list().getItems().stream().map(item -> item.game().getName()).toList();
    }

    /**
     * Returns the caption of a game's card.
     *
     * @param name the game's name
     * @return the card's caption
     */
    private String caption(String name) {
        TilePane grid = node("gameGrid");
        for (Node card : grid.getChildren()) {
            if (((OwnedGame) card.getUserData()).game().getName().equals(name)) {
                return ((Label) ((VBox) card).getChildren().get(2)).getText();
            }
        }
        throw new AssertionError("No card for " + name);
    }
}
