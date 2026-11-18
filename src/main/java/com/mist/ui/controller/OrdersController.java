package com.mist.ui.controller;

import com.mist.model.Order;
import com.mist.model.OrderItem;
import com.mist.service.OwnedGame;
import com.mist.ui.I18n;
import com.mist.ui.NavigationAware;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import com.mist.ui.component.OrderRow;
import com.mist.ui.support.Dialogs;
import com.mist.ui.support.Formats;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseButton;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controller of the orders: a tree of the player's orders with their games, to return a game within 14 days or
 * download a receipt.
 *
 * <p>Implements the behaviors ORD-1 to ORD-4 of {@code docs/ui-design.md}.</p>
 */
public class OrdersController implements NavigationAware {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");

    /**
     * The player's orders and which owned games can still be returned, loaded together.
     *
     * @param orders     the orders, newest first
     * @param returnable each owned game and whether it can be returned
     */
    private record History(List<Order> orders, Map<ObjectId, Boolean> returnable) {
    }

    /** What the controller works with. */
    private final UiContext context;

    /** Goes back to the library. */
    @FXML
    private Button backButton;
    /** Downloads the receipt of the selected row's order. */
    @FXML
    private Button receiptButton;
    /** Returns the selected game. */
    @FXML
    private Button returnButton;
    /** The orders and their games. */
    @FXML
    private TreeTableView<OrderRow> ordersTable;
    /** When the order was placed. */
    @FXML
    private TreeTableColumn<OrderRow, String> dateColumn;
    /** The number of games of an order, or a game's name. */
    @FXML
    private TreeTableColumn<OrderRow, String> gameColumn;
    /** The order's total, or a game's price. */
    @FXML
    private TreeTableColumn<OrderRow, String> priceColumn;
    /** The order's status, or whether a game can be returned. */
    @FXML
    private TreeTableColumn<OrderRow, String> statusColumn;
    /** When a game was returned. */
    @FXML
    private TreeTableColumn<OrderRow, String> refundColumn;
    /** The actions on the selected row. */
    @FXML
    private ContextMenu rowMenu;
    /** Opens the selected game. */
    @FXML
    private MenuItem openItem;
    /** Returns the selected game. */
    @FXML
    private MenuItem returnItem;
    /** Downloads the receipt of the selected row's order. */
    @FXML
    private MenuItem receiptItem;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public OrdersController(UiContext context) {
        this.context = context;
    }

