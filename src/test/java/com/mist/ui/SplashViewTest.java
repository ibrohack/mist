package com.mist.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opens the splash view as MIST does and checks that TailwindFX utilities, the brand stylesheet, the font and the
 * bundles all reach the screen.
 */
@ExtendWith(ApplicationExtension.class)
class SplashViewTest {

    private Scene scene;

    @Start
    void start(Stage stage) throws IOException {
        scene = SceneRouter.createScene(View.SPLASH);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void taglineIsSpanishByDefault(FxRobot robot) {
        assertEquals("PREPARANDO TU PRÓXIMO MUNDO", robot.lookup("#tagline").queryLabeled().getText());
    }

    @Test
    void brandStylesheetIsInstalledLast() {
        List<String> sheets = scene.getStylesheets();

        assertTrue(sheets.getLast().endsWith("css/mist-theme.css"), () -> "last stylesheet: " + sheets.getLast());
        assertTrue(sheets.stream().anyMatch(s -> s.startsWith("data:text/css")), () -> "stylesheets: " + sheets);
    }

    @Test
    void tailwindUtilitiesShapeTheBrandBlock(FxRobot robot) {
        VBox brandBlock = robot.lookup("#brandBlock").queryAs(VBox.class);

        assertEquals(new Insets(96), brandBlock.getPadding());
        assertEquals(24.0, brandBlock.getSpacing());
    }

    @Test
    void tailwindArbitraryValueSizesTheProgressBar(FxRobot robot) {
        assertEquals(350.0, robot.lookup("#progress").queryAs(ProgressBar.class).getPrefWidth());
    }

    @Test
    void brandUtilitiesColorTheText(FxRobot robot) {
        Labeled wordmark = robot.lookup("#wordmark").queryLabeled();
        Labeled tagline = robot.lookup("#tagline").queryLabeled();

        assertEquals(Color.web("#f2f5ee"), wordmark.getTextFill());
        assertEquals(Color.web("#818782"), tagline.getTextFill());
    }

    @Test
    void wordmarkUsesSpaceGrotesk(FxRobot robot) {
        assertEquals("Space Grotesk", robot.lookup("#wordmark").queryLabeled().getFont().getFamily());
    }

    @Test
    void wordmarkMatchesTheKitTypography(FxRobot robot) {
        Labeled wordmark = robot.lookup("#wordmark").queryLabeled();

        assertEquals(104.0, wordmark.getFont().getSize());
        assertEquals("Bold", wordmark.getFont().getStyle());
    }

    @Test
    void progressBarUsesAcid(FxRobot robot) {
        ProgressBar progress = robot.lookup("#progress").queryAs(ProgressBar.class);
        Region bar = (Region) progress.lookup(".bar");

        assertEquals(Color.web("#c6ff3d"), bar.getBackground().getFills().getFirst().getFill());
    }

    @Test
    void runsOnJavaFx25() {
        String version = System.getProperty("javafx.runtime.version");

        assertTrue(version.startsWith("25"), () -> "JavaFX runtime " + version);
    }
}
