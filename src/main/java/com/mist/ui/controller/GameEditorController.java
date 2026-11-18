package com.mist.ui.controller;

import com.mist.exception.MistException;
import com.mist.exception.ValidationException;
import com.mist.model.Achievement;
import com.mist.model.Game;
import com.mist.model.GameStatus;
import com.mist.model.ModelKeys;
import com.mist.model.Pegi;
import com.mist.model.SystemRequirements;
import com.mist.service.AchievementForm;
import com.mist.service.GameForm;
import com.mist.service.RequirementsForm;
import com.mist.service.ServiceKeys;
import com.mist.ui.I18n;
import com.mist.ui.NavigationAware;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import com.mist.ui.component.AchievementRow;
import com.mist.ui.support.Dialogs;
import com.mist.ui.support.FormValidator;
import com.mist.ui.support.Formats;
import com.mist.ui.support.GameForms;
import com.mist.ui.support.UiTasks;
import com.mist.ui.support.Uploads;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.util.StringConverter;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Controller of the game editor: a developer completes, saves, publishes or takes off sale one of their games.
 *
 * <p>Implements the behaviors EDIT-1 to EDIT-9 of {@code docs/ui-design.md}; it is opened with the game's
 * {@link ObjectId}. The achievements table is edited in place: each committed cell is checked against the model's
 * limits, and a text out of range keeps the old one.</p>
 */
public class GameEditorController implements NavigationAware {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");
    /** The languages offered as check boxes, besides any the game already has. */
    private static final List<String> LANGUAGES = List.of("en", "es", "fr", "de", "it", "pt", "ja", "ko", "zh",
            "ru", "pl", "nl");
    /** Width of a screenshot thumbnail. */
    private static final double SHOT_WIDTH = 160;
    /** Height of a screenshot thumbnail. */
    private static final double SHOT_HEIGHT = 90;
    /** Marks the cover's frame when a cover is missing. */
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    /** What the controller works with. */
    private final UiContext context;
    /** The game's tags, as chips. */
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    /** The achievements being edited. */
    private final ObservableList<AchievementRow> achievements = FXCollections.observableArrayList();
    /** The game as last loaded or saved. */
    private Game game;
    /** Checks the form before saving and marks the service's rejections. */
    private FormValidator form;
    /** Marks the fields a game needs before it can be published. */
    private FormValidator publishCheck;

    /** Goes back to the dashboard. */
    @FXML
    private Button backButton;
    /** The game's name. */
    @FXML
    private Label titleLabel;
    /** Draft, published or delisted. */
    @FXML
    private Label statusLabel;
    /** Takes the game off sale. */
    @FXML
    private Button delistButton;
    /** Publishes the draft. */
    @FXML
    private Button publishButton;
    /** Saves the form. */
    @FXML
    private Button saveButton;
    /** The result of the last action, or the first error. */
    @FXML
    private Label formMessage;
    /** The name. */
    @FXML
    private TextField nameField;
    /** The price. */
    @FXML
    private TextField priceField;
    /** The release date. */
    @FXML
    private DatePicker releasePicker;
    /** The PEGI rating. */
    @FXML
    private ComboBox<Pegi> pegiBox;
    /** The store description. */
    @FXML
    private TextArea descriptionArea;
    /** How long the description is. */
    @FXML
    private Label descriptionCounter;
    /** The cover's frame, marked when a cover is missing. */
    @FXML
    private StackPane coverFrame;
    /** The cover. */
    @FXML
    private ImageView coverView;
    /** Uploads a new cover. */
    @FXML
    private Button changeCoverButton;
    /** The screenshots. */
    @FXML
    private TilePane screenshotsPane;
    /** Uploads a screenshot. */
    @FXML
    private Button addScreenshotButton;
    /** Operating system. */
    @FXML
    private TextField osField;
    /** Processor. */
    @FXML
    private TextField processorField;
    /** Memory. */
    @FXML
    private TextField memoryField;
    /** Graphics. */
    @FXML
    private TextField graphicsField;
    /** Storage. */
    @FXML
    private TextField storageField;
    /** The tags, as chips with a remove cross. */
    @FXML
    private FlowPane tagsPane;
    /** A tag to add. */
    @FXML
    private TextField tagField;
    /** Adds the typed tag. */
    @FXML
    private Button addTagButton;
    /** The language check boxes. */
    @FXML
    private FlowPane languagesPane;
    /** Removes the selected achievement. */
    @FXML
    private Button removeAchievementButton;
    /** Adds an achievement. */
    @FXML
    private Button addAchievementButton;
    /** The achievements, editable in place. */
    @FXML
    private TableView<AchievementRow> achievementsTable;
    /** The achievements table's context menu: add a row, remove the selected one (EDIT-3). */
    @FXML
    private ContextMenu achievementMenu;
    /** Removes the selected achievement; disabled when no row is selected. */
    @FXML
    private MenuItem removeAchievementItem;
    /** The achievements' names, editable. */
    @FXML
    private TableColumn<AchievementRow, String> achievementNameColumn;
    /** The achievements' descriptions, editable. */
    @FXML
    private TableColumn<AchievementRow, String> achievementDescriptionColumn;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public GameEditorController(UiContext context) {
        this.context = context;
    }

