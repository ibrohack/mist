package com.mist.ui.controller;

import com.mist.exception.NotFoundException;
import com.mist.model.Achievement;
import com.mist.model.Developer;
import com.mist.model.Game;
import com.mist.model.GameStatus;
import com.mist.model.Player;
import com.mist.model.Role;
import com.mist.model.SystemRequirements;
import com.mist.model.User;
import com.mist.service.GameReview;
import com.mist.ui.I18n;
import com.mist.ui.NavigationAware;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import com.mist.ui.support.Dialogs;
import com.mist.ui.support.Formats;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Pagination;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller of a game's page: media, description and facts, the buy box, requirements, languages, achievements and
 * reviews.
 *
 * <p>Implements the behaviors GAME-1 to GAME-7 of {@code docs/ui-design.md}; it is opened with the game's
 * {@link ObjectId}.</p>
 */
public class GameController implements NavigationAware {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");
    /** Reviews per page. */
    private static final int REVIEWS_PER_PAGE = 5;
    /** Width of the large image. */
    private static final double MAIN_WIDTH = 640;
    /** Height of the large image. */
    private static final double MAIN_HEIGHT = 360;
    /** Width of a thumbnail. */
    private static final double THUMB_WIDTH = 96;
    /** Height of a thumbnail. */
    private static final double THUMB_HEIGHT = 54;
    /** How many stars a review can have. */
    private static final int STARS = 5;

    /**
     * A game with the name of its studio, loaded together.
     *
     * @param game   the game
     * @param studio the studio's name, or {@code null} if the developer is gone
     */
    private record Page(Game game, String studio) {
    }

    /** What the controller works with. */
    private final UiContext context;
    /** The game on display. */
    private Game game;

    /** Goes back. */
    @FXML
    private Button backButton;
    /** The game's name. */
    @FXML
    private Label nameTitle;
    /** The selected image, large. */
    @FXML
    private ImageView mainImage;
    /** The cover and screenshots, small. */
    @FXML
    private HBox thumbnails;
    /** The store description. */
    @FXML
    private Label descriptionText;
    /** The average rating and how many reviews. */
    @FXML
    private Label ratingText;
    /** The release date. */
    @FXML
    private Label releaseText;
    /** The studio's name. */
    @FXML
    private Label studioText;
    /** The tags, as chips. */
    @FXML
    private FlowPane tagsPane;
    /** "Buy" and the game's name. */
    @FXML
    private Label buyTitle;
    /** The result of the last cart or wishlist action. */
    @FXML
    private Label statusLabel;
    /** The price. */
    @FXML
    private Label priceLabel;
    /** Adds the game to the wishlist. */
    @FXML
    private Button wishlistButton;
    /** Adds the game to the cart. */
    @FXML
    private Button addToCartButton;
    /** Says the player owns the game. */
    @FXML
    private Label ownedLabel;
    /** Says the game is not on sale. */
    @FXML
    private Label notOnSaleLabel;
    /** Operating system. */
    @FXML
    private Label osText;
    /** Processor. */
    @FXML
    private Label processorText;
    /** Memory. */
    @FXML
    private Label memoryText;
    /** Graphics. */
    @FXML
    private Label graphicsText;
    /** Storage. */
    @FXML
    private Label storageText;
    /** The languages, by name. */
    @FXML
    private Label languagesText;
    /** The PEGI rating. */
    @FXML
    private Label pegiBadge;
    /** The achievements. */
    @FXML
    private ListView<Achievement> achievementsList;
    /** The reviews of the current page. */
    @FXML
    private ListView<GameReview> reviewsList;
    /** The pages of reviews. */
    @FXML
    private Pagination reviewsPages;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public GameController(UiContext context) {
        this.context = context;
    }

    /** Sets up the lists and the review pages. */
    @FXML
    private void initialize() {
        achievementsList.setCellFactory(list -> new AchievementCell());
        achievementsList.setPlaceholder(new Label(I18n.text("game.noAchievements")));
        reviewsList.setCellFactory(list -> new ReviewCell());
        reviewsList.setPlaceholder(new Label(I18n.text("game.noReviews")));
        // The pagination only picks the page; the reviews show in the list above it.
        reviewsPages.setPageFactory(index -> new Region());
        reviewsPages.currentPageIndexProperty().addListener((observable, before, after) ->
                loadReviews(after.intValue()));
        statusLabel.setText("");
    }

