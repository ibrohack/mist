package com.mist.ui.controller;

import com.mist.model.Game;
import com.mist.model.Player;
import com.mist.model.Role;
import com.mist.ui.I18n;
import com.mist.ui.NavigationAware;
import com.mist.ui.UiContext;
import com.mist.ui.component.GameCell;
import com.mist.ui.support.Formats;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Controller of the store: the featured carousel, the New releases, Top sellers and Free tabs, and the search.
 *
 * <p>Implements the behaviors STORE-1, STORE-2, STORE-4 and STORE-5 of {@code docs/ui-design.md}.</p>
 */
public class StoreController implements NavigationAware {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");
    /** How many games each list shows. */
    private static final int LIST_SIZE = 20;
    /** How many games the carousel shows. */
    private static final int FEATURED_SIZE = 5;
    /** How long typing must pause before the search runs. */
    private static final Duration SEARCH_PAUSE = Duration.millis(300);
    /** Width of a banner's cover. */
    private static final double BANNER_COVER_WIDTH = 180;
    /** Height of a banner's cover. */
    private static final double BANNER_COVER_HEIGHT = 240;

    /**
     * The three lists of the tabs, loaded together.
     *
     * @param latest the newest releases
     * @param top    the best sellers
     * @param free   the free games
     */
    private record Shelves(List<Game> latest, List<Game> top, List<Game> free) {
    }

    /** What the controller works with. */
    private final UiContext context;
    /** Runs the search once typing pauses. */
    private final PauseTransition searchDelay = new PauseTransition(SEARCH_PAUSE);
    /** The games of the carousel. */
    private List<Game> featuredGames = List.of();

    /** Tells the result of the last cart or wishlist action. */
    @FXML
    private Label statusLabel;
    /** The search text. */
    @FXML
    private TextField searchField;
    /** The carousel and the tabs, shown while not searching. */
    @FXML
    private VBox browseBox;
    /** The carousel of featured games. */
    @FXML
    private Pagination featured;
    /** The New releases, Top sellers and Free tabs. */
    @FXML
    private TabPane tabs;
    /** The newest releases. */
    @FXML
    private ListView<Game> newList;
    /** The best sellers. */
    @FXML
    private ListView<Game> topList;
    /** The free games. */
    @FXML
    private ListView<Game> freeList;
    /** The search results, shown while searching. */
    @FXML
    private VBox resultsBox;
    /** Names the search. */
    @FXML
    private Label resultsTitle;
    /** The games found. */
    @FXML
    private ListView<Game> resultsList;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public StoreController(UiContext context) {
        this.context = context;
    }

    /** Sets up the lists, their context menus, the carousel and the search. */
    @FXML
    private void initialize() {
        for (ListView<Game> list : lists()) {
            list.setCellFactory(view -> new GameCell(context.images()));
            list.setPlaceholder(new Label(I18n.text("store.empty")));
            list.setContextMenu(menuFor(list));
        }
        resultsList.setPlaceholder(new Label(I18n.text("store.noResults")));
        featured.setPageFactory(this::banner);
        featured.setPageCount(1);
        searchDelay.setOnFinished(event -> search());
        searchField.textProperty().addListener((observable, before, after) -> searchDelay.playFromStart());
        // STORE-2: Enter searches at once.
        searchField.setOnAction(event -> {
            searchDelay.stop();
            search();
        });
        showResults(false);
    }

    @Override
    public void onNavigatedTo(Object parameter) {
        // STORE-1: fill the carousel and the tabs.
        context.tasks().run(null, () -> new Shelves(context.services().store().newReleases(LIST_SIZE),
                context.services().store().topSellers(LIST_SIZE), context.services().store().freeGames(LIST_SIZE)),
                shelves -> {
                    newList.getItems().setAll(shelves.latest());
                    topList.getItems().setAll(shelves.top());
                    freeList.getItems().setAll(shelves.free());
                    featuredGames = shelves.latest().stream().filter(game -> game.getCoverId() != null)
                            .limit(FEATURED_SIZE).toList();
                    featured.setPageCount(Math.max(1, featuredGames.size()));
                    featured.setCurrentPageIndex(0);
                    featured.setPageFactory(this::banner);
                });
    }

