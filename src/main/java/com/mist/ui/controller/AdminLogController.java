package com.mist.ui.controller;

import com.mist.service.LogEntry;
import com.mist.ui.I18n;
import com.mist.ui.NavigationAware;
import com.mist.ui.UiContext;
import com.mist.ui.support.Formats;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Controller of the operation log viewer, where admins read and filter the database operation log of one day.
 *
 * <p>Implements the behaviors LOG-1 to LOG-5 of {@code docs/ui-design.md}.</p>
 */
public class AdminLogController implements NavigationAware {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");
    /** The choice of the operation filter that keeps every operation. */
    private static final String ALL_OPERATIONS = "";

    /** What the controller works with. */
    private final UiContext context;
    /** Every entry of the chosen day. */
    private final ObservableList<LogEntry> entries = FXCollections.observableArrayList();
    /** The entries the filters keep. */
    private final FilteredList<LogEntry> filtered = new FilteredList<>(entries);
    /** The days that have a log; the date picker only offers these. */
    private final Set<LocalDate> days = new HashSet<>();

    /** The day to read. */
    @FXML
    private DatePicker dayPicker;
    /** Keeps the entries of users whose email contains this text. */
    @FXML
    private TextField userFilter;
    /** Keeps the entries of one operation. */
    @FXML
    private ComboBox<String> operationFilter;
    /** Keeps only the failed entries. */
    @FXML
    private CheckBox failedOnly;
    /** Reads the log again. */
    @FXML
    private Button refreshButton;
    /** How many entries the filters keep, of how many. */
    @FXML
    private Label countLabel;
    /** The entries. */
    @FXML
    private TableView<LogEntry> logTable;
    /** Says why the table is empty. */
    @FXML
    private Label emptyLabel;
    /** When the operation happened. */
    @FXML
    private TableColumn<LogEntry, LocalDateTime> timeColumn;
    /** Who did it. */
    @FXML
    private TableColumn<LogEntry, String> userColumn;
    /** What was done. */
    @FXML
    private TableColumn<LogEntry, String> operationColumn;
    /** On which collection. */
    @FXML
    private TableColumn<LogEntry, String> collectionColumn;
    /** The ids, filters or failure reason. */
    @FXML
    private TableColumn<LogEntry, String> detailsColumn;
    /** Whether it worked. */
    @FXML
    private TableColumn<LogEntry, Boolean> outcomeColumn;
    /** The context menu of the table's rows. */
    @FXML
    private ContextMenu rowMenu;
    /** Copies the selected row as a log line. */
    @FXML
    private MenuItem copyItem;
    /** Keeps only the selected row's user. */
    @FXML
    private MenuItem filterUserItem;
    /** Keeps only the selected row's operation. */
    @FXML
    private MenuItem filterOperationItem;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public AdminLogController(UiContext context) {
        this.context = context;
    }

