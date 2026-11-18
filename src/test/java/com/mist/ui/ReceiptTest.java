package com.mist.ui;

import com.mist.service.DemoData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The purchase receipt offered after a checkout (CART-6), written as a PDF by JasperReports.
 */
class ReceiptTest extends MistUiTest {

    /** Where the receipts are written. */
    @TempDir
    Path folder;

    @Test
    void theReceiptIsWrittenAsAPdf() throws Exception {
        buy();
        Path target = folder.resolve("receipt.pdf");
        files.answerSave(target);

        click("dialogAction");

        assertTrue(Files.exists(target));
        assertEquals("%PDF", new String(Files.readAllBytes(target), 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
        assertEquals(I18n.format("report.saved", target), dialogMessage());
    }

    @Test
    void anUnwritableTargetIsReported() throws Exception {
        buy();
        files.answerSave(folder.resolve("no-such-folder").resolve("receipt.pdf"));

        click("dialogAction");

        assertEquals(msg("error.reportNotSaved"), dialogMessage());
    }

    @Test
    void closingTheDialogOrCancellingTheChooserWritesNothing() throws Exception {
        buy();
        click("dialogAction");
        assertFalse(dialogShowing());

        try (var listing = Files.list(folder)) {
            assertEquals(0, listing.count());
        }
    }

    /**
     * Logs in as the demo player, opens the cart and checks out.
     *
     * @throws Exception if the login fails
     */
    private void buy() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        click("cartLink");
        click("termsCheck");
        click("checkoutButton");
        click("dialogConfirm");
    }
}