    @Override
    public void onNavigatedTo(Object parameter) {
        // GAME-1: load the game and its studio.
        ObjectId gameId = (ObjectId) parameter;
        context.tasks().run(null, () -> {
            Game loaded = context.services().store().game(gameId);
            User developer = context.services().users().get(loaded.getDeveloperId());
            return new Page(loaded, developer instanceof Developer studio ? studio.getStudioName() : null);
        }, this::render, error -> {
            context.errors().report(context.navigator().window(), error);
            if (error instanceof NotFoundException) {
                // R2: the game is gone; leave its page.
                onBack();
            }
        });
    }

    /**
     * GAME-1.R1: shows the game.
     *
     * @param page the game and its studio
     */
    private void render(Page page) {
        game = page.game();
        LOG.info("Showing the page of {}", game.getName());
        nameTitle.setText(game.getName());
        descriptionText.setText(game.getDescription() == null ? "" : game.getDescription());
        ratingText.setText(game.getStats().ratingCount() == 0 ? I18n.text("game.noRatings")
                : I18n.format("game.rating", Formats.rating(game.getStats()), game.getStats().ratingCount()));
        releaseText.setText(Formats.date(game.releaseDay()));
        studioText.setText(page.studio() == null ? "—" : page.studio());
        tagsPane.getChildren().setAll(game.getTags().stream().map(GameController::chip).toList());
        buyTitle.setText(I18n.format("game.buy", game.getName()));
        priceLabel.setText(Formats.price(game.getPrice()));
        renderRequirements(game.getRequirements());
        languagesText.setText(languages(game.getLanguages()));
        pegiBadge.setText(game.getPegi() == null ? "" : "PEGI " + game.getPegi().minimumAge());
        pegiBadge.setVisible(game.getPegi() != null);
        achievementsList.getItems().setAll(game.getAchievements());
        renderMedia();
        updateBuyBox();
        int pages = Math.max(1, (game.getStats().ratingCount() + REVIEWS_PER_PAGE - 1) / REVIEWS_PER_PAGE);
        reviewsPages.setPageCount(pages);
        reviewsPages.setCurrentPageIndex(0);
        loadReviews(0);
    }

    /** Builds the thumbnails of the cover and the screenshots, and shows the first one large. */
    private void renderMedia() {
        List<ObjectId> images = new ArrayList<>();
        if (game.getCoverId() != null) {
            images.add(game.getCoverId());
        }
        images.addAll(game.getScreenshotIds());
        thumbnails.getChildren().clear();
        for (int i = 0; i < images.size(); i++) {
            ObjectId id = images.get(i);
            ImageView thumbnail = new ImageView();
            thumbnail.setId("thumbnail" + i);
            thumbnail.setFitWidth(THUMB_WIDTH);
            thumbnail.setFitHeight(THUMB_HEIGHT);
            thumbnail.setPreserveRatio(true);
            thumbnail.getStyleClass().add("mist-thumbnail");
            // GAME-2: a thumbnail shows its image large.
            thumbnail.setOnMouseClicked(event -> showImage(id));
            context.images().load(id, THUMB_WIDTH * 2, THUMB_HEIGHT * 2, thumbnail::setImage);
            thumbnails.getChildren().add(thumbnail);
        }
        mainImage.setImage(null);
        if (!images.isEmpty()) {
            showImage(images.getFirst());
        }
    }

    /**
     * GAME-2: shows an image large.
     *
     * @param imageId the media id
     */
    private void showImage(ObjectId imageId) {
        context.images().load(imageId, MAIN_WIDTH * 2, MAIN_HEIGHT * 2, mainImage::setImage);
    }

    /** Shows the buttons of the buy box that fit the user and the game. */
    private void updateBuyBox() {
        boolean onSale = game.getStatus() == GameStatus.PUBLISHED;
        Player player = context.session().user().orElse(null) instanceof Player p ? p : null;
        boolean guest = context.session().isGuest();
        boolean owned = player != null && player.owns(game.getId());
        boolean inCart = player != null && player.getCartGameIds().contains(game.getId());
        boolean wished = player != null && player.getWishlistGameIds().contains(game.getId());
        show(notOnSaleLabel, !onSale && !owned);
        show(ownedLabel, owned);
        show(addToCartButton, onSale && !owned && (guest || player != null));
        addToCartButton.setDisable(inCart);
        addToCartButton.setText(I18n.text(inCart ? "game.inCart" : "game.addToCart"));
        show(wishlistButton, onSale && !owned && player != null);
        wishlistButton.setDisable(wished);
        wishlistButton.setText(I18n.text(wished ? "game.onWishlist" : "game.addToWishlist"));
    }

