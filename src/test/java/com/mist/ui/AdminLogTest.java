package com.mist.ui;

import com.mist.service.DemoData;
import com.mist.service.LogEntry;
import com.mist.service.ServiceKeys;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.junit.jupiter.api.Test;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The admins' operation log viewer (LOG-1 to LOG-5) and who can open it (SHELL-6).
 */
class AdminLogTest extends MistUiTest {

    /** Yesterday's log, rolled at midnight. */
    private static final List<String> YESTERDAY = List.of(
            "2026-11-17 09:00:00 | user=anonymous | FIND | users | email=lucia@mist.com | OK",
            "2026-11-17 09:00:01 | user=lucia@mist.com | UPDATE | users | _id=1 | OK",
            "2026-11-17 22:10:00 | user=marc@mist.com | FIND | games | status=PUBLISHED | OK");
    /** Today's log, still in the current file. */
    private static final List<String> TODAY = List.of(
            "2026-11-18 10:00:00 | user=lucia@mist.com | INSERT | orders | playerId=1 | OK",
            "2026-11-18 10:00:01 | user=lucia@mist.com | UPDATE | games | _id=2, status=PUBLISHED | OK",
            "2026-11-18 10:05:00 | user=marc@mist.com | INSERT | reviews | gameId=2 | FAILED",
            "2026-11-18 10:05:01 | user=marc@mist.com | ROLLBACK | - | DuplicateKey: E11000 | FAILED",
            "this line is not a log entry");

    @Test
    void adminLandsOnTheNewestDayNewestFirst() throws Exception {
        writeLogs();

        loginAs(DemoData.ADMIN_EMAIL);

        assertEquals(View.ADMIN_LOG, navigator.current());
        assertEquals(LocalDate.of(2026, 11, 18), this.<javafx.scene.control.DatePicker>node("dayPicker").getValue());
        assertEquals(4, table().getItems().size());
        assertEquals("ROLLBACK", table().getItems().getFirst().operation());
        assertEquals(I18n.format("log.count", 4, 4), text("countLabel"));
    }

    @Test
    void filtersNarrowTheTable() throws Exception {
        writeLogs();
        loginAs(DemoData.ADMIN_EMAIL);

        type("userFilter", "MARC");
        assertEquals(2, table().getItems().size());
        assertEquals(I18n.format("log.count", 2, 4), text("countLabel"));

        type("userFilter", "");
        click("failedOnly");
        assertEquals(2, table().getItems().size());
        assertTrue(table().getItems().stream().noneMatch(LogEntry::ok));

        click("failedOnly");
        choose("operationFilter", "INSERT");
        assertEquals(2, table().getItems().size());
    }

    @Test
    void anotherDayIsRead() throws Exception {
        writeLogs();
        loginAs(DemoData.ADMIN_EMAIL);

        choose("dayPicker", LocalDate.of(2026, 11, 17));

        assertEquals(3, table().getItems().size());
    }

    @Test
    void contextMenuFiltersByTheSelectedRow() throws Exception {
        writeLogs();
        loginAs(DemoData.ADMIN_EMAIL);
        robot.interact(() -> table().getSelectionModel().select(0));

        contextItem("filterUserItem");
        assertEquals("marc@mist.com", text("userFilter"));

        contextItem("filterOperationItem");
        assertEquals(1, table().getItems().size());
    }

    @Test
    void refreshReadsNewLines() throws Exception {
        writeLogs();
        loginAs(DemoData.ADMIN_EMAIL);
        Files.writeString(backend.logDirectory().resolve("mist-db.log"),
                "2026-11-18 11:00:00 | user=nora@mist.com | FIND | games | limit=20 | OK\n",
                java.nio.file.StandardOpenOption.APPEND);

        click("refreshButton");

        assertEquals(5, table().getItems().size());
    }

    @Test
    void emptyLogSaysSo() throws Exception {
        loginAs(DemoData.ADMIN_EMAIL);

        assertTrue(table().getItems().isEmpty());
        assertEquals(msg("log.empty"), text("emptyLabel"));
    }

    @Test
    void unreadableLogIsReported() throws Exception {
        writeLogs();
        Path current = backend.logDirectory().resolve("mist-db.log");
        try (RandomAccessFile file = new RandomAccessFile(current.toFile(), "rw");
             FileChannel channel = file.getChannel();
             FileLock lock = channel.lock()) {
            loginAs(DemoData.ADMIN_EMAIL);

            assertTrue(lock.isValid());
            assertEquals(msg(ServiceKeys.LOG_UNREADABLE), dialogMessage());
        }
    }

    @Test
    void onlyAdminsSeeTheMenuItem() throws Exception {
        loginAs(DemoData.ADMIN_EMAIL);
        assertTrue(menuItem("userMenu", "logItem").orElseThrow().isVisible());
        shortcut(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN));
        assertEquals(View.ADMIN_LOG, navigator.current());

        menu("userMenu", "logoutItem");
        loginAs(DemoData.PLAYER_EMAIL);
        assertFalse(menuItem("userMenu", "logItem").orElseThrow().isVisible());
    }

    @Test
    void theServiceRefusesNonAdminsEvenIfTheScreenOpens() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        robot.interact(() -> navigator.show(View.ADMIN_LOG));

        assertEquals(msg(ServiceKeys.USER_NOT_ADMIN), dialogMessage());
    }

    /**
     * Writes yesterday's rolled log and today's current log into the test's log folder.
     *
     * @throws Exception if writing fails
     */
    private void writeLogs() throws Exception {
        Files.write(backend.logDirectory().resolve("mist-db.2026-11-17.log"), YESTERDAY);
        Files.write(backend.logDirectory().resolve("mist-db.log"), TODAY);
    }

    /**
     * Returns the log table.
     *
     * @return the table
     */
    private TableView<LogEntry> table() {
        return node("logTable");
    }

    /**
     * Fires an item of the table's context menu.
     *
     * @param id the item's fx:id
     */
    private void contextItem(String id) {
        ContextMenu menu = table().getContextMenu();
        MenuItem item = menu.getItems().stream().filter(i -> id.equals(i.getId())).findFirst().orElseThrow();
        robot.interact(item::fire);
    }
}
