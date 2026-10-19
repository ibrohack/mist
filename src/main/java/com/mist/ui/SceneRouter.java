package com.mist.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;

/**
 * Loads views. Every FXML load in MIST goes through here, so each view gets the current message bundle and its
 * TailwindFX utilities.
 */
public final class SceneRouter {

    private SceneRouter() {
    }

    /**
     * Loads a view with the messages of the current language and applies its TailwindFX utilities.
     *
     * @param view the view to load
     * @return the root node of the view
     * @throws IOException if the FXML file cannot be read or parsed
     */
    public static Parent load(View view) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneRouter.class.getResource(view.fxml()), I18n.bundle());
        Parent root = loader.load();
        Styles.process(root);
        return root;
    }

    /**
     * Creates a scene for a view, sized by the view's preferred size and styled with TailwindFX and the brand theme.
     *
     * @param view the view to show
     * @return a scene holding the view
     * @throws IOException if the FXML file cannot be read or parsed
     */
    public static Scene createScene(View view) throws IOException {
        Scene scene = new Scene(load(view));
        Styles.install(scene);
        return scene;
    }
}
