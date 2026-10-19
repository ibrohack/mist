package com.mist.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks where MIST's settings come from: {@code mist.properties} first, then overrides.
 */
class AppConfigTest {

    @Test
    void loadsClasspathDefaults() {
        AppConfig config = AppConfig.load();

        assertEquals("mist", config.mongoDatabase());
        assertEquals("mongodb://localhost:27017/?replicaSet=rs0", config.mongoUri());
    }

    @Test
    void systemPropertyWins() {
        System.setProperty("mist.mongo.database", "mist_override");
        try {
            assertEquals("mist_override", AppConfig.load().mongoDatabase());
        } finally {
            System.clearProperty("mist.mongo.database");
        }
    }
}
