package com.mist.ui.controller;

import com.mist.exception.DuplicateEntityException;
import com.mist.exception.UnauthorizedException;
import com.mist.exception.ValidationException;
import com.mist.model.Game;
import com.mist.model.Review;
import com.mist.ui.I18n;
import com.mist.ui.SceneRouter;
import com.mist.ui.UiContext;
import com.mist.ui.support.Dialogs;
import com.mist.ui.support.FormValidator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * The review dialog: the player rates an owned game from one to five stars and may add a text.
 *
 * <p>Controller of {@code review.fxml}, shown in a themed dialog by {@link #show(UiContext, Game, Runnable)}.
 * Implements the behaviors REVIEW-1 to REVIEW-4 of {@code docs/ui-design.md}. Publish keeps the dialog open until
 * the service accepts the review, so a refusal is shown inside it.</p>
 */
public class ReviewDialog {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");
    /** The dialog's view. */
    private static final String VIEW = "view/review.fxml";

    /** What the controller works with. */
    private final UiContext context;
    /** The game being reviewed. */
    private Game game;
    /** The chosen rating, 0 until a star is clicked. */
    private int rating;
    /** What runs once the review is published. */
    private Runnable onPublished;
    /** Shows the dialog's messages. */
    private FormValidator form;
    /** The dialog this controller fills. */
    private Dialog<ButtonType> dialog;

    /** The game's name. */
    @FXML
    private Label gameNameLabel;
    /** One star. */
    @FXML
    private ToggleButton star1;
    /** Two stars. */
    @FXML
    private ToggleButton star2;
    /** Three stars. */
    @FXML
    private ToggleButton star3;
    /** Four stars. */
    @FXML
    private ToggleButton star4;
    /** Five stars. */
    @FXML
    private ToggleButton star5;
    /** The optional text. */
    @FXML
    private TextArea commentArea;
    /** How many characters the text has, of how many. */
    @FXML
    private Label commentCounter;
    /** The result of the last attempt. */
    @FXML
    private Label formMessage;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public ReviewDialog(UiContext context) {
        this.context = context;
    }

    /**
     * REVIEW-1: opens the review dialog for a game the player owns.
     *
     * @param context     what the dialog works with
     * @param game        the game to review
     * @param onPublished what runs once the review is published, on the JavaFX thread
     */
    public static void show(UiContext context, Game game, Runnable onPublished) {
        SceneRouter.Loaded loaded;
        try {
            loaded = SceneRouter.load(VIEW, context);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load " + VIEW, e);
        }
        ReviewDialog controller = (ReviewDialog) loaded.controller();
        controller.open(game, loaded.root(), onPublished);
    }

    /** Limits the text to the model's length and keeps the counter up to date. */
    @FXML
    private void initialize() {
        form = new FormValidator(formMessage);
        // REVIEW-3: refuse a text longer than the model allows.
        commentArea.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= Review.MAX_COMMENT_LENGTH ? change : null));
        commentArea.textProperty().addListener((observable, before, after) -> updateCounter());
        updateCounter();
    }

    /**
     * Builds and shows the dialog around the loaded view.
     *
     * @param reviewed  the game to review
     * @param content   the loaded view
     * @param published what runs once the review is published
     */
    private void open(Game reviewed, Node content, Runnable published) {
        this.game = reviewed;
        this.onPublished = published;
        gameNameLabel.setText(I18n.format("review.title", reviewed.getName()));
        dialog = new Dialog<>();
        dialog.initOwner(context.navigator().window());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(I18n.text("review.dialogTitle"));
        DialogPane pane = dialog.getDialogPane();
        pane.setContent(content);
        Dialogs.style(pane, context.navigator().window());
        ButtonType publish = new ButtonType(I18n.text("review.publish"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(I18n.text("dialog.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().setAll(publish, cancel);
        Node publishButton = pane.lookupButton(publish);
        publishButton.setId("dialogConfirm");
        Node cancelButton = pane.lookupButton(cancel);
        cancelButton.setId("dialogCancel");
        cancelButton.getStyleClass().add("mist-button-secondary");
        // REVIEW-4: Publish keeps the dialog open until the service accepts the review.
        publishButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            onPublish();
        });
        dialog.show();
    }

    /**
     * REVIEW-2: lights the clicked star and those before it.
     *
     * @param event the action of a star
     */
    @FXML
    private void onStar(ActionEvent event) {
        rating = stars().indexOf((ToggleButton) event.getSource()) + 1;
        List<ToggleButton> stars = stars();
        for (int i = 0; i < stars.size(); i++) {
            stars.get(i).setSelected(i < rating);
        }
        form.clear();
    }

    /** REVIEW-4: publishes the review. */
    private void onPublish() {
        if (rating == 0) {
            form.showMessage("review.ratingRequired", false);
            return;
        }
        String comment = commentArea.getText();
        int stars = rating;
        LOG.info("Publishing a {}-star review of {}", stars, game.getName());
        context.tasks().run(commentArea, () -> context.services().reviews().rate(context.session().userId(),
                game.getId(), stars, comment), review -> {
                    // R1: close and let the screen show the new review.
                    dialog.close();
                    onPublished.run();
                }, error -> {
                    if (error instanceof DuplicateEntityException || error instanceof UnauthorizedException
                            || error instanceof ValidationException) {
                        // R2: an existing review, a game not owned or a broken rule shows inside the dialog.
                        form.showMessage(error.getMessage(), false);
                    } else {
                        // R3: a database failure offers to retry.
                        context.errors().report(dialog.getDialogPane().getScene().getWindow(), error,
                                this::onPublish);
                    }
                });
    }

    /** Shows how many characters the text has. */
    private void updateCounter() {
        int length = commentArea.getText() == null ? 0 : commentArea.getText().length();
        commentCounter.setText(I18n.format("review.counter", length, Review.MAX_COMMENT_LENGTH));
    }

    /**
     * Returns the star buttons, from one to five.
     *
     * @return the stars
     */
    private List<ToggleButton> stars() {
        return List.of(star1, star2, star3, star4, star5);
    }

    /** HELP-1: opens the help on the screen the dialog was opened from (library or game page). */
    @FXML
    private void onHelp() {
        context.navigator().showHelp();
    }
}