    /** GAME-3: puts the game in the cart; a guest is asked to log in first. */
    @FXML
    private void onAddToCart() {
        if (!context.session().hasRole(Role.PLAYER)) {
            Dialogs.confirm(context.navigator().window(), I18n.text("game.loginToBuy"), "menu.login",
                    () -> context.navigator().signOut(null));
            return;
        }
        context.tasks().run(addToCartButton, () -> context.services().store().addToCart(context.session().userId(),
                game.getId()), player -> {
                    // R1: the session keeps the cart, so the header's badge follows.
                    context.session().update(player);
                    statusLabel.setText(I18n.format("store.addedToCart", game.getName()));
                    updateBuyBox();
                });
    }

    /** GAME-5: puts the game on the wishlist. */
    @FXML
    private void onAddToWishlist() {
        context.tasks().run(wishlistButton, () -> context.services().store().addToWishlist(
                context.session().userId(), game.getId()), player -> {
                    context.session().update(player);
                    statusLabel.setText(I18n.format("store.addedToWishlist", game.getName()));
                    updateBuyBox();
                });
    }

    /**
     * GAME-6: shows a page of reviews.
     *
     * @param page the page, from 0
     */
    private void loadReviews(int page) {
        if (game == null) {
            return;
        }
        ObjectId gameId = game.getId();
        context.tasks().run(null, () -> context.services().reviews().reviews(gameId, page, REVIEWS_PER_PAGE),
                found -> reviewsList.getItems().setAll(found));
    }

    /** GAME-7: goes back to the previous screen, or to the store. */
    @FXML
    private void onBack() {
        if (context.navigator().canGoBack()) {
            context.navigator().back();
        } else {
            context.navigator().show(View.STORE);
        }
    }

    /**
     * Shows the system requirements, or dashes when there are none.
     *
     * @param requirements the requirements, or {@code null}
     */
    private void renderRequirements(SystemRequirements requirements) {
        osText.setText(requirements == null ? "—" : requirements.os());
        processorText.setText(requirements == null ? "—" : requirements.processor());
        memoryText.setText(requirements == null ? "—" : requirements.memory());
        graphicsText.setText(requirements == null ? "—" : requirements.graphics());
        storageText.setText(requirements == null ? "—" : requirements.storage());
    }

    /**
     * Names the languages in the UI language.
     *
     * @param codes two-letter language codes
     * @return the names, such as "inglés, español"
     */
    private static String languages(List<String> codes) {
        if (codes.isEmpty()) {
            return "—";
        }
        Locale ui = I18n.locale();
        return String.join(", ", codes.stream().map(code -> Locale.of(code).getDisplayLanguage(ui)).toList());
    }

    /**
     * Builds a tag chip.
     *
     * @param tag the tag
     * @return the chip
     */
    private static Label chip(String tag) {
        Label chip = new Label(tag);
        chip.getStyleClass().add("mist-chip");
        return chip;
    }

    /**
     * Shows or hides a node, taking its space only when shown.
     *
     * @param node    the node
     * @param visible whether to show it
     */
    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /** An achievement: its name over its description. */
    private static final class AchievementCell extends ListCell<Achievement> {

        /** Creates an empty cell; the list fills it. */
        AchievementCell() {
            // The list calls updateItem.
        }

        @Override
        protected void updateItem(Achievement achievement, boolean empty) {
            super.updateItem(achievement, empty);
            if (empty || achievement == null) {
                setGraphic(null);
                return;
            }
            Label name = new Label(achievement.name());
            name.getStyleClass().add("mist-game-name");
            Label description = new Label(achievement.description());
            description.getStyleClass().add("mist-caption");
            description.setWrapText(true);
            setGraphic(new VBox(2, name, description));
        }
    }

    /** A review: stars, author and date over the text. */
    private static final class ReviewCell extends ListCell<GameReview> {

        /** Creates an empty cell; the list fills it. */
        ReviewCell() {
            // The list calls updateItem.
        }

        @Override
        protected void updateItem(GameReview item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            int rating = item.review().getRating();
            Label stars = new Label("★".repeat(rating) + "☆".repeat(STARS - rating));
            stars.getStyleClass().add("mist-stars");
            String author = item.authorNickname() == null ? I18n.text("game.anonymous") : item.authorNickname();
            Label byline = new Label(author + " · " + Formats.date(item.review().getCreatedAt()));
            byline.getStyleClass().add("mist-caption");
            HBox header = new HBox(10, stars, byline);
            header.setAlignment(Pos.CENTER_LEFT);
            VBox box = new VBox(4, header);
            if (item.review().getComment() != null) {
                Label comment = new Label(item.review().getComment());
                comment.setWrapText(true);
                box.getChildren().add(comment);
            }
            setGraphic(box);
        }
    }
}
