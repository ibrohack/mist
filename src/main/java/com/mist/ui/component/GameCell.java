package com.mist.ui.component;

import com.mist.model.Game;
import com.mist.ui.support.Formats;
import com.mist.ui.support.Images;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A row of a game list: the cover, the name with the tags under it, the average rating and the price.
 *
 * <p>The cover loads in the background; a cell reused for another game before its cover arrives does not show the
 * old one.</p>
 */
public class GameCell extends ListCell<Game> {

    /** Width of the cover thumbnail. */
    private static final double COVER_WIDTH = 48;
    /** Height of the cover thumbnail, in the covers' 3:4 ratio. */
    private static final double COVER_HEIGHT = 64;

    /** Loads the covers. */
    private final Images images;
    /** The cover thumbnail. */
    private final ImageView cover = new ImageView();
    /** The game's name. */
    private final Label name = new Label();
    /** The game's tags. */
    private final Label tags = new Label();
    /** The average rating. */
    private final Label rating = new Label();
    /** The price, or "Free". */
    private final Label price = new Label();
    /** The whole row. */
    private final HBox row;

    /**
     * Creates a cell.
     *
     * @param images loads the covers
     */
    public GameCell(Images images) {
        this.images = images;
        cover.setFitWidth(COVER_WIDTH);
        cover.setFitHeight(COVER_HEIGHT);
        cover.setPreserveRatio(true);
        StackPane frame = new StackPane(cover);
        frame.getStyleClass().add("mist-cover-frame");
        frame.setMinSize(COVER_WIDTH, COVER_HEIGHT);
        frame.setPrefSize(COVER_WIDTH, COVER_HEIGHT);
        name.getStyleClass().add("mist-game-name");
        tags.getStyleClass().add("mist-caption");
        rating.getStyleClass().add("mist-caption");
        price.getStyleClass().add("mist-price");
        VBox text = new VBox(4, name, tags);
        text.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row = new HBox(14, frame, text, spacer, rating, price);
        row.setAlignment(Pos.CENTER_LEFT);
        price.setMinWidth(Region.USE_PREF_SIZE);
    }

    @Override
    protected void updateItem(Game game, boolean empty) {
        super.updateItem(game, empty);
        if (empty || game == null) {
            setGraphic(null);
            setText(null);
            return;
        }
        name.setText(game.getName());
        tags.setText(String.join(" · ", game.getTags()));
        rating.setText("★ " + Formats.rating(game.getStats()));
        price.setText(Formats.price(game.getPrice()));
        cover.setImage(null);
        images.load(game.getCoverId(), COVER_WIDTH * 2, COVER_HEIGHT * 2, image -> {
            if (getItem() == game) {
                cover.setImage(image);
            }
        });
        setText(null);
        setGraphic(row);
    }
}