    /** Sets up the columns, the sorting, the day picker and the filters. */
    @FXML
    private void initialize() {
        timeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().time()));
        timeColumn.setCellFactory(column -> textCell(Formats::time));
        userColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().user()));
        operationColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().operation()));
        collectionColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().collection()));
        detailsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().details()));
        detailsColumn.setCellFactory(column -> new DetailsCell());
        outcomeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().ok()));
        outcomeColumn.setCellFactory(column -> new OutcomeCell());

        SortedList<LogEntry> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(logTable.comparatorProperty());
        logTable.setItems(sorted);
        timeColumn.setSortType(TableColumn.SortType.DESCENDING);
        logTable.getSortOrder().add(timeColumn);

        dayPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate day, boolean empty) {
                super.updateItem(day, empty);
                setDisable(empty || !days.contains(day));
            }
        });
        dayPicker.valueProperty().addListener((observable, before, after) -> onDayChanged());
        operationFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(String operation) {
                return operation == null || operation.isEmpty() ? I18n.text("log.allOperations") : operation;
            }

            @Override
            public String fromString(String text) {
                return text;
            }
        });
        operationFilter.getItems().setAll(ALL_OPERATIONS);
        operationFilter.setValue(ALL_OPERATIONS);
        userFilter.textProperty().addListener((observable, before, after) -> applyFilters());
        operationFilter.valueProperty().addListener((observable, before, after) -> applyFilters());
        failedOnly.selectedProperty().addListener((observable, before, after) -> applyFilters());
        rowMenu.setOnShowing(event -> {
            boolean none = logTable.getSelectionModel().getSelectedItem() == null;
            rowMenu.getItems().forEach(item -> item.setDisable(none));
        });
        applyFilters();
    }

    @Override
    public void onNavigatedTo(Object parameter) {
        loadDays(null);
    }

    /**
     * LOG-1 and LOG-4: lists the days with a log and selects one.
     *
     * @param keep the day to keep selected if it still has a log, or {@code null} for the newest
     */
    private void loadDays(LocalDate keep) {
        context.tasks().run(refreshButton, () -> context.services().logs().days(context.session().userId()),
                found -> {
                    days.clear();
                    days.addAll(found);
                    if (found.isEmpty()) {
                        // LOG-1.R2: nothing logged yet.
                        entries.clear();
                        emptyLabel.setText(I18n.text("log.empty"));
                        applyFilters();
                        return;
                    }
                    LocalDate day = keep != null && days.contains(keep) ? keep : found.getFirst();
                    if (day.equals(dayPicker.getValue())) {
                        onDayChanged();
                    } else {
                        dayPicker.setValue(day);
                    }
                });
    }

    /** LOG-2: reads the entries of the chosen day. */
    private void onDayChanged() {
        LocalDate day = dayPicker.getValue();
        if (day == null) {
            return;
        }
        LOG.info("Reading the operation log of {}", day);
        context.tasks().run(dayPicker, () -> context.services().logs().entries(context.session().userId(), day),
                found -> {
                    entries.setAll(found);
                    emptyLabel.setText(I18n.text("log.noEntries"));
                    List<String> operations = new ArrayList<>();
                    operations.add(ALL_OPERATIONS);
                    found.stream().map(LogEntry::operation).distinct().sorted().forEach(operations::add);
                    String chosen = operationFilter.getValue();
                    operationFilter.getItems().setAll(operations);
                    operationFilter.setValue(operations.contains(chosen) ? chosen : ALL_OPERATIONS);
                    applyFilters();
                },
                error -> {
                    // R2: an unreadable file empties the table and offers to retry.
                    entries.clear();
                    applyFilters();
                    context.errors().report(context.navigator().window(), error, this::onDayChanged);
                });
    }

    /** LOG-4: reads the log again, keeping the chosen day. */
    @FXML
    private void onRefresh() {
        loadDays(dayPicker.getValue());
    }

    /** LOG-3: keeps the entries that match every filter and updates the count. */
    private void applyFilters() {
        String user = userFilter.getText() == null ? "" : userFilter.getText().strip().toLowerCase(Locale.ROOT);
        String operation = operationFilter.getValue();
        boolean failures = failedOnly.isSelected();
        filtered.setPredicate(entry -> entry.user().toLowerCase(Locale.ROOT).contains(user)
                && (operation == null || operation.isEmpty() || operation.equals(entry.operation()))
                && (!failures || !entry.ok()));
        countLabel.setText(I18n.format("log.count", filtered.size(), entries.size()));
    }

    /** LOG-5: copies the selected row as a log line. */
    @FXML
    private void onCopy() {
        LogEntry entry = logTable.getSelectionModel().getSelectedItem();
        if (entry != null) {
            ClipboardContent content = new ClipboardContent();
            content.putString(entry.line());
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    /** LOG-5: keeps only the selected row's user. */
    @FXML
    private void onFilterUser() {
        LogEntry entry = logTable.getSelectionModel().getSelectedItem();
        if (entry != null) {
            userFilter.setText(entry.user());
        }
    }

    /** LOG-5: keeps only the selected row's operation. */
    @FXML
    private void onFilterOperation() {
        LogEntry entry = logTable.getSelectionModel().getSelectedItem();
        if (entry != null) {
            operationFilter.setValue(entry.operation());
        }
    }

    /**
     * Creates a cell that shows its value through a formatter.
     *
     * @param format the formatter
     * @param <T>    the value type
     * @return the cell
     */
    private static <T> TableCell<LogEntry, T> textCell(Function<T, String> format) {
        return new TableCell<>() {
            @Override
            protected void updateItem(T value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : format.apply(value));
            }
        };
    }

    /** Shows the details on one line, cut with an ellipsis, and the whole text in a tooltip. */
    private static final class DetailsCell extends TableCell<LogEntry, String> {

        /** Creates an empty cell; the table fills it. */
        DetailsCell() {
            // The table calls updateItem.
        }

        @Override
        protected void updateItem(String details, boolean empty) {
            super.updateItem(details, empty);
            setText(empty ? null : details);
            setTooltip(empty || details == null ? null : new Tooltip(details));
        }
    }

    /** Shows the outcome as "OK" in acid or "FAILED" in the danger color. */
    private static final class OutcomeCell extends TableCell<LogEntry, Boolean> {

        /** Creates an empty cell; the table fills it. */
        OutcomeCell() {
            // The table calls updateItem.
        }

        @Override
        protected void updateItem(Boolean ok, boolean empty) {
            super.updateItem(ok, empty);
            getStyleClass().removeAll("mist-outcome-ok", "mist-outcome-failed");
            if (empty || ok == null) {
                setText(null);
                return;
            }
            setText(I18n.text(ok ? "log.ok" : "log.failed"));
            getStyleClass().add(ok ? "mist-outcome-ok" : "mist-outcome-failed");
        }
    }
}
