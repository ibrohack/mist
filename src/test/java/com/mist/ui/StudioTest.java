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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The developer dashboard (STUDIO-1 to STUDIO-3, STUDIO-5, STUDIO-6), its editable price column and its header link.
 */
class StudioTest extends MistUiTest {

    @Test
    void developersLandOnTheirDashboard() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);

        assertEquals(View.STUDIO, navigator.current());
        assertTrue(visible("studioLink"));
        assertEquals("Nebula Forge", text("studioTitle"));
        assertEquals(7, table().getItems().size());
        assertEquals(Formats.money(new BigDecimal("99.96")), text("revenueLabel"));
        assertEquals("5", text("unitsLabel"));
        assertEquals("6", text("publishedLabel"));
        BarChart<String, Number> chart = node("revenueChart");
        assertEquals(6, chart.getData().getFirst().getData().size());
    }

    @Test
    void aPriceIsEditedInPlaceAndSaved() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);

        editPrice("Tidebreak Harbor", "17,50");

        assertEquals(new BigDecimal("17.50"), db.gameByName("Tidebreak Harbor").getPrice());
        assertEquals(new BigDecimal("17.50"), row("Tidebreak Harbor").getPrice());
    }

    @Test
    void aWrongPriceGoesBackAndWarns() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);

        editPrice("Tidebreak Harbor", "1500");

        assertEquals(msg(ModelKeys.GAME_PRICE), dialogMessage());
        assertEquals(new BigDecimal("24.99"), db.gameByName("Tidebreak Harbor").getPrice());
    }

    @Test
    void aNewGameStartsAsADraft() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);

        click("newGameButton");
        type("nameField", "Hollow Echo");
        type("priceField", "12,99");
        click("dialogConfirm");

        assertEquals(I18n.format("studio.created", "Hollow Echo"), dialogMessage());
        assertEquals(8, table().getItems().size());
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
        assertEquals(7, table().getItems().size());
    }

    @Test
    void aCompleteDraftIsPublished() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        select(DemoData.DRAFT_GAME);
        assertTrue(this.<Button>node("delistButton").isDisable());

        click("publishButton");
        click("dialogConfirm");

        assertEquals(GameStatus.PUBLISHED, db.gameByName(DemoData.DRAFT_GAME).getStatus());
        assertEquals("7", text("publishedLabel"));
    }

    @Test
    void anIncompleteDraftCannotBePublished() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        click("newGameButton");
        type("nameField", "Hollow Echo");
        type("priceField", "5");
        click("dialogConfirm");
        click("dialogOk");
        select("Hollow Echo");

        click("publishButton");
        click("dialogConfirm");

        assertEquals(msg(ModelKeys.GAME_INCOMPLETE), dialogMessage());
        assertEquals(GameStatus.DRAFT, db.gameByName("Hollow Echo").getStatus());
    }

    @Test
    void aPublishedGameIsTakenOffSale() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        select("Ashen Lanterns");
        assertTrue(this.<Button>node("publishButton").isDisable());

        click("delistButton");
        click("dialogConfirm");

        assertEquals(GameStatus.DELISTED, row("Ashen Lanterns").getStatus());
        assertTrue(backend.services().store().newReleases(50).stream()
                .noneMatch(game -> game.getName().equals("Ashen Lanterns")));
    }

    @Test
    void playersHaveNoStudioLink() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        assertFalse(visible("studioLink"));
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
