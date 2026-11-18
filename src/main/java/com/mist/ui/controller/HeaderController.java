package com.mist.ui.controller;

import com.mist.model.Role;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller of the header: the navigation links of the user's role, in the shell's top bar.
 *
 * <p>Implements the behaviors HEADER-1 to HEADER-3 of {@code docs/ui-design.md}. The shell tells it which screen is
 * on display, so the matching link is highlighted.</p>
 */
public class HeaderController {

    /** What the controller works with. */
    private final UiContext context;
    /** Each link and the screen it opens. */
    private final Map<ToggleButton, View> targets = new LinkedHashMap<>();
    /** Shows the links of the role when the session changes; kept here because the session holds it weakly. */
    private final ChangeListener<Number> sessionListener = (observable, before, after) -> refresh();
    /** The screen on display, whose link stays selected. */
    private View current;

    /** The links, one selected at most. */
    @FXML
    private ToggleGroup links;
    /** Opens the store. */
    @FXML
    private ToggleButton storeLink;
    /** Opens the operation log, for admins. */
    @FXML
    private ToggleButton logLink;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public HeaderController(UiContext context) {
        this.context = context;
    }

    /** Maps the links to their screens and shows those of the user's role. */
    @FXML
    private void initialize() {
        targets.put(storeLink, View.STORE);
        targets.put(logLink, View.ADMIN_LOG);
        context.session().versionProperty().addListener(new WeakChangeListener<>(sessionListener));
        refresh();
    }

    /** HEADER-1: shows the links of the user's role. */
    public void refresh() {
        show(logLink, context.session().hasRole(Role.ADMIN));
    }

    /**
     * HEADER-3: highlights the link of the screen on display, or none.
     *
     * @param view the screen on display
     */
    public void highlight(View view) {
        current = view;
        targets.forEach((link, target) -> link.setSelected(target == view));
    }

    /**
     * HEADER-2: opens the screen of the clicked link.
     *
     * @param event the action of a link
     */
    @FXML
    private void onLink(ActionEvent event) {
        ToggleButton link = (ToggleButton) event.getSource();
        View target = targets.get(link);
        if (target == current) {
            // Clicking the current screen's link keeps it highlighted instead of clearing it.
            link.setSelected(true);
            return;
        }
        context.navigator().show(target);
    }

    /**
     * Shows or hides a link, taking its space only when shown.
     *
     * @param link    the link
     * @param visible whether to show it
     */
    private static void show(Node link, boolean visible) {
        link.setVisible(visible);
        link.setManaged(visible);
    }
}
