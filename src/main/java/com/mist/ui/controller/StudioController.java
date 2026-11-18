package com.mist.ui.controller;

import com.mist.model.Developer;
import com.mist.model.Game;
import com.mist.model.GameStatus;
import com.mist.model.ModelKeys;
import com.mist.service.SalesReport;
import com.mist.ui.I18n;
import com.mist.ui.NavigationAware;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import com.mist.ui.support.Dialogs;
import com.mist.ui.support.Formats;
import com.mist.ui.support.GameForms;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.function.Function;

/**
 * Controller of the developer dashboard: sales cards, the revenue chart, and the editable table of the developer's
 * games.
 *
 * <p>Implements the behaviors STUDIO-1 to STUDIO-6 of {@code docs/ui-design.md}. The price
 * column is edited in place: committing a cell saves the new price with {@code PublishingService#update}, and a
 * rejected price goes back to the stored one.</p>
 */
public class StudioController implements NavigationAware {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");

    /** What the controller works with. */
    private final UiContext context;
    /** The developer's games. */
    private final ObservableList<Game> games = FXCollections.observableArrayList();

    /** The studio's name. */
    @FXML
    private Label studioTitle;
    /** Creates a draft. */
    @FXML
    private Button newGameButton;
    /** The developer's total revenue. */
    @FXML
    private Label revenueLabel;
    /** Units sold over all games. */
    @FXML
    private Label unitsLabel;
    /** Games on sale. */
    @FXML
    private Label publishedLabel;
    /** Revenue per game. */
    @FXML
    private BarChart<String, Number> revenueChart;
    /** Opens the selected game in the editor. */
    @FXML
    private Button editButton;
    /** Publishes the selected draft. */
    @FXML
    private Button publishButton;
    /** Takes the selected game off sale. */
    @FXML
    private Button delistButton;
    /** The developer's games. */
    @FXML
    private TableView<Game> gamesTable;
    /** The name. */
    @FXML
    private TableColumn<Game, String> nameColumn;
    /** Draft, published or delisted. */
    @FXML
    private TableColumn<Game, GameStatus> statusColumn;
    /** The price, editable. */
    @FXML
    private TableColumn<Game, BigDecimal> priceColumn;
    /** The release date. */
    @FXML
    private TableColumn<Game, LocalDate> releaseColumn;
    /** Units sold. */
    @FXML
    private TableColumn<Game, Integer> unitsColumn;
    /** Revenue after refunds. */
    @FXML
    private TableColumn<Game, BigDecimal> revenueColumn;
    /** The average rating. */
    @FXML
    private TableColumn<Game, String> ratingColumn;
    /** The actions on the selected game. */
    @FXML
    private ContextMenu gameMenu;
    /** Opens the selected game in the editor. */
    @FXML
    private MenuItem editItem;
    /** Publishes the selected draft. */
    @FXML
    private MenuItem publishItem;
    /** Takes the selected game off sale. */
    @FXML
    private MenuItem delistItem;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public StudioController(UiContext context) {
        this.context = context;
    }

