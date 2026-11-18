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
import com.mist.dao.DaoKeys;
import com.mist.exception.InvalidDocumentException;
import java.nio.file.Files;
import javafx.scene.control.MenuItem;
import javafx.stage.WindowEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
        assertEquals("79,99", text("priceField"));
        assertEquals(Pegi.PEGI_18, this.<ComboBox<Pegi>>node("pegiBox").getValue());
        assertEquals(2, this.<FlowPane>node("tagsPane").getChildren().size());
        assertEquals(3, achievements().getItems().size());
        assertTrue(this.<javafx.scene.control.CheckBox>node("lang-es").isSelected());
        assertEquals(msg("enum.GameStatus.DRAFT"), text("statusLabel"));
    }

    @Test
    void theWholeFormIsSaved() throws Exception {
        openEditor(DemoData.DRAFT_GAME);

        type("descriptionArea", "Two outlaws, one state and a heist gone wrong.");
        type("priceField", "11,50");
        type("tagField", "Indie");
        click("addTagButton");
        click("lang-fr");
        click("removeTag-crime");
        editAchievement(0, "First heist");
        click("saveButton");

        assertEquals(msg("editor.saved"), text("formMessage"));
        Game saved = db.gameByName(DemoData.DRAFT_GAME);
        assertEquals("Two outlaws, one state and a heist gone wrong.", saved.getDescription());
        assertEquals(new BigDecimal("11.50"), saved.getPrice());
        assertEquals(List.of("open world", "indie"), saved.getTags());
        assertTrue(saved.getLanguages().contains("fr"));
        assertEquals("First heist", saved.getAchievements().getFirst().name());
    }

    @Test
    void anEmptyAchievementNameIsRefusedInTheCell() throws Exception {
        openEditor(DemoData.DRAFT_GAME);

        editAchievement(1, "  ");

        assertEquals(msg(ModelKeys.ACHIEVEMENT), text("formMessage"));
        assertEquals("Bonnie and Clyde", achievements().getItems().get(1).getName());
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
        assertEquals(new BigDecimal("79.99"), db.gameByName(DemoData.DRAFT_GAME).getPrice());
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
        openEditor("Grand Theft Auto V");

        click("delistButton");
        click("dialogConfirm");

        assertEquals(GameStatus.DELISTED, db.gameByName("Grand Theft Auto V").getStatus());
        assertTrue(this.<javafx.scene.control.Button>node("publishButton").isDisable());
    }

    @Test
    void anotherDevelopersGameIsRefused() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        ObjectId theirs = db.gameByName("Red Dead Redemption 2").getId();

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

    @Test
    void aDatabaseFailureWhileSavingOffersARetry() throws Exception {
        openEditor(DemoData.DRAFT_GAME);
        type("descriptionArea", "Saved on the second try.");
        db.failNextCall();

        click("saveButton");
        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        click("dialogRetry");

        assertEquals("Saved on the second try.", db.gameByName(DemoData.DRAFT_GAME).getDescription());
        assertEquals(msg("editor.saved"), text("formMessage"));
    }

    @Test
    void aDatabaseFailureWhilePublishingOffersARetry() throws Exception {
        openEditor(DemoData.DRAFT_GAME);
        db.failNextCall();

        click("publishButton");
        click("dialogConfirm");
        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        assertEquals(GameStatus.DRAFT, db.gameByName(DemoData.DRAFT_GAME).getStatus());
        click("dialogRetry");

        assertEquals(GameStatus.PUBLISHED, db.gameByName(DemoData.DRAFT_GAME).getStatus());
    }

    @Test
    void aValueTheDatabaseRefusesIsAWarningWithoutRetry() throws Exception {
        openEditor(DemoData.DRAFT_GAME);
        db.failNextCall(new InvalidDocumentException(DaoKeys.INVALID_DOCUMENT));

        click("saveButton");

        assertEquals(msg(DaoKeys.INVALID_DOCUMENT), dialogMessage());
        assertFalse(visible("dialogRetry"));
    }

    @Test
    void aDatabaseFailureWhileUploadingTheCoverKeepsTheOldOne() throws Exception {
        openEditor(DemoData.DRAFT_GAME);
        ObjectId oldCover = db.gameByName(DemoData.DRAFT_GAME).getCoverId();
        files.answerImage(png("cover.png"));
        db.failNextCall();

        click("changeCoverButton");

        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        assertEquals(oldCover, db.gameByName(DemoData.DRAFT_GAME).getCoverId());
    }

    @Test
    void anOversizedCoverIsRefused() throws Exception {
        openEditor(DemoData.DRAFT_GAME);
        ObjectId oldCover = db.gameByName(DemoData.DRAFT_GAME).getCoverId();
        Path big = folder.resolve("big.png");
        Files.write(big, new byte[3 * 1024 * 1024]);
        files.answerImage(big);

        click("changeCoverButton");

        assertEquals(msg(ModelKeys.MEDIA_SIZE), dialogMessage());
        assertEquals(oldCover, db.gameByName(DemoData.DRAFT_GAME).getCoverId());
    }

    @Test
    void anUnreadableScreenshotIsReported() throws Exception {
        openEditor(DemoData.DRAFT_GAME);
        files.answerImage(folder.resolve("gone.png"));

        click("addScreenshotButton");

        assertEquals(msg("error.fileUnreadable"), dialogMessage());
        assertEquals(2, db.gameByName(DemoData.DRAFT_GAME).getScreenshotIds().size());
    }

    @Test
    void achievementsAreAddedAndRemovedFromTheContextMenu() throws Exception {
        openEditor(DemoData.DRAFT_GAME);
        ContextMenu menu = achievements().getContextMenu();

        robot.interact(() -> menu.getOnShowing().handle(new WindowEvent(menu, WindowEvent.WINDOW_SHOWING)));
        assertTrue(menuItem(menu, "removeAchievementItem").isDisable());
        robot.interact(menuItem(menu, "addAchievementItem")::fire);
        assertEquals(4, achievements().getItems().size());

        robot.interact(() -> achievements().getSelectionModel().select(0));
        robot.interact(() -> menu.getOnShowing().handle(new WindowEvent(menu, WindowEvent.WINDOW_SHOWING)));
        assertFalse(menuItem(menu, "removeAchievementItem").isDisable());
        robot.interact(menuItem(menu, "removeAchievementItem")::fire);

        assertEquals(3, achievements().getItems().size());
    }

    /**
     * Returns an item of a context menu.
     *
     * @param menu the menu
     * @param id   the item's fx:id
     * @return the item
     */
    private static MenuItem menuItem(ContextMenu menu, String id) {
        return menu.getItems().stream().filter(item -> id.equals(item.getId())).findFirst().orElseThrow();
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
