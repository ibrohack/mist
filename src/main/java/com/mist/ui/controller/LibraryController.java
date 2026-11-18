package com.mist.ui.controller;

import com.mist.service.LibraryService;
import com.mist.service.OwnedGame;
import com.mist.ui.I18n;
import com.mist.ui.NavigationAware;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import com.mist.ui.support.Dialogs;
import com.mist.ui.support.Formats;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Controller of the library: the games the player owns, as a filtered list of names and a grid of covers.
 *
 * <p>Implements the behaviors LIB-1 to LIB-6 of {@code docs/ui-design.md}.</p>
 */
public class LibraryController implements NavigationAware {

    /** The sort by acquisition date, newest first. */
    private static final String SORT_RECENT = "library.sort.recent";
    /** The sort by name. */
    private static final String SORT_NAME = "library.sort.name";
    /** The tag filter's choice that keeps every game. */
    private static final String ALL_TAGS = "";
    /** Width of a card's cover. */
    private static final double COVER_WIDTH = 150;
    /** Height of a card's cover. */
    private static final double COVER_HEIGHT = 200;

    /** What the controller works with. */
    private final UiContext context;
    /** Every owned game. */
    private final ObservableList<OwnedGame> owned = FXCollections.observableArrayList();
    /** The owned games the search and the tag keep. */
    private final FilteredList<OwnedGame> filtered = new FilteredList<>(owned);
    /** The kept games in the chosen order. */
    private final SortedList<OwnedGame> sorted = new SortedList<>(filtered);

    /** The sidebar and the grid. */
    @FXML
    private SplitPane librarySplit;
    /** Filters the names. */
    @FXML
    private TextField searchField;
    /** The order of the games. */
    @FXML
    private ComboBox<String> sortBox;
    /** Keeps the games with one tag. */
    @FXML
    private ComboBox<String> tagFilter;
    /** The names of the kept games. */
    @FXML
    private ListView<OwnedGame> gameList;
    /** The actions on the selected game. */
    @FXML
    private ContextMenu gameMenu;
    /** Opens the selected game. */
    @FXML
    private MenuItem openItem;
    /** Opens the review dialog for the selected game. */
    @FXML
    private MenuItem reviewItem;
    /** Returns the selected game, while it can be returned. */
    @FXML
    private MenuItem returnItem;
    /** Opens the orders. */
    @FXML
    private Button ordersButton;
    /** The covers of the kept games. */
    @FXML
    private TilePane gameGrid;
    /** Shown instead of the library when it is empty. */
    @FXML
    private VBox emptyState;
    /** Opens the store from the empty state. */
    @FXML
    private Button browseButton;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public LibraryController(UiContext context) {
        this.context = context;
    }