    /** Sets up the choices, the editable table, the tags and the checks of every field. */
    @FXML
    private void initialize() {
        // EDIT-3: the context menu can only remove a row when one is selected.
        achievementMenu.setOnShowing(event -> removeAchievementItem.setDisable(
                achievementsTable.getSelectionModel().getSelectedItem() == null));
        pegiBox.getItems().setAll(Pegi.values());
        pegiBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Pegi pegi) {
                return pegi == null ? "" : "PEGI " + pegi.minimumAge();
            }

            @Override
            public Pegi fromString(String text) {
                return null;
            }
        });
        descriptionArea.textProperty().addListener((observable, before, after) -> updateCounter());
        // EDIT-3: the achievements are edited in place and checked on each commit.
        achievementsTable.setItems(achievements);
        achievementNameColumn.setCellValueFactory(cell -> cell.getValue().nameProperty());
        achievementNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        achievementNameColumn.setOnEditCommit(event -> onAchievementCommit(event, Achievement.MAX_NAME_LENGTH,
                AchievementRow::setName));
        achievementDescriptionColumn.setCellValueFactory(cell -> cell.getValue().descriptionProperty());
        achievementDescriptionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        achievementDescriptionColumn.setOnEditCommit(event -> onAchievementCommit(event,
                Achievement.MAX_DESCRIPTION_LENGTH, AchievementRow::setDescription));
        removeAchievementButton.disableProperty().bind(
                achievementsTable.getSelectionModel().selectedItemProperty().isNull());
        tagField.setOnAction(event -> onAddTag());
        // The message line takes room only when it says something.
        formMessage.managedProperty().bind(formMessage.textProperty().isNotEmpty());
        formMessage.visibleProperty().bind(formMessage.managedProperty());
        form = new FormValidator(formMessage)
                .required(nameField).maxLength(nameField, Game.MAX_NAME_LENGTH, ModelKeys.GAME_NAME)
                .required(priceField)
                .rule(priceField, () -> GameForms.parsePrice(priceField.getText()) != null, ModelKeys.GAME_PRICE)
                .maxLength(descriptionArea, Game.MAX_DESCRIPTION_LENGTH, ModelKeys.GAME_DESCRIPTION)
                .rule(osField, this::requirementsWholeOrEmpty, ModelKeys.REQUIREMENTS)
                .bind(ModelKeys.GAME_NAME, nameField).bind(ModelKeys.GAME_PRICE, priceField)
                .bind(ModelKeys.GAME_DESCRIPTION, descriptionArea).bind(ModelKeys.REQUIREMENTS, osField)
                .bind(ModelKeys.GAME_TAGS, tagField).bind(ModelKeys.ACHIEVEMENT, achievementsTable)
                .bind(ModelKeys.GAME_ACHIEVEMENTS, achievementsTable);
        for (TextField field : requirementFields()) {
            form.maxLength(field, SystemRequirements.MAX_LENGTH, ModelKeys.REQUIREMENTS);
        }
        // What a game needs before it can be published, marked all at once (EDIT-7.R2).
        publishCheck = new FormValidator(null)
                .required(descriptionArea).required(releasePicker).required(pegiBox).required(osField);
        updateCounter();
    }

    @Override
    public void onNavigatedTo(Object parameter) {
        // EDIT-1: load the game and check it is the developer's own.
        ObjectId gameId = (ObjectId) parameter;
        context.tasks().run(null, () -> context.services().store().game(gameId), loaded -> {
            if (!loaded.getDeveloperId().equals(context.session().userId())) {
                // R2: another developer's game.
                Dialogs.warning(context.navigator().window(), I18n.text(ServiceKeys.GAME_NOT_OWNER));
                context.navigator().show(View.STUDIO);
                return;
            }
            fill(loaded);
        });
    }

    /**
     * EDIT-1.R1: shows a game in the form.
     *
     * @param shown the game
     */
    private void fill(Game shown) {
        this.game = shown;
        titleLabel.setText(shown.getName());
        nameField.setText(shown.getName());
        priceField.setText(shown.getPrice().toPlainString().replace('.', decimalSeparator()));
        releasePicker.setValue(shown.releaseDay());
        pegiBox.setValue(shown.getPegi());
        descriptionArea.setText(shown.getDescription() == null ? "" : shown.getDescription());
        SystemRequirements requirements = shown.getRequirements();
        List<String> values = requirements == null ? List.of("", "", "", "", "") : List.of(requirements.os(),
                requirements.processor(), requirements.memory(), requirements.graphics(), requirements.storage());
        List<TextField> fields = requirementFields();
        for (int i = 0; i < fields.size(); i++) {
            fields.get(i).setText(values.get(i));
        }
        tags.setAll(shown.getTags());
        renderTags();
        renderLanguages(shown.getLanguages());
        achievements.setAll(shown.getAchievements().stream()
                .map(achievement -> new AchievementRow(achievement.name(), achievement.description())).toList());
        renderMedia();
        renderStatus();
        form.clear();
        publishCheck.clear();
        coverFrame.pseudoClassStateChanged(ERROR, false);
    }

    /** EDIT-2: checks the form and saves it. */
    @FXML
    private void onSave() {
        if (!form.validate()) {
            return;
        }
        GameForm values = formValues();
        save(saveButton, () -> context.services().publishing().update(context.session().userId(), game.getId(),
                values), saved -> {
                    fill(saved);
                    form.showMessage("editor.saved", true);
                    LOG.info("Saved the game {}", saved.getName());
                });
    }

    /** EDIT-7: saves the form and publishes the draft, after confirming. */
    @FXML
    private void onPublish() {
        if (game.getStatus() != GameStatus.DRAFT || !form.validate()) {
            return;
        }
        if (!readyToPublish()) {
            return;
        }
        GameForm values = formValues();
        Dialogs.confirm(context.navigator().window(), I18n.format("studio.publish.confirm", game.getName()),
                "studio.publish", () -> save(publishButton, () -> {
                    context.services().publishing().update(context.session().userId(), game.getId(), values);
                    return context.services().publishing().publish(context.session().userId(), game.getId());
                }, published -> {
                    fill(published);
                    form.showMessage("editor.published", true);
                }));
    }

    /** EDIT-8: takes the game off sale, after confirming. */
    @FXML
    private void onDelist() {
        if (game.getStatus() != GameStatus.PUBLISHED) {
            return;
        }
        Dialogs.confirm(context.navigator().window(), I18n.format("studio.delist.confirm", game.getName()),
                "studio.delist", () -> save(delistButton, () -> context.services().publishing()
                        .delist(context.session().userId(), game.getId()), this::fill));
    }

    /** EDIT-5: uploads a new cover. */
    @FXML
    private void onChangeCover() {
        context.files().openImage(context.navigator().window()).ifPresent(file -> context.tasks().run(
                changeCoverButton, () -> context.services().publishing().changeCover(context.session().userId(),
                        game.getId(), Uploads.image(file)), this::mediaChanged,
                error -> context.errors().report(context.navigator().window(), error)));
    }

    /** EDIT-6: uploads a screenshot. */
    @FXML
    private void onAddScreenshot() {
        context.files().openImage(context.navigator().window()).ifPresent(file -> context.tasks().run(
                addScreenshotButton, () -> context.services().publishing().addScreenshot(context.session().userId(),
                        game.getId(), Uploads.image(file)), this::mediaChanged,
                error -> context.errors().report(context.navigator().window(), error)));
    }

    /**
     * EDIT-6: removes a screenshot.
     *
     * @param screenshotId the screenshot's media id
     */
    private void removeScreenshot(ObjectId screenshotId) {
        context.tasks().run(screenshotsPane, () -> context.services().publishing().removeScreenshot(
                context.session().userId(), game.getId(), screenshotId), this::mediaChanged);
    }

    /** EDIT-4: adds the typed tag as a chip. */
    @FXML
    private void onAddTag() {
        String tag = tagField.getText() == null ? "" : tagField.getText().strip().toLowerCase(Locale.ROOT);
        if (tag.isEmpty()) {
            return;
        }
        if (tag.length() > Game.MAX_TAG_LENGTH || (tags.size() >= Game.MAX_TAGS && !tags.contains(tag))) {
            form.showMessage(ModelKeys.GAME_TAGS, false);
            return;
        }
        if (!tags.contains(tag)) {
            tags.add(tag);
            renderTags();
        }
        tagField.clear();
    }

    /** EDIT-3: adds an achievement and starts editing its name. */
    @FXML
    private void onAddAchievement() {
        if (achievements.size() >= Game.MAX_ACHIEVEMENTS) {
            form.showMessage(ModelKeys.GAME_ACHIEVEMENTS, false);
            return;
        }
        AchievementRow row = new AchievementRow(I18n.text("editor.newAchievement"),
                I18n.text("editor.newAchievementDescription"));
        achievements.add(row);
        achievementsTable.getSelectionModel().select(row);
        achievementsTable.layout();
        achievementsTable.edit(achievements.indexOf(row), achievementNameColumn);
    }

    /** EDIT-3: removes the selected achievement. */
    @FXML
    private void onRemoveAchievement() {
        AchievementRow row = achievementsTable.getSelectionModel().getSelectedItem();
        if (row != null) {
            achievements.remove(row);
        }
    }

    /**
     * EDIT-3: keeps a committed achievement text when its length is within the model's limits.
     *
     * @param event  the cell's commit
     * @param max    the most characters allowed
     * @param setter where the text goes
     */
    private void onAchievementCommit(TableColumn.CellEditEvent<AchievementRow, String> event, int max,
                                     BiConsumer<AchievementRow, String> setter) {
        String text = event.getNewValue() == null ? "" : event.getNewValue().strip();
        if (text.isEmpty() || text.length() > max) {
            // R2: out of range; the cell shows the old text again.
            achievementsTable.refresh();
            form.showMessage(ModelKeys.ACHIEVEMENT, false);
            return;
        }
        setter.accept(event.getRowValue(), text);
        form.clear();
    }

    /** EDIT-9: goes back to the dashboard. */
    @FXML
    private void onBack() {
        context.navigator().show(View.STUDIO);
    }

    /**
     * Runs a change; a rejection is marked on the form, a database failure offers to retry.
     *
     * @param busy    the control to disable while it runs
     * @param work    the service call
     * @param onSaved what to do with the saved game
     */
    private void save(Node busy, UiTasks.Work<Game> work, Consumer<Game> onSaved) {
        context.tasks().run(busy, work, onSaved, error -> {
            if (error instanceof ValidationException refused && ModelKeys.GAME_INCOMPLETE.equals(refused.getMessage())) {
                // EDIT-7.R2: mark what the game still needs.
                readyToPublish();
                return;
            }
            if (error instanceof MistException refused && form.showServiceError(refused)) {
                return;
            }
            context.errors().report(context.navigator().window(), error, () -> save(busy, work, onSaved));
        });
    }

    /**
     * Marks what a game needs before it can be published: a cover, a description, a release date, a PEGI rating and
     * the requirements.
     *
     * @return {@code true} if nothing is missing
     */
    private boolean readyToPublish() {
        boolean fields = publishCheck.validate();
        boolean cover = game.getCoverId() != null;
        coverFrame.pseudoClassStateChanged(ERROR, !cover);
        if (fields && cover) {
            return true;
        }
        form.showMessage(ModelKeys.GAME_INCOMPLETE, false);
        return false;
    }

    /**
     * Shows a game whose cover or screenshots changed, keeping the unsaved fields of the form.
     *
     * @param saved the game as saved
     */
    private void mediaChanged(Game saved) {
        game = saved;
        renderMedia();
        coverFrame.pseudoClassStateChanged(ERROR, false);
    }

    /** Shows the cover and the screenshots, each screenshot with a Remove context menu. */
    private void renderMedia() {
        coverView.setImage(null);
        context.images().load(game.getCoverId(), coverView.getFitWidth() * 2, coverView.getFitHeight() * 2,
                coverView::setImage);
        screenshotsPane.getChildren().clear();
        for (ObjectId id : game.getScreenshotIds()) {
            ImageView shot = new ImageView();
            shot.setFitWidth(SHOT_WIDTH);
            shot.setFitHeight(SHOT_HEIGHT);
            shot.setPreserveRatio(true);
            context.images().load(id, SHOT_WIDTH * 2, SHOT_HEIGHT * 2, shot::setImage);
            MenuItem remove = new MenuItem(I18n.text("editor.removeScreenshot"));
            remove.setId("removeScreenshotItem");
            remove.setOnAction(event -> removeScreenshot(id));
            ContextMenu menu = new ContextMenu(remove);
            shot.setOnContextMenuRequested(event -> menu.show(shot, event.getScreenX(), event.getScreenY()));
            shot.getProperties().put("mist.menu", menu);
            screenshotsPane.getChildren().add(shot);
        }
        addScreenshotButton.setDisable(game.getScreenshotIds().size() >= Game.MAX_SCREENSHOTS);
    }

    /** Shows the game's status and enables the actions that fit it. */
    private void renderStatus() {
        statusLabel.setText(Formats.label(game.getStatus()));
        statusLabel.getStyleClass().removeIf(style -> style.startsWith("mist-status-"));
        statusLabel.getStyleClass().add("mist-status-" + game.getStatus().name().toLowerCase(Locale.ROOT));
        publishButton.setDisable(game.getStatus() != GameStatus.DRAFT);
        delistButton.setDisable(game.getStatus() != GameStatus.PUBLISHED);
    }

    /** Shows the tags as chips, each with a cross that removes it. */
    private void renderTags() {
        tagsPane.getChildren().clear();
        for (String tag : tags) {
            Label name = new Label(tag);
            Button remove = new Button("×");
            remove.setId("removeTag-" + tag);
            remove.getStyleClass().add("mist-chip-remove");
            remove.setOnAction(event -> {
                tags.remove(tag);
                renderTags();
            });
            HBox chip = new HBox(4, name, remove);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.getStyleClass().add("mist-chip");
            tagsPane.getChildren().add(chip);
        }
    }

    /**
     * Shows a check box per language, named in the UI language, checked for those the game has.
     *
     * @param chosen the game's languages
     */
    private void renderLanguages(List<String> chosen) {
        Set<String> codes = new LinkedHashSet<>(LANGUAGES);
        codes.addAll(chosen);
        languagesPane.getChildren().clear();
        for (String code : codes) {
            CheckBox box = new CheckBox(Locale.of(code).getDisplayLanguage(I18n.locale()));
            box.setId("lang-" + code);
            box.setUserData(code);
            box.setSelected(chosen.contains(code));
            languagesPane.getChildren().add(box);
        }
    }

    /**
     * Reads the whole form into the values the service saves.
     *
     * @return the game's values
     */
    private GameForm formValues() {
        boolean noRequirements = requirementFields().stream().allMatch(field -> field.getText().isBlank());
        RequirementsForm requirements = noRequirements ? null : new RequirementsForm(osField.getText(),
                processorField.getText(), memoryField.getText(), graphicsField.getText(), storageField.getText());
        List<String> languages = languagesPane.getChildren().stream()
                .filter(node -> node instanceof CheckBox box && box.isSelected())
                .map(node -> (String) node.getUserData()).toList();
        List<AchievementForm> achievementForms = achievements.stream()
                .map(row -> new AchievementForm(row.getName(), row.getDescription())).toList();
        return new GameForm(nameField.getText(), descriptionArea.getText(), GameForms.parsePrice(priceField.getText()),
                releasePicker.getValue(), pegiBox.getValue(), requirements, new ArrayList<>(tags), languages,
                achievementForms);
    }

    /**
     * Tells whether the requirements are all filled in or all blank, as the model needs.
     *
     * @return {@code true} if they are whole or empty
     */
    private boolean requirementsWholeOrEmpty() {
        long filled = requirementFields().stream().filter(field -> !field.getText().isBlank()).count();
        return filled == 0 || filled == requirementFields().size();
    }

    /** Shows how long the description is. */
    private void updateCounter() {
        int length = descriptionArea.getText() == null ? 0 : descriptionArea.getText().length();
        descriptionCounter.setText(I18n.format("review.counter", length, Game.MAX_DESCRIPTION_LENGTH));
    }

    /**
     * Returns the five requirement fields, in order.
     *
     * @return the fields
     */
    private List<TextField> requirementFields() {
        return Stream.of(osField, processorField, memoryField, graphicsField, storageField).toList();
    }

    /**
     * Returns the decimal separator of the UI language, to show prices as the user types them.
     *
     * @return {@code ,} or {@code .}
     */
    private static char decimalSeparator() {
        return "es".equals(I18n.locale().getLanguage()) ? ',' : '.';
    }
}
