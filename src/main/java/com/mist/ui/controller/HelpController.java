package com.mist.ui.controller;

import com.mist.model.Role;
import com.mist.ui.HelpPanel;
import com.mist.ui.I18n;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller of the help window: one topic per screen, in the current language.
 *
 * <p>Implements the behaviors HELP-1 to HELP-3 of {@code docs/ui-design.md}. A screen has a topic when the bundles
 * hold {@code help.<topic>.title} and {@code help.<topic>.body}; the body's paragraphs are separated by blank
 * lines. The operation log's topic is listed for admins only.</p>
 */
public class HelpController implements HelpPanel {

    /** The UI's own log: user actions and the errors shown to the user. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");

    /** What the controller works with. */
    private final UiContext context;

    /** The topics, one per screen with help. */
    @FXML
    private ListView<View> topicList;
    /** The chosen topic's title. */
    @FXML
    private Label topicTitle;
    /** The chosen topic's paragraphs. */
    @FXML
    private VBox topicBody;
    /** Closes the window. */
    @FXML
    private Button closeButton;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public HelpController(UiContext context) {
        this.context = context;
    }

    /** Lists the topics the bundles hold and shows the one picked. */
    @FXML
    private void initialize() {
        ResourceBundle bundle = I18n.bundle();
        // The general topic first, then the screens in their order.
        List<View> topics = Arrays.stream(View.values())
                .filter(view -> bundle.containsKey(key(view, "title")))
                .filter(view -> view != View.ADMIN_LOG || context.session().hasRole(Role.ADMIN))
                .sorted(Comparator.comparing(view -> view != View.SHELL))
                .toList();
        topicList.getItems().setAll(topics);
        topicList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(View view, boolean empty) {
                super.updateItem(view, empty);
                setText(empty || view == null ? null : I18n.text(key(view, "title")));
            }
        });
        // HELP-2: show the topic picked in the list.
        topicList.getSelectionModel().selectedItemProperty().addListener((observable, before, after) -> {
            if (after != null) {
                showTopic(after);
            }
        });
    }

    /**
     * HELP-1: selects the topic of a screen, or the general one if that screen has none.
     *
     * @param screen the screen the user was on
     */
    @Override
    public void select(View screen) {
        View topic = topicList.getItems().contains(screen) ? screen : View.SHELL;
        LOG.info("Help opened on the {} topic", topic);
        topicList.getSelectionModel().select(topic);
        topicList.scrollTo(topic);
    }

    /**
     * Returns the topic on display.
     *
     * @return the selected topic
     */
    public View selected() {
        return topicList.getSelectionModel().getSelectedItem();
    }

    /**
     * Shows a topic's title and paragraphs.
     *
     * @param topic the topic
     */
    private void showTopic(View topic) {
        topicTitle.setText(I18n.text(key(topic, "title")));
        topicBody.getChildren().clear();
        for (String paragraph : I18n.text(key(topic, "body")).split("\n\n")) {
            Label text = new Label(paragraph.strip());
            text.setWrapText(true);
            topicBody.getChildren().add(text);
        }
    }

    /** HELP-3: closes the window. */
    @FXML
    private void onClose() {
        LOG.info("Help closed");
        closeButton.getScene().getWindow().hide();
    }

    /**
     * Returns a bundle key of a topic.
     *
     * @param topic the topic's screen
     * @param part  {@code title} or {@code body}
     * @return the key, such as {@code help.login.title}
     */
    private static String key(View topic, String part) {
        return "help." + topic.helpTopic() + "." + part;
    }
}
