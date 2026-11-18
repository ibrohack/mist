package com.mist.ui.support;

import com.mist.ui.I18n;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * Themed dialogs with texts from the bundles.
 *
 * <p>Dialogs never block the JavaFX thread: they are shown with {@code show()} and run a callback when closed. They
 * copy the owner's stylesheets, so they follow the brand theme, light or dark. Their buttons carry MIST's own texts
 * and ids ({@code dialogOk}, {@code dialogCancel}, {@code dialogRetry}, {@code dialogConfirm}) instead of JavaFX's
 * built-in "OK" and "Cancel", which would follow the machine's language.</p>
 */
public final class Dialogs {

    /** Id of the label that holds a dialog's message. */
    public static final String MESSAGE_ID = "dialogMessage";

    /** Not instantiable: a holder of dialog helpers. */
    private Dialogs() {
    }

    /**
     * Shows a warning, such as a rule the user broke.
     *
     * @param owner   the window the dialog belongs to
     * @param message the text to show, already translated
     */
    public static void warning(Window owner, String message) {
        Alert alert = alert(owner, Alert.AlertType.WARNING, "dialog.warning.title", message);
        buttons(alert, new Choice("dialog.ok", ButtonBar.ButtonData.OK_DONE, "dialogOk"));
        alert.show();
    }

    /**
     * Shows information, such as the result of an action.
     *
     * @param owner   the window the dialog belongs to
     * @param message the text to show, already translated
     */
    public static void info(Window owner, String message) {
        Alert alert = alert(owner, Alert.AlertType.INFORMATION, "dialog.info.title", message);
        buttons(alert, new Choice("dialog.ok", ButtonBar.ButtonData.OK_DONE, "dialogOk"));
        alert.show();
    }

    /**
     * Shows an error, with a Retry button when the action can be tried again.
     *
     * @param owner   the window the dialog belongs to
     * @param message the text to show, already translated
     * @param retry   what Retry runs, or {@code null} for no Retry button
     */
    public static void error(Window owner, String message, Runnable retry) {
        Alert alert = alert(owner, Alert.AlertType.ERROR, "dialog.error.title", message);
        if (retry == null) {
            buttons(alert, new Choice("dialog.ok", ButtonBar.ButtonData.OK_DONE, "dialogOk"));
        } else {
            ButtonType again = buttons(alert, new Choice("dialog.retry", ButtonBar.ButtonData.OK_DONE, "dialogRetry"),
                    new Choice("dialog.cancel", ButtonBar.ButtonData.CANCEL_CLOSE, "dialogCancel"));
            alert.setOnHidden(event -> {
                if (alert.getResult() == again) {
                    retry.run();
                }
            });
        }
        alert.show();
    }

    /**
     * Asks the user to confirm an action, such as deactivating the account.
     *
     * @param owner      the window the dialog belongs to
     * @param message    the question, already translated, naming what will happen
     * @param confirmKey the bundle key of the confirming button, such as {@code dialog.confirm}
     * @param onConfirm  what runs if the user confirms
     */
    public static void confirm(Window owner, String message, String confirmKey, Runnable onConfirm) {
        Alert alert = alert(owner, Alert.AlertType.CONFIRMATION, "dialog.confirm.title", message);
        ButtonType yes = buttons(alert, new Choice(confirmKey, ButtonBar.ButtonData.OK_DONE, "dialogConfirm"),
                new Choice("dialog.cancel", ButtonBar.ButtonData.CANCEL_CLOSE, "dialogCancel"));
        alert.setOnHidden(event -> {
            if (alert.getResult() == yes) {
                onConfirm.run();
            }
        });
        alert.show();
    }

    /**
     * Gives a dialog the brand look of its owner: its stylesheets and the {@code mist-dialog} class.
     *
     * @param pane  the dialog's pane
     * @param owner the owner window, or {@code null}
     */
    public static void style(DialogPane pane, Window owner) {
        pane.getStyleClass().add("mist-dialog");
        if (owner != null && owner.getScene() != null) {
            pane.getStylesheets().setAll(owner.getScene().getStylesheets());
        }
    }

    /**
     * Builds a themed, window-modal alert with a message label.
     *
     * @param owner    the owner window
     * @param type     the alert type, which sets the icon
     * @param titleKey the bundle key of the title
     * @param message  the message, already translated
     * @return the alert, not shown yet
     */
    private static Alert alert(Window owner, Alert.AlertType type, String titleKey, String message) {
        Alert alert = new Alert(type);
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
        }
        alert.setTitle(I18n.text(titleKey));
        alert.setHeaderText(null);
        Label text = new Label(message);
        text.setId(MESSAGE_ID);
        text.setWrapText(true);
        text.setMaxWidth(420);
        alert.getDialogPane().setContent(text);
        style(alert.getDialogPane(), owner);
        return alert;
    }

    /**
     * A dialog button to create: its text, its role and the id tests find it by.
     *
     * @param key  the bundle key of the text
     * @param data the button's role, which sets its place in the button bar
     * @param id   the id given to the button node
     */
    private record Choice(String key, ButtonBar.ButtonData data, String id) {
    }

    /**
     * Puts buttons with bundle texts and ids on an alert.
     *
     * @param alert   the alert
     * @param choices the buttons, the first one being the main action
     * @return the button type of the first choice
     */
    private static ButtonType buttons(Alert alert, Choice... choices) {
        ButtonType[] types = new ButtonType[choices.length];
        for (int i = 0; i < choices.length; i++) {
            types[i] = new ButtonType(I18n.text(choices[i].key()), choices[i].data());
        }
        alert.getButtonTypes().setAll(types);
        for (int i = 0; i < choices.length; i++) {
            alert.getDialogPane().lookupButton(types[i]).setId(choices[i].id());
        }
        return types[0];
    }
}
