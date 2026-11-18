package com.mist.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

/** Controller of the header bar: the brand and the navigation links. */
public class HeaderController {

    /** Opens the cart. */
    @FXML
    private Label LblCart;

    /** Opens the library. */
    @FXML
    private Label LblLibrary;

    /** Opens the store. */
    @FXML
    private Label LblStore;

    /** The MIST logo. */
    @FXML
    private ImageView Logo;

    /** The user's avatar. */
    @FXML
    private Circle Profile;

    /** Creates the controller; FXML injects the fields. */
    public HeaderController() {
        // Nothing to set up.
    }

}
