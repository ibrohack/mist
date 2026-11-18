package com.mist.ui.controller;

import com.mist.exception.DataAccessException;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller of the offline state, shown when MongoDB cannot be reached at startup.
 *
 * <p>Implements the behavior OFFLINE-1 of {@code docs/ui-design.md}: Retry connects again.</p>
 */
public class OfflineController {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");

    /** What the controller works with. */
    private final UiContext context;

    /** Tries to connect again. */
    @FXML
    private Button retryButton;
    /** Tells that a retry failed too; hidden until then. */
    @FXML
    private Label retryStatus;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public OfflineController(UiContext context) {
        this.context = context;
    }

    /** Hides the retry status until a retry fails. */
    @FXML
    private void initialize() {
        retryStatus.setVisible(false);
    }

    /** OFFLINE-1: connects again. */
    @FXML
    private void onRetry() {
        retryStatus.setVisible(false);
        LOG.info("Retrying the database connection");
        context.tasks().run(retryButton, context::connect,
                // R1: connected.
                services -> context.navigator().show(View.LOGIN),
                error -> {
                    if (error instanceof DataAccessException) {
                        // R2: still offline; the button is enabled again when the task ends.
                        retryStatus.setVisible(true);
                    } else {
                        context.errors().unexpected(context.navigator().window(), error);
                    }
                });
    }

    /** HELP-1: opens the help on the offline topic. */
    @FXML
    private void onHelp() {
        context.navigator().showHelp();
    }
}
