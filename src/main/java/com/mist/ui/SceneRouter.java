package com.mist.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;

/**
 * Loads views. Every FXML load in MIST goes through here, so each view gets the current message bundle, its
 * TailwindFX utilities and a controller built with the {@link UiContext}.
 */
public final class SceneRouter {

    private SceneRouter() {
    }

    /**
     * A loaded view: its root node and its controller.
     *
     * @param root       the root node of the view
     * @param controller the controller FXML created, or {@code null} if the view has none
     */
    public record Loaded(Parent root, Object controller) {
    }

    /**
     * Loads a view with the messages of the current language, gives its controller the context, and applies its
     * TailwindFX utilities.
     *
     * <p>A controller with a constructor taking a {@link UiContext} gets the context; one with a no-argument
     * constructor is built without it.</p>
     *
     * @param view    the view to load
     * @param context what the controller needs, or {@code null} to load the view for display only
     * @return the root node and the controller
     * @throws IOException if the FXML file cannot be read or parsed
     */
    public static Loaded load(View view, UiContext context) throws IOException {
        return load(resource(view.fxml()), context);
    }

    /**
     * Loads an FXML file next to the views, such as a dialog's content, the same way as a view.
     *
     * @param fxml    the FXML path, relative to {@code com/mist/ui/}
     * @param context what the controller needs, or {@code null}
     * @return the root node and the controller
     * @throws IOException if the FXML file cannot be read or parsed
     */
    public static Loaded load(String fxml, UiContext context) throws IOException {
        return load(resource(fxml), context);
    }

    /**
     * Loads a view for display only, without a context.
     *
     * @param view the view to load
     * @return the root node of the view
     * @throws IOException if the FXML file cannot be read or parsed
     */
    public static Parent load(View view) throws IOException {
        return load(view, null).root();
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

    /**
     * Loads an FXML document.
     *
     * @param url     the document
     * @param context the context for its controller
     * @return the root node and the controller
     * @throws IOException if the document cannot be read or parsed
     */
    private static Loaded load(URL url, UiContext context) throws IOException {
        FXMLLoader loader = new FXMLLoader(url, I18n.bundle());
        loader.setControllerFactory(type -> controller(type, context));
        Parent root = loader.load();
        Styles.process(root);
        return new Loaded(root, loader.getController());
    }

    /**
     * Builds a controller, passing the context to the constructor that takes it.
     *
     * @param type    the controller class named by {@code fx:controller}
     * @param context the context, possibly {@code null}
     * @return the controller
     */
    private static Object controller(Class<?> type, UiContext context) {
        try {
            if (Arrays.stream(type.getConstructors()).anyMatch(SceneRouter::takesContext)) {
                return type.getConstructor(UiContext.class).newInstance(context);
            }
            return type.getConstructor().newInstance();
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Controller " + type.getName() + " failed to start", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Controller " + type.getName() + " cannot be created", e);
        }
    }

    /**
     * Tells whether a constructor takes exactly the context.
     *
     * @param constructor a public constructor of a controller
     * @return {@code true} for {@code Controller(UiContext)}
     */
    private static boolean takesContext(Constructor<?> constructor) {
        return constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0] == UiContext.class;
    }

    /**
     * Finds a UI resource.
     *
     * @param path the path relative to {@code com/mist/ui/}
     * @return its URL
     */
    private static URL resource(String path) {
        return Objects.requireNonNull(SceneRouter.class.getResource(path), () -> "Missing view com/mist/ui/" + path);
    }
}
