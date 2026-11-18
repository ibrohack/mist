package com.mist.ui.controller;

import com.mist.exception.ValidationException;
import com.mist.model.Game;
import com.mist.model.Order;
import com.mist.model.PaymentCard;
import com.mist.model.Player;
import com.mist.model.User;
import com.mist.service.ServiceKeys;
import com.mist.ui.I18n;
import com.mist.ui.NavigationAware;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import com.mist.ui.component.CartCell;
import com.mist.ui.support.Dialogs;
import com.mist.ui.support.Formats;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Controller of the cart: the games about to be bought, the summary, the terms and the checkout.
 *
 * <p>Implements the behaviors CART-1 to CART-6 of {@code docs/ui-design.md}.</p>
 */
public class CartController implements NavigationAware {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");

    /**
     * A finished checkout: the order and the player as saved after it.
     *
     * @param order  the order placed
     * @param player the player, with the games in the library and an empty cart
     */
    private record Checkout(Order order, User player) {
    }

    /** What the controller works with. */
    private final UiContext context;

    /** The games in the cart. */
    @FXML
    private ListView<Game> cartList;
    /** Opens the store from the empty cart. */
    @FXML
    private Hyperlink browseLink;
    /** Opens the selected game. */
    @FXML
    private MenuItem openItem;
    /** Moves the selected game to the wishlist. */
    @FXML
    private MenuItem moveItem;
    /** Removes the selected game. */
    @FXML
    private MenuItem removeItem;
    /** The total. */
    @FXML
    private Label totalLabel;
    /** The names of the games being bought. */
    @FXML
    private VBox summaryList;
    /** The card that will be charged. */
    @FXML
    private Label cardLabel;
    /** Accepts the terms; checkout needs it. */
    @FXML
    private CheckBox termsCheck;
    /** Shows the terms. */
    @FXML
    private Hyperlink termsLink;
    /** Pays and places the order. */
    @FXML
    private Button checkoutButton;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public CartController(UiContext context) {
        this.context = context;
    }

    /** Sets up the rows, the opening of games and the checkout's conditions. */
    @FXML
    private void initialize() {
        cartList.setCellFactory(list -> new CartCell(context.images(), this::remove));
        cartList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                onOpen();
            }
        });
        cartList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onOpen();
            } else if (event.getCode() == KeyCode.DELETE) {
                onRemove();
            }
        });
        // CART-5: checkout needs the terms accepted and a game in the cart.
        checkoutButton.disableProperty().bind(termsCheck.selectedProperty().not()
                .or(Bindings.isEmpty(cartList.getItems())));
        updateSummary();
    }

    @Override
    public void onNavigatedTo(Object parameter) {
        load();
    }

    /** CART-1: loads the games in the cart. */
    private void load() {
        context.tasks().run(null, () -> context.services().store().cart(context.session().userId()), games -> {
            cartList.getItems().setAll(games);
            updateSummary();
        });
    }

    /**
     * CART-2: removes a game from the cart.
     *
     * @param game the game
     */
    private void remove(Game game) {
        context.tasks().run(cartList, () -> context.services().store().removeFromCart(context.session().userId(),
                game.getId()), player -> {
                    context.session().update(player);
                    cartList.getItems().remove(game);
                    updateSummary();
                });
    }

    /** CART-2: removes the selected game. */
    @FXML
    private void onRemove() {
        Game game = cartList.getSelectionModel().getSelectedItem();
        if (game != null) {
            remove(game);
        }
    }

    /** CART-3: moves the selected game to the wishlist. */
    @FXML
    private void onMoveToWishlist() {
        Game game = cartList.getSelectionModel().getSelectedItem();
        if (game == null) {
            return;
        }
        context.tasks().run(cartList, () -> {
            context.services().store().addToWishlist(context.session().userId(), game.getId());
            return context.services().store().removeFromCart(context.session().userId(), game.getId());
        }, player -> {
            context.session().update(player);
            cartList.getItems().remove(game);
            updateSummary();
        });
    }

    /** Opens the selected game's page. */
    @FXML
    private void onOpen() {
        Game game = cartList.getSelectionModel().getSelectedItem();
        if (game != null) {
            context.navigator().show(View.GAME, game.getId());
        }
    }

    /** CART-1.R2: the empty cart's link opens the store. */
    @FXML
    private void onBrowse() {
        context.navigator().show(View.STORE);
    }

    /** CART-4: shows the terms of sale. */
    @FXML
    private void onTerms() {
        Dialogs.info(context.navigator().window(), I18n.text("cart.terms.text"));
    }

    /** CART-5: asks to confirm the amount and the card, then checks out. */
    @FXML
    private void onCheckout() {
        String question = I18n.format("cart.confirm", Formats.money(total()), card());
        Dialogs.confirm(context.navigator().window(), question, "cart.pay", this::checkout);
    }

    /** CART-5: places the order and refreshes the player, whose library and cart changed. */
    private void checkout() {
        LOG.info("Checking out {} games", cartList.getItems().size());
        context.tasks().run(checkoutButton, () -> {
            Order order = context.services().store().checkout(context.session().userId());
            return new Checkout(order, context.services().users().get(context.session().userId()));
        }, done -> {
            // R1: the games are in the library and the cart is empty.
            context.session().update(done.player());
            cartList.getItems().clear();
            termsCheck.setSelected(false);
            updateSummary();
            Dialogs.offer(context.navigator().window(),
                    I18n.format("cart.done", done.order().getItems().size(), Formats.money(done.order().getTotal())),
                    "cart.receipt", () -> saveReceipt(done.order(), done.player()));
        }, error -> {
            // R2 and R4 are reported as usual; R3, a game taken off sale meanwhile, also reloads the cart.
            context.errors().report(context.navigator().window(), error, this::checkout);
            if (error instanceof ValidationException refused
                    && ServiceKeys.GAME_NOT_PURCHASABLE.equals(refused.getMessage())) {
                load();
            }
        });
    }

    /**
     * CART-6: writes the receipt of an order as a PDF where the user chooses.
     *
     * @param order the order
     * @param buyer the player who placed it
     */
    private void saveReceipt(Order order, User buyer) {
        Receipts.save(context, order, buyer);
    }

    /** Updates the total, the list of names and the card. */
    private void updateSummary() {
        totalLabel.setText(I18n.format("cart.total", Formats.money(total())));
        summaryList.getChildren().setAll(cartList.getItems().stream().map(game -> new Label("· " + game.getName()))
                .toList());
        cardLabel.setText(I18n.format("cart.card", card()));
    }

    /**
     * Adds up the prices of the games in the cart.
     *
     * @return the total
     */
    private BigDecimal total() {
        return cartList.getItems().stream().map(Game::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Describes the player's saved card.
     *
     * @return brand and last digits, or a dash
     */
    private String card() {
        PaymentCard card = context.session().user().orElse(null) instanceof Player player ? player.getCard() : null;
        return card == null ? "—" : Formats.label(card.brand()) + " " + card.masked();
    }
}