    /** Sets up the typed columns, the editable price column and the actions by selection. */
    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(cell -> value(cell, Game::getName));
        statusColumn.setCellValueFactory(cell -> value(cell, Game::getStatus));
        statusColumn.setCellFactory(column -> new StatusCell());
        priceColumn.setCellValueFactory(cell -> value(cell, Game::getPrice));
        // STUDIO-2: the price is edited in place, typed in either language's format.
        priceColumn.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<>() {
            @Override
            public String toString(BigDecimal price) {
                return price == null ? "" : Formats.money(price);
            }

            @Override
            public BigDecimal fromString(String text) {
                return GameForms.parsePrice(text);
            }
        }));
        priceColumn.setOnEditCommit(this::onPriceCommit);
        releaseColumn.setCellValueFactory(cell -> value(cell, Game::releaseDay));
        releaseColumn.setCellFactory(column -> textCell(Formats::date));
        unitsColumn.setCellValueFactory(cell -> value(cell, game -> game.getStats().unitsSold()));
        revenueColumn.setCellValueFactory(cell -> value(cell, game -> game.getStats().revenue()));
        revenueColumn.setCellFactory(column -> textCell(Formats::money));
        ratingColumn.setCellValueFactory(cell -> value(cell, game -> Formats.rating(game.getStats())));
        gamesTable.setItems(games);
        // STUDIO-4: a double click on a row's name opens the editor (the price cell edits in place).
        gamesTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2
                    && gamesTable.getEditingCell() == null) {
                onEdit();
            }
        });
        gamesTable.getSelectionModel().selectedItemProperty().addListener((observable, before, after) ->
                updateActions());
        updateActions();
    }

    @Override
    public void onNavigatedTo(Object parameter) {
        if (context.session().requireUser() instanceof Developer developer) {
            studioTitle.setText(developer.getStudioName());
        }
        load();
    }

    /** STUDIO-1: loads the sales and the games. */
    private void load() {
        context.tasks().run(null, () -> context.services().publishing().sales(context.session().userId()),
                this::render);
    }

    /**
     * STUDIO-1.R1: shows the cards, the chart and the table.
     *
     * @param report the developer's sales
     */
    private void render(SalesReport report) {
        Game selected = gamesTable.getSelectionModel().getSelectedItem();
        games.setAll(report.games());
        revenueLabel.setText(Formats.money(report.totalRevenue()));
        unitsLabel.setText(Formats.count(report.unitsSold()));
        publishedLabel.setText(Formats.count(report.games().stream()
                .filter(game -> game.getStatus() == GameStatus.PUBLISHED).count()));
        XYChart.Series<String, Number> revenue = new XYChart.Series<>();
        for (Game game : report.games()) {
            if (game.getStatus() != GameStatus.DRAFT) {
                revenue.getData().add(new XYChart.Data<>(game.getName(), game.getStats().revenue()));
            }
        }
        revenueChart.getData().clear();
        revenueChart.getData().add(revenue);
        if (selected != null) {
            games.stream().filter(game -> game.getId().equals(selected.getId())).findFirst()
                    .ifPresent(game -> gamesTable.getSelectionModel().select(game));
        }
        updateActions();
    }

    /**
     * STUDIO-2: saves the price typed in a cell; a rejected price goes back to the stored one.
     *
     * @param event the cell's commit
     */
    private void onPriceCommit(TableColumn.CellEditEvent<Game, BigDecimal> event) {
        Game game = event.getRowValue();
        BigDecimal price = event.getNewValue();
        if (price == null) {
            // R2: not a price; show the stored one again.
            gamesTable.refresh();
            Dialogs.warning(context.navigator().window(), I18n.text(ModelKeys.GAME_PRICE));
            return;
        }
        LOG.info("Changing the price of {} to {}", game.getName(), price);
        context.tasks().run(gamesTable, () -> context.services().publishing().update(context.session().userId(),
                game.getId(), GameForms.withPrice(game, price)), saved -> {
                    // R1: the row shows the saved game.
                    games.set(games.indexOf(game), saved);
                    gamesTable.getSelectionModel().select(saved);
                }, error -> {
                    gamesTable.refresh();
                    context.errors().report(context.navigator().window(), error);
                });
    }

    /** STUDIO-3: opens the New game dialog; the new draft opens in the editor. */
    @FXML
    private void onNewGame() {
        NewGameDialog.show(context, draft -> context.navigator().show(View.GAME_EDITOR, draft.getId()));
    }

    /** STUDIO-4: opens the selected game in the editor. */
    @FXML
    private void onEdit() {
        Game game = gamesTable.getSelectionModel().getSelectedItem();
        if (game != null) {
            context.navigator().show(View.GAME_EDITOR, game.getId());
        }
    }

    /** STUDIO-5: publishes the selected draft after confirming. */
    @FXML
    private void onPublish() {
        Game game = gamesTable.getSelectionModel().getSelectedItem();
        if (game == null || game.getStatus() != GameStatus.DRAFT) {
            return;
        }
        Dialogs.confirm(context.navigator().window(), I18n.format("studio.publish.confirm", game.getName()),
                "studio.publish", () -> context.tasks().run(publishButton, () -> context.services().publishing()
                        .publish(context.session().userId(), game.getId()), published -> load()));
    }

    /** STUDIO-6: takes the selected game off sale after confirming. */
    @FXML
    private void onDelist() {
        Game game = gamesTable.getSelectionModel().getSelectedItem();
        if (game == null || game.getStatus() != GameStatus.PUBLISHED) {
            return;
        }
        Dialogs.confirm(context.navigator().window(), I18n.format("studio.delist.confirm", game.getName()),
                "studio.delist", () -> context.tasks().run(delistButton, () -> context.services().publishing()
                        .delist(context.session().userId(), game.getId()), delisted -> load()));
    }

    /** Enables the actions that fit the selected game. */
    private void updateActions() {
        Game game = gamesTable.getSelectionModel().getSelectedItem();
        boolean draft = game != null && game.getStatus() == GameStatus.DRAFT;
        boolean published = game != null && game.getStatus() == GameStatus.PUBLISHED;
        editButton.setDisable(game == null);
        editItem.setDisable(game == null);
        publishButton.setDisable(!draft);
        publishItem.setDisable(!draft);
        delistButton.setDisable(!published);
        delistItem.setDisable(!published);
    }

    /**
     * Turns a game into the value of a cell.
     *
     * @param cell   the cell's data
     * @param getter what the column shows
     * @param <T>    the value type
     * @return the cell's value
     */
    private static <T> ReadOnlyObjectWrapper<T> value(TableColumn.CellDataFeatures<Game, T> cell,
                                                      Function<Game, T> getter) {
        return new ReadOnlyObjectWrapper<>(getter.apply(cell.getValue()));
    }

    /**
     * Creates a cell that shows its value through a formatter.
     *
     * @param format the formatter
     * @param <T>    the value type
     * @return the cell
     */
    private static <T> TableCell<Game, T> textCell(Function<T, String> format) {
        return new TableCell<>() {
            @Override
            protected void updateItem(T value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : format.apply(value));
            }
        };
    }

    /** Shows a game's status as a colored chip. */
    private static final class StatusCell extends TableCell<Game, GameStatus> {

        /** Creates an empty cell; the table fills it. */
        StatusCell() {
            // The table calls updateItem.
        }

        @Override
        protected void updateItem(GameStatus status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label chip = new Label(Formats.label(status));
            chip.getStyleClass().addAll("mist-chip", "mist-status-" + status.name().toLowerCase(Locale.ROOT));
            setText(null);
            setGraphic(chip);
        }
    }
}
