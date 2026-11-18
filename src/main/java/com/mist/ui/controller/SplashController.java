package com.mist.ui.controller;

import com.mist.exception.DataAccessException;
import com.mist.ui.NavigationAware;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller of the splash view, the first screen of MIST.
 *
 * <p>While the splash is shown, it connects to the database, then opens Login, or the offline state if MongoDB
 * cannot be reached (behavior SPLASH-1 of {@code docs/ui-design.md}). A splash loaded without a context, as the brand
 * test does, only shows the art.</p>
 */
public class SplashController implements NavigationAware {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");

    /** What the controller works with, or {@code null} for a display-only splash. */
    private final UiContext context;

    /** The loading bar, indeterminate while connecting. */
    @FXML
    private ProgressBar progress;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with, or {@code null} to only show the art
     */
    public SplashController(UiContext context) {
        this.context = context;
    }

    @Override
    public void onNavigatedTo(Object parameter) {
        if (context == null) {
            return;
        }
        // SPLASH-1: connect while the brand art shows.
        progress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        context.tasks().run(null, context::connect, services -> {
            // R1: connected.
            LOG.info("Connected to the database");
            context.navigator().show(View.LOGIN);
        }, error -> {
            if (error instanceof DataAccessException) {
                // R2: MongoDB cannot be reached.
                LOG.warn("Cannot reach the database: {}", error.getMessage());
                context.navigator().show(View.OFFLINE);
            } else {
                context.errors().unexpected(context.navigator().window(), error);
            }
        });
    }
}