    /** Sets up the filters, the list and the grid, which follow the same kept games. */
    @FXML
    private void initialize() {
        sortBox.getItems().setAll(SORT_RECENT, SORT_NAME);
        sortBox.setConverter(keyConverter());
        sortBox.setValue(SORT_RECENT);
        tagFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(String tag) {
                return tag == null || tag.isEmpty() ? I18n.text("library.allTags") : tag;
            }

            @Override
            public String fromString(String text) {
                return text;
            }
        });
        tagFilter.getItems().setAll(ALL_TAGS);
        tagFilter.setValue(ALL_TAGS);
        gameList.setItems(sorted);
        gameList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(OwnedGame item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.game().getName());
            }
        });
        // LIB-2: every filter applies at once.
        searchField.textProperty().addListener((observable, before, after) -> applyFilters());
        tagFilter.valueProperty().addListener((observable, before, after) -> applyFilters());
        sortBox.valueProperty().addListener((observable, before, after) -> applyFilters());
        sorted.addListener((ListChangeListener<OwnedGame>) change -> renderGrid());
        // LIB-5: only a game still in its refund window can be returned.
        gameMenu.setOnShowing(event -> {
            OwnedGame item = selected();
            openItem.setDisable(item == null);
            reviewItem.setDisable(item == null);
            returnItem.setDisable(item == null || !item.returnable());
        });
        gameList.getSelectionModel().selectedItemProperty().addListener((observable, before, after) ->
                highlight(after));
        // LIB-3: a double click or Enter opens the selected game.
        gameList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                onOpen();
            }
        });
        gameList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onOpen();
            }
        });
        showEmpty(false);
    }

    @Override
    public void onNavigatedTo(Object parameter) {
        load();
    }

    /** LIB-1: loads the owned games, newest first. */
    void load() {
        context.tasks().run(null, () -> context.services().library().ownedGames(context.session().userId()),
                games -> {
                    owned.setAll(games);
                    String tag = tagFilter.getValue();
                    List<String> tags = games.stream().flatMap(game -> game.game().getTags().stream()).distinct()
                            .sorted().toList();
                    tagFilter.getItems().setAll(ALL_TAGS);
                    tagFilter.getItems().addAll(tags);
                    tagFilter.setValue(tags.contains(tag) ? tag : ALL_TAGS);
                    // R2: an empty library shows the brand's empty state.
                    showEmpty(games.isEmpty());
                    applyFilters();
                });
    }

    /** LIB-2: keeps the games that match the search and the tag, in the chosen order. */
    private void applyFilters() {
        String text = searchField.getText() == null ? "" : searchField.getText().strip().toLowerCase(Locale.ROOT);
        String tag = tagFilter.getValue();
        filtered.setPredicate(item -> item.game().getName().toLowerCase(Locale.ROOT).contains(text)
                && (tag == null || tag.isEmpty() || item.game().getTags().contains(tag)));
        sorted.setComparator(SORT_NAME.equals(sortBox.getValue())
                ? Comparator.comparing(item -> item.game().getName(), String.CASE_INSENSITIVE_ORDER)
                : Comparator.comparing(OwnedGame::acquiredAt).reversed());
    }

    /** LIB-3: opens the selected game's page. */
    @FXML
    private void onOpen() {
        OwnedGame item = gameList.getSelectionModel().getSelectedItem();
        if (item != null) {
            context.navigator().show(View.GAME, item.game().getId());
        }
    }

    /** LIB-4: opens the review dialog for the selected game. */
    @FXML
    private void onReview() {
        OwnedGame item = selected();
        if (item != null) {
            ReviewDialog.show(context, item.game(),
                    () -> Dialogs.info(context.navigator().window(), I18n.text("review.published")));
        }
    }

    /** LIB-5: returns the selected game after confirming the refund. */
    @FXML
    private void onReturn() {
        OwnedGame item = selected();
        if (item == null || !item.returnable()) {
            return;
        }
        Dialogs.confirm(context.navigator().window(), I18n.format("library.return.confirm", item.game().getName()),
                "library.return", () -> context.tasks().run(gameList, () -> {
                    var amount = context.services().library().returnGame(context.session().userId(),
                            item.game().getId());
                    context.session().update(context.services().users().get(context.session().userId()));
                    return amount;
                }, amount -> {
                    // R1: the refund is done and the game leaves the library.
                    Dialogs.info(context.navigator().window(), I18n.format("library.returned", Formats.money(amount)));
                    load();
                }));
    }

    /** LIB-6: opens the orders. */
    @FXML
    private void onOrders() {
        context.navigator().show(View.ORDERS);
    }

    /** The empty state's button opens the store. */
    @FXML
    private void onBrowse() {
        context.navigator().show(View.STORE);
    }

    /**
     * Returns the selected owned game.
     *
     * @return the game, or {@code null}
     */
    OwnedGame selected() {
        return gameList.getSelectionModel().getSelectedItem();
    }

    /** Rebuilds the grid of covers from the kept games. */
    private void renderGrid() {
        gameGrid.getChildren().setAll(sorted.stream().map(this::card).toList());
        highlight(selected());
    }

    /**
     * Builds the card of an owned game: its cover, its name and, while it can be returned, until when.
     *
     * @param item the owned game
     * @return the card
     */
    private VBox card(OwnedGame item) {
        ImageView cover = new ImageView();
        cover.setFitWidth(COVER_WIDTH);
        cover.setFitHeight(COVER_HEIGHT);
        cover.setPreserveRatio(true);
        StackPane frame = new StackPane(cover);
        frame.getStyleClass().add("mist-cover-frame");
        frame.setPrefSize(COVER_WIDTH, COVER_HEIGHT);
        context.images().load(item.game().getCoverId(), COVER_WIDTH * 2, COVER_HEIGHT * 2, cover::setImage);
        Label name = new Label(item.game().getName());
        name.getStyleClass().add("mist-game-name");
        name.setMaxWidth(COVER_WIDTH);
        Label caption = new Label(item.returnable() ? I18n.format("library.returnableUntil",
                Formats.date(item.acquiredAt().plus(LibraryService.REFUND_WINDOW))) : "");
        caption.getStyleClass().add("mist-caption");
        VBox card = new VBox(frame, name, caption);
        card.setAlignment(Pos.TOP_LEFT);
        card.getStyleClass().add("game-card");
        card.setUserData(item);
        card.setOnMouseClicked(event -> {
            gameList.getSelectionModel().select(item);
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                onOpen();
            }
        });
        // The cards offer the same actions as the list.
        card.setOnContextMenuRequested(event -> {
            gameList.getSelectionModel().select(item);
            gameMenu.show(card, event.getScreenX(), event.getScreenY());
        });
        return card;
    }

    /**
     * Marks the card of the selected game.
     *
     * @param item the selected game, or {@code null}
     */
    private void highlight(OwnedGame item) {
        gameGrid.getChildren().forEach(card -> {
            card.getStyleClass().remove("selected");
            if (item != null && Objects.equals(((OwnedGame) card.getUserData()).game().getId(), item.game().getId())) {
                card.getStyleClass().add("selected");
            }
        });
    }

    /**
     * Shows the empty state or the library.
     *
     * @param empty whether the player owns no game
     */
    private void showEmpty(boolean empty) {
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
        librarySplit.setVisible(!empty);
        librarySplit.setManaged(!empty);
    }

    /**
     * Shows bundle keys by their text.
     *
     * @return the converter
     */
    private static StringConverter<String> keyConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(String key) {
                return key == null ? "" : I18n.text(key);
            }

            @Override
            public String fromString(String text) {
                return text;
            }
        };
    }
}
