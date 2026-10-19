package com.mist;

import com.mist.ui.I18n;
import com.mist.ui.SceneRouter;
import com.mist.ui.Styles;
import com.mist.ui.View;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The MIST desktop application. It opens the splash screen, which leads to the rest of the app.
 */
public class MistApp extends Application {

    private static final double MIN_WIDTH = 1024;
    private static final double MIN_HEIGHT = 640;

    @Override
    public void start(Stage stage) throws IOException {
        stage.setScene(SceneRouter.createScene(View.SPLASH));
        stage.setTitle(I18n.bundle().getString("app.title"));
        stage.getIcons().setAll(Styles.appIcons());
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.show();
    }
}
