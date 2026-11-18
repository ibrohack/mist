package com.mist.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Startup with MongoDB stopped: the splash shows the offline state (SPLASH-1.R2), whose Retry connects again
 * (OFFLINE-1) and whose "?" opens the help (HELP-1).
 */
class OfflineTest extends MistUiTest {

    @Override
    protected void prepare() throws Exception {
        super.prepare();
        db.goOffline();
    }

    @Test
    void splashShowsTheOfflineState() {
        assertEquals(View.OFFLINE, navigator.current());
        assertEquals(msg("offline.title"), text("offlineTitle"));
        assertFalse(visible("retryStatus"));
    }

    @Test
    void retryWhileStillOfflineSaysSo() {
        click("retryButton");

        assertEquals(View.OFFLINE, navigator.current());
        assertTrue(visible("retryStatus"));
        assertFalse(context.isConnected());
    }

    @Test
    void retryOnceMongoIsBackOpensLogin() {
        db.goOnline();

        click("retryButton");

        assertEquals(View.LOGIN, navigator.current());
        assertTrue(context.isConnected());
    }

    @Test
    void theQuestionMarkOpensTheOfflineHelp() {
        click("helpButton");

        assertTrue(navigator.isHelpShowing());
        assertEquals(msg("help.offline.title"), text("topicTitle"));
    }
}