    /** Sets up the typed columns and enables the actions by the selected row. */
    @FXML
    private void initialize() {
        dateColumn.setCellValueFactory(cell -> text(cell, row -> row.isItem() ? ""
                : Formats.dateTime(row.order().getCreatedAt())));
        gameColumn.setCellValueFactory(cell -> text(cell, row -> row.isItem() ? row.item().name()
                : I18n.format("orders.gameCount", row.order().getItems().size())));
        priceColumn.setCellValueFactory(cell -> text(cell, row -> Formats.money(row.isItem()
                ? row.item().price() : row.order().getTotal())));
        statusColumn.setCellValueFactory(cell -> text(cell, OrdersController::status));
        refundColumn.setCellValueFactory(cell -> text(cell, row -> row.isItem() && row.item().refundedAt() != null
                ? Formats.date(row.item().refundedAt()) : ""));
        ordersTable.setRoot(new TreeItem<>());
        ordersTable.getSelectionModel().selectedItemProperty().addListener((observable, before, after) ->
                updateActions());
        ordersTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                onOpen();
            }
        });
        updateActions();
    }

    @Override
    public void onNavigatedTo(Object parameter) {
        load();
    }

    /** ORD-1: loads the orders and which of their games can still be returned. */
    private void load() {
        context.tasks().run(null, () -> {
            List<Order> orders = context.services().library().orders(context.session().userId());
            Map<ObjectId, Boolean> returnable = context.services().library().ownedGames(context.session().userId())
                    .stream().collect(Collectors.toMap(owned -> owned.game().getId(), OwnedGame::returnable,
                            (a, b) -> a || b));
            return new History(orders, returnable);
        }, this::render);
    }

    /**
     * ORD-1.R1: builds the tree, each order expanded over its games.
     *
     * @param history the orders and the returnable games
     */
    private void render(History history) {
        TreeItem<OrderRow> root = new TreeItem<>();
        for (Order order : history.orders()) {
            TreeItem<OrderRow> orderRow = new TreeItem<>(new OrderRow(order, null, false));
            for (OrderItem item : order.getItems()) {
                boolean returnable = item.refundedAt() == null
                        && history.returnable().getOrDefault(item.gameId(), false);
                orderRow.getChildren().add(new TreeItem<>(new OrderRow(order, item, returnable)));
            }
            orderRow.setExpanded(true);
            root.getChildren().add(orderRow);
        }
        ordersTable.setRoot(root);
        updateActions();
    }

    /** ORD-2: returns the selected game after confirming the refund. */
    @FXML
    private void onReturn() {
        OrderRow row = selected();
        if (row == null || !row.returnable()) {
            return;
        }
        String question = I18n.format("orders.return.confirm", row.item().name(), Formats.money(row.item().price()));
        Dialogs.confirm(context.navigator().window(), question, "orders.return", () -> returnGame(row));
    }

    /**
     * ORD-2: returns a game and refreshes the player and the tree.
     *
     * @param row the game's row
     */
    private void returnGame(OrderRow row) {
        LOG.info("Returning {}", row.item().name());
        ObjectId gameId = row.item().gameId();
        context.tasks().run(returnButton, () -> {
            var amount = context.services().library().returnGame(context.session().userId(), gameId);
            context.session().update(context.services().users().get(context.session().userId()));
            return amount;
        }, amount -> {
            // R1: the refund is done; the row shows its date and the order its new status. R2, a refund window that
            // closed meanwhile, is reported as a warning by the error reporter.
            Dialogs.info(context.navigator().window(), I18n.format("library.returned", Formats.money(amount)));
            load();
        });
    }

    /** ORD-3: downloads the receipt of the selected row's order. */
    @FXML
    private void onReceipt() {
        OrderRow row = selected();
        if (row != null) {
            Receipts.save(context, row.order(), context.session().requireUser());
        }
    }

    /** ORD-4: opens the selected game's page. */
    @FXML
    private void onOpen() {
        OrderRow row = selected();
        if (row != null && row.isItem()) {
            context.navigator().show(View.GAME, row.item().gameId());
        }
    }

    /** Goes back to the library. */
    @FXML
    private void onBack() {
        context.navigator().show(View.LIBRARY);
    }

    /** Enables the actions that fit the selected row. */
    private void updateActions() {
        OrderRow row = selected();
        boolean returnable = row != null && row.returnable();
        returnButton.setDisable(!returnable);
        returnItem.setDisable(!returnable);
        receiptButton.setDisable(row == null);
        receiptItem.setDisable(row == null);
        openItem.setDisable(row == null || !row.isItem());
    }

    /**
     * Returns the selected row.
     *
     * @return the row, or {@code null}
     */
    private OrderRow selected() {
        TreeItem<OrderRow> item = ordersTable.getSelectionModel().getSelectedItem();
        return item == null ? null : item.getValue();
    }

    /**
     * Describes the status of a row: the order's status, or whether its game was returned or can be.
     *
     * @param row the row
     * @return the status text
     */
    private static String status(OrderRow row) {
        if (!row.isItem()) {
            return Formats.label(row.order().getStatus());
        }
        if (row.item().refundedAt() != null) {
            return I18n.text("orders.returned");
        }
        return row.returnable() ? I18n.text("orders.returnable") : "";
    }

    /**
     * Turns a row into the text of a cell.
     *
     * @param cell   the cell's data
     * @param format how the column shows a row
     * @return the cell's value
     */
    private static ReadOnlyStringWrapper text(TreeTableColumn.CellDataFeatures<OrderRow, String> cell,
                                              Function<OrderRow, String> format) {
        OrderRow row = cell.getValue().getValue();
        return new ReadOnlyStringWrapper(row == null ? "" : format.apply(row));
    }
}
