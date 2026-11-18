package com.mist.ui;

import com.mist.model.Achievement;
import com.mist.model.Game;
import com.mist.model.GameStatus;
import com.mist.model.ModelKeys;
import com.mist.model.Pegi;
import com.mist.model.User;
import com.mist.service.DemoData;
import com.mist.service.ServiceKeys;
import com.mist.ui.component.AchievementRow;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.TilePane;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game editor (EDIT-1 to EDIT-9), its editable achievements table and how the dashboard opens it (STUDIO-4).
 */
class GameEditorTest extends MistUiTest {

    /** The pseudo-class of a field in error. */
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    /** Where the test images are written. */
    @TempDir
    Path folder;

    @Test
    void theDashboardOpensAGameInTheEditor() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        TableView<Game> games = node("gamesTable");
        Game draft = games.getItems().stream().filter(g -> g.getName().equals(DemoData.DRAFT_GAME)).findFirst()
                .orElseThrow();
        robot.interact(() -> games.getSelectionModel().select(draft));

        click("editButton");

        assertEquals(View.GAME_EDITOR, navigator.current());
        assertEquals(DemoData.DRAFT_GAME, text("nameField"));
        assertEquals("9,99", text("priceField"));
        assertEquals(Pegi.PEGI_12, this.<ComboBox<Pegi>>node("pegiBox").getValue());
        assertEquals(2, this.<FlowPane>node("tagsPane").getChildren().size());
        assertEquals(3, achievements().getItems().size());
        assertTrue(this.<javafx.scene.control.CheckBox>node("lang-es").isSelected());
        assertEquals(msg("enum.GameStatus.DRAFT"), text("statusLabel"));
    }

    @Test
    void theWholeFormIsSaved() throws Exception {
        openEditor(DemoData.DRAFT_GAME);

        type("descriptionArea", "A lighthouse, a radio and a message from tomorrow.");
        type("priceField", "11,50");
        type("tagField", "Indie");
        click("addTagButton");
        click("lang-fr");
        click("removeTag-mystery");
        editAchievement(0, "First signal");
        click("saveButton");

        assertEquals(msg("editor.saved"), text("formMessage"));
        Game saved = db.gameByName(DemoData.DRAFT_GAME);
        assertEquals("A lighthouse, a radio and a message from tomorrow.", saved.getDescription());
        assertEquals(new BigDecimal("11.50"), saved.getPrice());
        assertEquals(List.of("narrative", "indie"), saved.getTags());
        assertTrue(saved.getLanguages().contains("fr"));
        assertEquals("First signal", saved.getAchievements().getFirst().name());
    }

    @Test
    void anEmptyAchievementNameIsRefusedInTheCell() throws Exception {
        openEditor(DemoData.DRAFT_GAME);

        editAchievement(1, "  ");

        assertEquals(msg(ModelKeys.ACHIEVEMENT), text("formMessage"));
        assertEquals("Collector", achievements().getItems().get(1).getName());
    }

    @Test
    void achievementsAreAddedAndRemoved() throws Exception {
        openEditor(DemoData.DRAFT_GAME);

        click("addAchievementButton");
        assertEquals(4, achievements().getItems().size());
        robot.interact(() -> achievements().getSelectionModel().select(0));
        click("removeAchievementButton");
        click("saveButton");

        List<Achievement> saved = db.gameByName(DemoData.DRAFT_GAME).getAchievements();
        assertEquals(3, saved.size());
        assertEquals(msg("editor.newAchievement"), saved.getLast().name());
    }

    @Test
    void requirementsMustBeWholeOrEmpty() throws Exception {
        openEditor(DemoData.DRAFT_GAME);

        type("processorField", "");
        click("saveButton");

        assertTrue(marked("osField"));
        assertEquals(msg(ModelKeys.REQUIREMENTS), text("formMessage"));
    }

    @Test
    void aWrongPriceIsMarked() throws Exception {
        openEditor(DemoData.DRAFT_GAME);

        type("priceField", "1000");
        click("saveButton");

        assertTrue(marked("priceField"));
        assertEquals(new BigDecimal("9.99"), db.gameByName(DemoData.DRAFT_GAME).getPrice());
    }

    @Test
    void aCompleteDraftIsPublishedFromTheEditor() throws Exception {
        openEditor(DemoData.DRAFT_GAME);

        click("publishButton");
        click("dialogConfirm");

        assertEquals(msg("editor.published"), text("formMessage"));
        assertEquals(GameStatus.PUBLISHED, db.gameByName(DemoData.DRAFT_GAME).getStatus());
        assertEquals(msg("enum.GameStatus.PUBLISHED"), text("statusLabel"));
    }

    @Test
    void whatADraftLacksIsMarkedBeforePublishing() throws Exception {
        User nebula = backend.services().auth().login(DemoData.DEVELOPER_EMAIL, DemoData.PASSWORD.toCharArray());
        backend.services().publishing().createDraft(nebula.getId(), "Hollow Echo", new BigDecimal("5"));
        openEditor("Hollow Echo");

        click("publishButton");

        assertEquals(msg(ModelKeys.GAME_INCOMPLETE), text("formMessage"));
        assertTrue(marked("descriptionArea"));
        assertTrue(this.<Node>node("coverFrame").getPseudoClassStates().contains(ERROR));
        assertEquals(GameStatus.DRAFT, db.gameByName("Hollow Echo").getStatus());
    }

    @Test
    void theCoverAndScreenshotsAreUploadedAndRemoved() throws Exception {
        openEditor(DemoData.DRAFT_GAME);
        ObjectId oldCover = db.gameByName(DemoData.DRAFT_GAME).getCoverId();

        files.answerImage(png("cover.png"));
        click("changeCoverButton");
        assertNotEquals(oldCover, db.gameByName(DemoData.DRAFT_GAME).getCoverId());

        files.answerImage(png("shot.png"));
        click("addScreenshotButton");
        TilePane shots = node("screenshotsPane");
        assertEquals(3, shots.getChildren().size());

        ContextMenu menu = (ContextMenu) shots.getChildren().getFirst().getProperties().get("mist.menu");
        robot.interact(() -> menu.getItems().getFirst().fire());
        assertEquals(2, db.gameByName(DemoData.DRAFT_GAME).getScreenshotIds().size());
    }

    @Test
    void aPublishedGameIsTakenOffSaleFromTheEditor() throws Exception {
        openEditor("Ashen Lanterns");

        click("delistButton");
        click("dialogConfirm");

        assertEquals(GameStatus.DELISTED, db.gameByName("Ashen Lanterns").getStatus());
        assertTrue(this.<javafx.scene.control.Button>node("publishButton").isDisable());
    }

    @Test
    void anotherDevelopersGameIsRefused() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        ObjectId theirs = db.gameByName("Starlit Drift").getId();

        robot.interact(() -> navigator.show(View.GAME_EDITOR, theirs));

        assertEquals(msg(ServiceKeys.GAME_NOT_OWNER), dialogMessage());
        assertEquals(View.STUDIO, navigator.current());
    }

    @Test
    void backReturnsToTheDashboard() throws Exception {
        openEditor(DemoData.DRAFT_GAME);

        click("backButton");

        assertEquals(View.STUDIO, navigator.current());
    }

    /**
     * Logs in as the first demo developer and opens one of their games in the editor.
     *
     * @param name the game's name
     * @throws Exception if the login fails
     */
    private void openEditor(String name) throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        ObjectId id = db.gameByName(name).getId();
        robot.interact(() -> navigator.show(View.GAME_EDITOR, id));
    }

    /**
     * Edits an achievement's name in the table: starts editing the cell, types and presses Enter.
     *
     * @param row  the row's index
     * @param name the text to type
     */
    @SuppressWarnings("unchecked")
    private void editAchievement(int row, String name) {
        TableView<AchievementRow> table = achievements();
        TableColumn<AchievementRow, String> column = (TableColumn<AchievementRow, String>) table.getColumns().get(0);
        robot.interact(() -> {
            table.layout();
            table.edit(row, column);
        });
        TextField editor = (TextField) table.lookupAll(".text-field").stream().filter(Node::isVisible).findFirst()
                .orElseThrow();
        robot.interact(() -> {
            editor.setText(name);
            editor.fireEvent(new ActionEvent());
        });
    }

    /**
     * Returns the achievements table.
     *
     * @return the table
     */
    private TableView<AchievementRow> achievements() {
        return node("achievementsTable");
    }

    /**
     * Writes a small PNG image.
     *
     * @param name the file name
     * @return the file
     * @throws Exception if writing fails
     */
    private Path png(String name) throws Exception {
        Path file = folder.resolve(name);
        ImageIO.write(new BufferedImage(16, 9, BufferedImage.TYPE_INT_RGB), "png", file.toFile());
        return file;
    }

    /**
     * Tells whether a field shows the error mark.
     *
     * @param id the field's fx:id
     * @return {@code true} if it has the {@code :error} pseudo-class
     */
    private boolean marked(String id) {
        Control field = node(id);
        return field.getPseudoClassStates().contains(ERROR);
    }
}
