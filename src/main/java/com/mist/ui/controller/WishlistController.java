package com.mist.ui.controller;

import com.mist.model.Game;
import com.mist.ui.I18n;
import com.mist.ui.NavigationAware;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import com.mist.ui.component.GameCell;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

/**
 * Controller of the wishlist: the games the player wants, to move to the cart or remove.
 *
 * <p>Implements the behaviors WISH-1 to WISH-4 of {@code docs/ui-design.md}.</p>
 */
public class WishlistController implements NavigationAware {

    /** What the controller works with. */
    private final UiContext context;

    /** Tells the result of the last action. */
    @FXML
    private Label statusLabel;
    /** The wished games. */
    @FXML
    private ListView<Game> wishlistList;
    /** Opens the selected game. */
    @FXML
    private MenuItem openItem;
    /** Moves the selected game to the cart. */
    @FXML
    private MenuItem cartItem;
    /** Removes the selected game. */
    @FXML
    private MenuItem removeItem;
    /** Moves the selected game to the cart. */
    @FXML
    private Button addToCartButton;
    /** Removes the selected game. */
    @FXML
    private Button removeButton;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public WishlistController(UiContext context) {
        this.context = context;
    }

    /** Sets up the rows, the opening of games and the buttons that need a selection. */
    @FXML
    private void initialize() {
        wishlistList.setCellFactory(list -> new GameCell(context.images()));
        wishlistList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                onOpen();
            }
        });
        wishlistList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onOpen();
            }
        });
        addToCartButton.disableProperty().bind(wishlistList.getSelectionModel().selectedItemProperty().isNull());
        removeButton.disableProperty().bind(wishlistList.getSelectionModel().selectedItemProperty().isNull());
        statusLabel.setText("");
    }

    @Override
    public void onNavigatedTo(Object parameter) {
        // WISH-1: load the wished games.
        context.tasks().run(null, () -> context.services().store().wishlist(context.session().userId()),
                games -> wishlistList.getItems().setAll(games));
    }

    /** WISH-2: moves the selected game to the cart. */
    @FXML
    private void onAddToCart() {
        Game game = wishlistList.getSelectionModel().getSelectedItem();
        if (game == null) {
            return;
        }
        context.tasks().run(wishlistList, () -> {
            context.services().store().addToCart(context.session().userId(), game.getId());
            return context.services().store().removeFromWishlist(context.session().userId(), game.getId());
        }, player -> {
            // R1: the session keeps the cart, so the header's badge follows.
            context.session().update(player);
            wishlistList.getItems().remove(game);
            statusLabel.setText(I18n.format("store.addedToCart", game.getName()));
        });
    }

    /** WISH-3: removes the selected game from the wishlist. */
    @FXML
    private void onRemove() {
        Game game = wishlistList.getSelectionModel().getSelectedItem();
        if (game == null) {
            return;
        }
        context.tasks().run(wishlistList, () -> context.services().store().removeFromWishlist(
                context.session().userId(), game.getId()), player -> {
                    context.session().update(player);
                    wishlistList.getItems().remove(game);
                    statusLabel.setText(I18n.format("wishlist.removed", game.getName()));
                });
    }

    /** WISH-4: opens the selected game's page. */
    @FXML
    private void onOpen() {
        Game game = wishlistList.getSelectionModel().getSelectedItem();
        if (game != null) {
            context.navigator().show(View.GAME, game.getId());
        }
    }
}
