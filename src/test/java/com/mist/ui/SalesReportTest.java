package com.mist.ui;

import com.mist.service.DemoData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The developer's sales report (STUDIO-7), written as a PDF by JasperReports from the dashboard's figures.
 */
class SalesReportTest extends MistUiTest {

    /** Where the reports are written. */
    @TempDir
    Path folder;

    @Test
    void theDashboardFiguresAreWrittenAsAPdf() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        Path target = folder.resolve("sales.pdf");
        files.answerSave(target);

        click("salesReportButton");

        assertEquals("%PDF", header(target));
        assertEquals(I18n.format("report.saved", target), dialogMessage());
    }

    @Test
    void theReportIsAlsoWrittenInEnglish() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        robot.interact(() -> {
            I18n.use(Locale.ENGLISH);
            navigator.reload();
        });
        Path target = folder.resolve("sales-en.pdf");
        files.answerSave(target);

        click("salesReportButton");

        assertEquals("%PDF", header(target));
        assertEquals(I18n.format("report.saved", target), dialogMessage());
    }

    @Test
    void anUnwritableTargetIsReported() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);
        files.answerSave(folder.resolve("no-such-folder").resolve("sales.pdf"));

        click("salesReportButton");

        assertEquals(msg("error.reportNotSaved"), dialogMessage());
    }

    @Test
    void cancellingTheChooserWritesNothing() throws Exception {
        loginAs(DemoData.DEVELOPER_EMAIL);

        click("salesReportButton");

        assertFalse(dialogShowing());
        try (var listing = Files.list(folder)) {
            assertEquals(0, listing.count());
        }
    }

    /**
     * Reads the first four bytes of a file, which are {@code %PDF} in a PDF.
     *
     * @param file the file
     * @return its first four characters
     * @throws Exception if the file cannot be read
     */
    private static String header(Path file) throws Exception {
        return new String(Files.readAllBytes(file), 0, 4, StandardCharsets.US_ASCII);
    }
}
