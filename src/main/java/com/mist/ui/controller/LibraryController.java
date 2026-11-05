package com.mist.ui.controller;

import com.mist.model.Game;

import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

/**
 * Controller of the library view, the main interface for browsing and managing the media library.
 *
 * <p>While the library is shown, it displays the media items and allows the user to interact with them, or the offline state if MongoDB
 * cannot be reached.</p>
 */
public class LibraryController {
    @FXML
    private TilePane gameGrid;

    @FXML
    private ListView<?> gameList;

    @FXML
    private TextField searchField;

    public void initialize() {
        // Initialization code for the library view
    }

    /* public void addGame(String cover, String title) {
        VBox card = new VBox(8, cover, title);
        card.getStyleClass().add("game-card");
        card.setPrefSize(180, 260);
        gameGrid.getChildren().add(card);
    } */

    /* public void filterGameSearch(String query) {
        FilteredList<Game> filtered = new FilteredList<>(allGames, g -> true);
        searchField.textProperty().addListener((obs, o, n) ->
            filtered.setPredicate(g -> g.getName().toLowerCase().contains(n.toLowerCase())));
        gameList.setItems(filtered);
    } */


}
