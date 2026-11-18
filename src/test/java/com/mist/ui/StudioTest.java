package com.mist.ui;

import com.mist.model.Game;
import com.mist.model.GameStatus;
import com.mist.model.ModelKeys;
import com.mist.service.DemoData;
import com.mist.ui.support.Formats;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import com.mist.dao.DaoKeys;
import com.mist.exception.InvalidDocumentException;
import com.mist.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The developer dashboard (STUDIO-1 to STUDIO-3, STUDIO-5, STUDIO-6), its editable price column, the new game
 * dialog (NEW-1, NEW-2), its header link, and what the user sees when the database fails or refuses a value.
 */
class StudioTest extends MistUiTest {

    @Test
    void developersLandOnTheirDashboard() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);

        assertEquals(View.STUDIO, navigator.current());
        assertTrue(visible("studioLink"));
        assertEquals("Rockstar North", text("studioTitle"));
        assertEquals(3, table().getItems().size());
        assertEquals(Formats.money(new BigDecimal("59.98")), text("revenueLabel"));
        assertEquals("2", text("unitsLabel"));
        assertEquals("2", text("publishedLabel"));
        BarChart<String, Number> chart = node("revenueChart");
        assertEquals(2, chart.getData().getFirst().getData().size());
    }

    @Test
    void aPriceIsEditedInPlaceAndSaved() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);

        editPrice("Grand Theft Auto IV", "17,50");

        assertEquals(new BigDecimal("17.50"), db.gameByName("Grand Theft Auto IV").getPrice());
        assertEquals(new BigDecimal("17.50"), row("Grand Theft Auto IV").getPrice());
    }

    @Test
    void aWrongPriceGoesBackAndWarns() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);

        editPrice("Grand Theft Auto IV", "1500");

        assertEquals(msg(ModelKeys.GAME_PRICE), dialogMessage());
        assertEquals(new BigDecimal("19.99"), db.gameByName("Grand Theft Auto IV").getPrice());
    }

    @Test
    void aNewGameStartsAsADraft() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);

        click("newGameButton");
        type("nameField", "Hollow Echo");
        type("priceField", "12,99");
        click("dialogConfirm");

        assertEquals(View.GAME_EDITOR, navigator.current());
        assertEquals("Hollow Echo", text("titleLabel"));
        click("backButton");
        assertEquals(4, table().getItems().size());
        assertEquals(GameStatus.DRAFT, row("Hollow Echo").getStatus());
        assertEquals(new BigDecimal("12.99"), row("Hollow Echo").getPrice());
    }

    @Test
    void aNewGameNeedsAValidPrice() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);

        click("newGameButton");
        type("nameField", "Hollow Echo");
        type("priceField", "free");
        click("dialogConfirm");

        assertEquals(msg(ModelKeys.GAME_PRICE), text("formMessage"));
        assertEquals(3, table().getItems().size());
    }

    @Test
    void aCompleteDraftIsPublished() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        select(DemoData.DRAFT_GAME);
        assertTrue(this.<Button>node("delistButton").isDisable());

        click("publishButton");
        click("dialogConfirm");

        assertEquals(GameStatus.PUBLISHED, db.gameByName(DemoData.DRAFT_GAME).getStatus());
        assertEquals("3", text("publishedLabel"));
    }

    @Test
    void anIncompleteDraftCannotBePublished() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        click("newGameButton");
        type("nameField", "Hollow Echo");
        type("priceField", "5");
        click("dialogConfirm");
        click("backButton");
        select("Hollow Echo");

        click("publishButton");
        click("dialogConfirm");

        assertEquals(msg(ModelKeys.GAME_INCOMPLETE), dialogMessage());
        assertEquals(GameStatus.DRAFT, db.gameByName("Hollow Echo").getStatus());
    }

    @Test
    void aPublishedGameIsTakenOffSale() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        select("Grand Theft Auto V");
        assertTrue(this.<Button>node("publishButton").isDisable());

        click("delistButton");
        click("dialogConfirm");

        assertEquals(GameStatus.DELISTED, row("Grand Theft Auto V").getStatus());
        assertTrue(backend.services().store().newReleases(50).stream()
                .noneMatch(game -> game.getName().equals("Grand Theft Auto V")));
    }

    @Test
    void playersHaveNoStudioLink() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        assertFalse(visible("studioLink"));
    }

    @Test
    void aDatabaseFailureWhileSavingAPricePutsTheOldOneBack() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        db.failNextCall();

        editPrice("Grand Theft Auto IV", "17,50");

        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        assertEquals(new BigDecimal("19.99"), db.gameByName("Grand Theft Auto IV").getPrice());
        assertEquals(new BigDecimal("19.99"), row("Grand Theft Auto IV").getPrice());
    }

    @Test
    void aPriceTheDatabaseRefusesIsAWarningWithoutRetry() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        db.failNextCall(new InvalidDocumentException(DaoKeys.INVALID_DOCUMENT));

        editPrice("Grand Theft Auto IV", "17,50");

        assertEquals(msg(DaoKeys.INVALID_DOCUMENT), dialogMessage());
        assertFalse(visible("dialogRetry"));
        assertEquals(new BigDecimal("19.99"), db.gameByName("Grand Theft Auto IV").getPrice());
    }

    @Test
    void aDatabaseFailureWhileLoadingTheDashboardOffersARetry() throws Exception {
        User developer = backend.services().auth().login(DemoData.DEVELOPER_EMAIL, DemoData.PASSWORD.toCharArray());
        db.failNextCall();

        robot.interact(() -> {
            context.session().start(developer);
            navigator.home();
        });
        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        assertTrue(table().getItems().isEmpty());
        click("dialogRetry");

        assertEquals(3, table().getItems().size());
    }

    @Test
    void aDatabaseFailureWhileCreatingAGameOffersARetry() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        click("newGameButton");
        type("nameField", "Hollow Echo");
        type("priceField", "12,99");
        db.failNextCall();

        click("dialogConfirm");
        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        assertNull(db.gameByName("Hollow Echo"));
        click("dialogRetry");

        assertEquals(View.GAME_EDITOR, navigator.current());
        assertEquals(GameStatus.DRAFT, db.gameByName("Hollow Echo").getStatus());
    }

    /**
     * Edits a game's price in the table: starts editing the cell, types and presses Enter.
     *
     * @param name  the game's name
     * @param price the text to type
     */
    @SuppressWarnings("unchecked")
    private void editPrice(String name, String price) {
        TableView<Game> table = table();
        Game game = row(name);
        TableColumn<Game, BigDecimal> priceColumn = (TableColumn<Game, BigDecimal>) table.getColumns().get(2);
        robot.interact(() -> {
            table.layout();
            table.edit(table.getItems().indexOf(game), priceColumn);
        });
        TextField editor = (TextField) table.lookupAll(".text-field").stream().filter(Node::isVisible)
                .findFirst().orElseThrow();
        robot.interact(() -> {
            editor.setText(price);
            editor.fireEvent(new ActionEvent());
        });
    }

    /**
     * Returns the games table.
     *
     * @return the table
     */
    private TableView<Game> table() {
        return node("gamesTable");
    }

    /**
     * Returns the row of a game.
     *
     * @param name the game's name
     * @return the game as the table shows it
     */
    private Game row(String name) {
        return table().getItems().stream().filter(game -> game.getName().equals(name)).findFirst().orElseThrow();
    }

    /**
     * Selects a game in the table.
     *
     * @param name the game's name
     */
    private void select(String name) {
        Game game = row(name);
        robot.interact(() -> table().getSelectionModel().select(game));
    }
}
