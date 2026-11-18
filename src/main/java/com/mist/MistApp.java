package com.mist;

import com.mist.ui.I18n;
import com.mist.ui.Navigator;
import com.mist.ui.Styles;
import com.mist.ui.SystemFilePicker;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * The MIST desktop application. It builds the UI context on the real backend and opens the splash screen, which
 * leads to the rest of the app.
 */
public class MistApp extends Application {

    /** Smallest window width the layouts are designed for. */
    private static final double MIN_WIDTH = 1024;
    /** Smallest window height the layouts are designed for. */
    private static final double MIN_HEIGHT = 640;

    /** The context of the main window, closed when the app stops. */
    private UiContext context;

    @Override
    public void start(Stage stage) {
        I18n.use(I18n.DEFAULT_LOCALE);
        context = new UiContext(new MistBackend(), new SystemFilePicker());
        Navigator navigator = new Navigator(stage, context);
        stage.setTitle(I18n.text("app.title"));
        stage.getIcons().setAll(Styles.appIcons());
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        navigator.show(View.SPLASH);
        stage.show();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }
}
