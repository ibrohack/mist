package com.mist.ui;

import com.mist.model.OrderStatus;
import com.mist.model.Player;
import com.mist.service.DemoData;
import com.mist.service.OwnedGame;
import com.mist.service.ServiceKeys;
import com.mist.ui.component.OrderRow;
import com.mist.ui.support.Formats;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import com.mist.dao.DaoKeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The orders and returns (ORD-1 to ORD-4) and the returns from the library (LIB-5, LIB-6).
 */
class OrdersTest extends MistUiTest {

    /** Where the receipts are written. */
    @TempDir
    Path folder;

    @Test
    void theOrdersShowEachOrderWithItsGames() throws Exception {
        openOrders();

        TreeTableView<OrderRow> table = table();
        assertEquals(2, table.getRoot().getChildren().size());
        TreeItem<OrderRow> newest = table.getRoot().getChildren().getFirst();
        assertTrue(newest.isExpanded());
        assertEquals(2, newest.getChildren().size());
        assertTrue(newest.getChildren().stream().allMatch(row -> row.getValue().returnable()));
        TreeItem<OrderRow> oldest = table.getRoot().getChildren().getLast();
        assertTrue(oldest.getChildren().stream().noneMatch(row -> row.getValue().returnable()));
    }

    @Test
    void aRecentGameIsReturnedFromTheOrders() throws Exception {
        openOrders();
        select("Super Mario Odyssey");
        assertFalse(this.<Button>node("returnButton").isDisable());

        click("returnButton");
        assertTrue(dialogMessage().contains(Formats.money(new BigDecimal("49.99"))));
        click("dialogConfirm");

        assertEquals(I18n.format("library.returned", Formats.money(new BigDecimal("49.99"))), dialogMessage());
        OrderRow order = table().getRoot().getChildren().getFirst().getValue();
        assertEquals(OrderStatus.PARTIALLY_REFUNDED, order.order().getStatus());
        Player lucia = (Player) db.userByEmail(DemoData.PLAYER_EMAIL);
        assertFalse(lucia.owns(db.gameByName("Super Mario Odyssey").getId()));
        assertNotNull(row("Super Mario Odyssey").item().refundedAt());
    }

    @Test
    void anOldGameCannotBeReturned() throws Exception {
        openOrders();

        select("Grand Theft Auto V");

        assertTrue(this.<Button>node("returnButton").isDisable());
    }

    @Test
    void theWindowClosingMeanwhileIsReported() throws Exception {
        openOrders();
        select("Metroid Prime");
        clock.advance(Duration.ofDays(15));

        click("returnButton");
        click("dialogConfirm");

        assertEquals(msg(ServiceKeys.LIBRARY_REFUND_CLOSED), dialogMessage());
    }

    @Test
    void anOrdersReceiptIsDownloaded() throws Exception {
        openOrders();
        TreeTableView<OrderRow> table = table();
        robot.interact(() -> table.getSelectionModel().select(table.getRoot().getChildren().getLast()));
        Path target = folder.resolve("old.pdf");
        files.answerSave(target);

        click("receiptButton");

        assertTrue(Files.size(target) > 0);
    }

    @Test
    void aGameIsReturnedFromTheLibrary() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        click("libraryLink");
        ListView<OwnedGame> list = node("gameList");
        OwnedGame neon = list.getItems().stream().filter(g -> g.game().getName().equals("Metroid Prime"))
                .findFirst().orElseThrow();
        robot.interact(() -> list.getSelectionModel().select(neon));
        robot.interact(() -> list.getContextMenu().getOnShowing().handle(null));
        MenuItem returnItem = list.getContextMenu().getItems().stream()
                .filter(i -> "returnItem".equals(i.getId())).findFirst().orElseThrow();
        assertFalse(returnItem.isDisable());

        robot.interact(returnItem::fire);
        click("dialogConfirm");

        assertEquals(3, list.getItems().size());
        assertFalse(((Player) context.session().requireUser()).owns(neon.game().getId()));
    }

    @Test
    void theLibraryDoesNotOfferToReturnAnOldGame() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        click("libraryLink");
        ListView<OwnedGame> list = node("gameList");
        OwnedGame paper = list.getItems().stream().filter(g -> g.game().getName().equals("Super Mario Galaxy"))
                .findFirst().orElseThrow();
        robot.interact(() -> list.getSelectionModel().select(paper));

        robot.interact(() -> list.getContextMenu().getOnShowing().handle(null));

        MenuItem returnItem = list.getContextMenu().getItems().stream()
                .filter(i -> "returnItem".equals(i.getId())).findFirst().orElseThrow();
        assertTrue(returnItem.isDisable());
    }

    @Test
    void theOrdersOpenFromTheLibraryAndLeadBack() throws Exception {
        loginAs(DemoData.SECOND_PLAYER_EMAIL);
        click("libraryLink");
        click("ordersButton");

        assertEquals(View.ORDERS, navigator.current());
        assertEquals(1, table().getRoot().getChildren().size());
        click("backButton");
        assertEquals(View.LIBRARY, navigator.current());
    }

    @Test
    void aDatabaseFailureWhileReturningOffersARetry() throws Exception {
        openOrders();
        select("Super Mario Odyssey");
        click("returnButton");
        db.failNextCall();

        click("dialogConfirm");
        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        click("dialogRetry");

        assertEquals(I18n.format("library.returned", Formats.money(new BigDecimal("49.99"))), dialogMessage());
        assertNotNull(row("Super Mario Odyssey").item().refundedAt());
    }

    /**
     * Logs in as the demo player and opens the orders from the library.
     *
     * @throws Exception if the login fails
     */
    private void openOrders() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        click("libraryLink");
        click("ordersButton");
    }

    /**
     * Returns the orders tree.
     *
     * @return the tree table
     */
    private TreeTableView<OrderRow> table() {
        return node("ordersTable");
    }

    /**
     * Finds the row of a game in the tree.
     *
     * @param name the game's name
     * @return the tree item
     */
    private TreeItem<OrderRow> item(String name) {
        return table().getRoot().getChildren().stream().flatMap(order -> order.getChildren().stream())
                .filter(row -> row.getValue().item().name().equals(name)).findFirst().orElseThrow();
    }

    /**
     * Returns the row of a game in the tree.
     *
     * @param name the game's name
     * @return the row
     */
    private OrderRow row(String name) {
        return item(name).getValue();
    }

    /**
     * Selects the row of a game.
     *
     * @param name the game's name
     */
    private void select(String name) {
        TreeItem<OrderRow> item = item(name);
        robot.interact(() -> table().getSelectionModel().select(item));
    }
}
