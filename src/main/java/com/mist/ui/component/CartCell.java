package com.mist.ui.component;

import com.mist.model.Game;
import com.mist.ui.I18n;
import com.mist.ui.support.Formats;
import com.mist.ui.support.Images;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.function.Consumer;

/**
 * A row of the cart: the cover, the name, the price and a Remove button.
 */
public class CartCell extends ListCell<Game> {

    /** Width of the cover thumbnail. */
    private static final double COVER_WIDTH = 48;
    /** Height of the cover thumbnail. */
    private static final double COVER_HEIGHT = 64;

    /** Loads the covers. */
    private final Images images;
    /** The cover thumbnail. */
    private final ImageView cover = new ImageView();
    /** The game's name. */
    private final Label name = new Label();
    /** The price. */
    private final Label price = new Label();
    /** Removes the game from the cart. */
    private final Button removeButton = new Button(I18n.text("cart.remove"));
    /** The whole row. */
    private final HBox row;

    /**
     * Creates a cell.
     *
     * @param images loads the covers
     * @param remove what Remove does with the row's game
     */
    public CartCell(Images images, Consumer<Game> remove) {
        this.images = images;
        cover.setFitWidth(COVER_WIDTH);
        cover.setFitHeight(COVER_HEIGHT);
        cover.setPreserveRatio(true);
        StackPane frame = new StackPane(cover);
        frame.getStyleClass().add("mist-cover-frame");
        frame.setMinSize(COVER_WIDTH, COVER_HEIGHT);
        frame.setPrefSize(COVER_WIDTH, COVER_HEIGHT);
        name.getStyleClass().add("mist-game-name");
        price.getStyleClass().add("mist-price");
        removeButton.getStyleClass().add("mist-button-secondary");
        removeButton.setId("removeButton");
        // CART-2: the row's Remove button.
        removeButton.setOnAction(event -> {
            if (getItem() != null) {
                remove.accept(getItem());
            }
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row = new HBox(16, frame, name, spacer, price, removeButton);
        row.setAlignment(Pos.CENTER_LEFT);
    }

    @Override
    protected void updateItem(Game game, boolean empty) {
        super.updateItem(game, empty);
        if (empty || game == null) {
            setGraphic(null);
            return;
        }
        name.setText(game.getName());
        price.setText(Formats.price(game.getPrice()));
        cover.setImage(null);
        images.load(game.getCoverId(), COVER_WIDTH * 2, COVER_HEIGHT * 2, image -> {
            if (getItem() == game) {
                cover.setImage(image);
            }
        });
        setGraphic(row);
    }
}