    /** STORE-2: searches the catalog, or goes back to browsing when the text is blank. */
    private void search() {
        String text = searchField.getText() == null ? "" : searchField.getText().strip();
        if (text.isEmpty()) {
            // R2: blank text brings the carousel and the tabs back.
            showResults(false);
            return;
        }
        LOG.info("Searching the store for \"{}\"", text);
        context.tasks().run(null, () -> context.services().store().search(text, LIST_SIZE), found -> {
            // R1: the matches replace the carousel and the tabs.
            resultsTitle.setText(I18n.format("store.results", text));
            resultsList.getItems().setAll(found);
            showResults(true);
        });
    }

    /**
     * STORE-4: puts the selected game of a list in the player's cart.
     *
     * @param list the list the context menu belongs to
     */
    private void addToCart(ListView<Game> list) {
        Game game = list.getSelectionModel().getSelectedItem();
        if (game == null) {
            return;
        }
        context.tasks().run(list, () -> context.services().store().addToCart(context.session().userId(),
                game.getId()), player -> {
                    // R1: the session keeps the cart, so the header's badge follows.
                    context.session().update(player);
                    statusLabel.setText(I18n.format("store.addedToCart", game.getName()));
                });
    }

    /**
     * STORE-5: puts the selected game of a list on the player's wishlist.
     *
     * @param list the list the context menu belongs to
     */
    private void addToWishlist(ListView<Game> list) {
        Game game = list.getSelectionModel().getSelectedItem();
        if (game == null) {
            return;
        }
        context.tasks().run(list, () -> context.services().store().addToWishlist(context.session().userId(),
                game.getId()), player -> {
                    context.session().update(player);
                    statusLabel.setText(I18n.format("store.addedToWishlist", game.getName()));
                });
    }

    /**
     * Builds the context menu of a list: Add to cart and Add to wishlist for players, nothing for anyone else.
     *
     * @param list the list
     * @return the menu, or {@code null} when the user cannot buy
     */
    private ContextMenu menuFor(ListView<Game> list) {
        if (!context.session().hasRole(Role.PLAYER)) {
            return null;
        }
        MenuItem cartItem = new MenuItem(I18n.text("store.addToCart"));
        cartItem.setId("cartItem");
        cartItem.setOnAction(event -> addToCart(list));
        MenuItem wishlistItem = new MenuItem(I18n.text("store.addToWishlist"));
        wishlistItem.setId("wishlistItem");
        wishlistItem.setOnAction(event -> addToWishlist(list));
        ContextMenu menu = new ContextMenu(cartItem, wishlistItem);
        menu.setOnShowing(event -> {
            Game game = list.getSelectionModel().getSelectedItem();
            boolean owned = game == null || context.session().user()
                    .map(user -> user instanceof Player player && player.owns(game.getId())).orElse(true);
            cartItem.setDisable(owned);
            wishlistItem.setDisable(owned);
        });
        return menu;
    }

    /**
     * Builds the banner of one featured game: the cover, the name, the tags, the price and the description.
     *
     * @param index the page of the carousel
     * @return the banner, or an empty pane before the games arrive
     */
    private Node banner(int index) {
        if (index >= featuredGames.size()) {
            return new StackPane();
        }
        Game game = featuredGames.get(index);
        ImageView cover = new ImageView();
        cover.setFitWidth(BANNER_COVER_WIDTH);
        cover.setFitHeight(BANNER_COVER_HEIGHT);
        cover.setPreserveRatio(true);
        context.images().load(game.getCoverId(), BANNER_COVER_WIDTH * 2, BANNER_COVER_HEIGHT * 2, cover::setImage);
        Label name = new Label(game.getName());
        name.getStyleClass().add("mist-title");
        Label tags = new Label(String.join(" · ", game.getTags()));
        tags.getStyleClass().add("mist-caption");
        Label price = new Label(Formats.price(game.getPrice()));
        price.getStyleClass().add("mist-price");
        Label description = new Label(game.getDescription());
        description.setWrapText(true);
        description.setMaxHeight(80);
        VBox text = new VBox(10, name, tags, price, description);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox banner = new HBox(24, cover, text);
        banner.setId("banner" + index);
        banner.getStyleClass().add("mist-banner");
        banner.setAlignment(Pos.CENTER_LEFT);
        return banner;
    }

    /**
     * Shows the search results or the browsing lists.
     *
     * @param searching whether a search is active
     */
    private void showResults(boolean searching) {
        resultsBox.setVisible(searching);
        resultsBox.setManaged(searching);
        browseBox.setVisible(!searching);
        browseBox.setManaged(!searching);
    }

    /**
     * Returns the four game lists of the screen.
     *
     * @return the tabs' lists and the results list
     */
    private List<ListView<Game>> lists() {
        return List.of(newList, topList, freeList, resultsList);
    }
}
