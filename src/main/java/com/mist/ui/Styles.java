package com.mist.ui;

import com.mist.model.Theme;
import io.github.yasmramos.tailwindfx.TwFXML;
import io.github.yasmramos.tailwindfx.TwInstall;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Single entry point to TailwindFX and the brand resources: fonts, stylesheets and launcher icons.
 *
 * <p>No other class imports TailwindFX, so a change of styling library touches only this class.</p>
 */
public final class Styles {

    /** The styling log. */
    private static final Logger LOG = LoggerFactory.getLogger(Styles.class);

    /** The brand theme, dark by default. */
    private static final String THEME = "css/mist-theme.css";
    /** Redefines the brand tokens for the light theme; appended after {@link #THEME}. */
    private static final String LIGHT_THEME = "css/mist-theme-light.css";
    /** The stylesheet Maven generates for the static TailwindFX utilities. */
    private static final String TAILWIND_GENERATED = "/css/tailwindfx-generated.css";
    /** The Space Grotesk fonts. */
    private static final List<String> FONTS = List.of(
            "fonts/SpaceGrotesk-Regular.ttf",
            "fonts/SpaceGrotesk-Medium.ttf",
            "fonts/SpaceGrotesk-Bold.ttf");
    /** The sizes of the launcher icons. */
    private static final int[] ICON_SIZES = {16, 32, 48, 64, 256};

    /** Whether the fonts are loaded already. */
    private static boolean fontsLoaded;

    /** Not instantiable: a holder of the styling facade. */
    private Styles() {
    }

    /**
     * Styles a scene: loads the brand fonts, installs TailwindFX, then appends the brand theme last so it wins over
     * TailwindFX's stylesheets.
     *
     * <p>TailwindFX contributes its base variables and the stylesheet that the build generates for the static
     * utilities used in {@code src/main}.</p>
     *
     * @param scene the scene to style
     */
    public static void install(Scene scene) {
        loadFonts();
        TwInstall.installGenerated(scene, TAILWIND_GENERATED);
        scene.getStylesheets().add(resource(THEME).toExternalForm());
    }

    /**
     * Switches a scene between the dark and the light theme (behavior C-5 of {@code docs/ui-design.md}).
     *
     * <p>The light stylesheet only redefines the brand tokens, so it goes last and every view follows.</p>
     *
     * @param scene the scene, already styled by {@link #install(Scene)}
     * @param theme the theme to show
     */
    public static void applyTheme(Scene scene, Theme theme) {
        String light = resource(LIGHT_THEME).toExternalForm();
        scene.getStylesheets().remove(light);
        if (theme == Theme.LIGHT) {
            scene.getStylesheets().add(light);
        }
    }

    /**
     * Applies the TailwindFX utilities declared in a view's style classes.
     *
     * @param root the root node of a freshly loaded view
     */
    public static void process(Parent root) {
        TwFXML.process(root);
    }

    /**
     * Returns the launcher icon in every size the platform may ask for.
     *
     * @return the icons, smallest first
     */
    public static List<Image> appIcons() {
        return IntStream.of(ICON_SIZES)
                .mapToObj(size -> new Image(resource("images/mist-icon-" + size + ".png").toExternalForm()))
                .toList();
    }

    /** Loads the brand fonts once. */
    private static void loadFonts() {
        if (fontsLoaded) {
            return;
        }
        for (String font : FONTS) {
            if (Font.loadFont(resource(font).toExternalForm(), 0) == null) {
                LOG.warn("Could not load font {}; JavaFX will fall back to the system font", font);
            }
        }
        fontsLoaded = true;
    }

    /**
     * Finds a UI resource.
     *
     * @param path the path relative to {@code com/mist/ui/}
     * @return its URL
     */
    private static URL resource(String path) {
        return Objects.requireNonNull(Styles.class.getResource(path), () -> "Missing UI resource com/mist/ui/" + path);
    }
}
