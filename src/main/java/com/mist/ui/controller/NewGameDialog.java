package com.mist.ui.controller;

import com.mist.exception.MistException;
import com.mist.model.Game;
import com.mist.model.ModelKeys;
import com.mist.ui.I18n;
import com.mist.ui.SceneRouter;
import com.mist.ui.UiContext;
import com.mist.ui.support.Dialogs;
import com.mist.ui.support.FormValidator;
import com.mist.ui.support.GameForms;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * The New game dialog of the dashboard: a developer names a draft and gives its price (behavior STUDIO-3 of
 * {@code docs/ui-design.md}).
 *
 * <p>Controller of {@code new-game.fxml}. Create keeps the dialog open until the service accepts the draft.</p>
 */
public class NewGameDialog {

    /** The dialog's view. */
    private static final String VIEW = "view/new-game.fxml";

    /** What the controller works with. */
    private final UiContext context;
    /** Checks the name and price. */
    private FormValidator form;
    /** The dialog this controller fills. */
    private Dialog<ButtonType> dialog;
    /** What runs with the created draft. */
    private Consumer<Game> onCreated;

    /** The draft's name. */
    @FXML
    private TextField nameField;
    /** The draft's price. */
    @FXML
    private TextField priceField;
    /** The first error, or why the service refused. */
    @FXML
    private Label formMessage;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public NewGameDialog(UiContext context) {
        this.context = context;
    }

    /**
     * Opens the dialog.
     *
     * @param context   what the dialog works with
     * @param onCreated receives the created draft, on the JavaFX thread
     */
    public static void show(UiContext context, Consumer<Game> onCreated) {
        SceneRouter.Loaded loaded;
        try {
            loaded = SceneRouter.load(VIEW, context);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load " + VIEW, e);
        }
        ((NewGameDialog) loaded.controller()).open(loaded.root(), onCreated);
    }

    /** Declares the checks of the name and the price. */
    @FXML
    private void initialize() {
        form = new FormValidator(formMessage)
                .required(nameField).maxLength(nameField, Game.MAX_NAME_LENGTH, ModelKeys.GAME_NAME)
                .required(priceField)
                .rule(priceField, () -> GameForms.parsePrice(priceField.getText()) != null, ModelKeys.GAME_PRICE)
                .bind(ModelKeys.GAME_NAME, nameField).bind(ModelKeys.GAME_PRICE, priceField);
    }

    /**
     * Builds and shows the dialog around the loaded view.
     *
     * @param content the loaded view
     * @param created what runs with the created draft
     */
    private void open(Node content, Consumer<Game> created) {
        this.onCreated = created;
        dialog = new Dialog<>();
        dialog.initOwner(context.navigator().window());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(I18n.text("studio.newGame"));
        DialogPane pane = dialog.getDialogPane();
        pane.setContent(content);
        Dialogs.style(pane, context.navigator().window());
        ButtonType create = new ButtonType(I18n.text("studio.create"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(I18n.text("dialog.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().setAll(create, cancel);
        Node createButton = pane.lookupButton(create);
        createButton.setId("dialogConfirm");
        Node cancelButton = pane.lookupButton(cancel);
        cancelButton.setId("dialogCancel");
        cancelButton.getStyleClass().add("mist-button-secondary");
        createButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            onCreate();
        });
        dialog.show();
    }

    /** STUDIO-3: creates the draft. */
    private void onCreate() {
        if (!form.validate()) {
            return;
        }
        String name = nameField.getText();
        BigDecimal price = GameForms.parsePrice(priceField.getText());
        context.tasks().run(nameField, () -> context.services().publishing().createDraft(context.session().userId(),
                name, price), draft -> {
                    // R1: close and open the draft.
                    dialog.close();
                    onCreated.accept(draft);
                }, error -> {
                    // R2: a rejected name or price shows inside the dialog.
                    if (!(error instanceof MistException refused && form.showServiceError(refused))) {
                        context.errors().report(dialog.getDialogPane().getScene().getWindow(), error, this::onCreate);
                    }
                });
    }
}
