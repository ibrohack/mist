package com.mist.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Startup with MongoDB running: the splash connects and opens Login (SPLASH-1.R1).
 */
class StartupTest extends MistUiTest {

    @Test
    void splashConnectsAndOpensLogin() {
        assertEquals(View.LOGIN, navigator.current());
        assertTrue(context.isConnected());
    }
}
