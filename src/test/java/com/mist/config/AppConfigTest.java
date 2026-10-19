package com.mist.config;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks where MIST's settings come from: {@code mist.properties} first, then environment variables, then system
 * properties.
 */
class AppConfigTest {

    @Test
    void loadsClasspathDefaults() {
        AppConfig config = AppConfig.load(Map.of(), new Properties());

        assertEquals("mist", config.mongoDatabase());
        assertEquals("mongodb://localhost:27017/?replicaSet=rs0", config.mongoUri());
    }

    @Test
    void environmentVariableOverridesDefaults() {
        AppConfig config = AppConfig.load(Map.of("MIST_MONGO_URI", "mongodb://db.example:27017/?replicaSet=rs0"),
                new Properties());

        assertEquals("mongodb://db.example:27017/?replicaSet=rs0", config.mongoUri());
        assertEquals("mist", config.mongoDatabase());
    }

    @Test
    void systemPropertyBeatsEnvironmentVariable() {
        Properties systemProperties = new Properties();
        systemProperties.setProperty("mist.mongo.database", "mist_property");

        AppConfig config = AppConfig.load(Map.of("MIST_MONGO_DATABASE", "mist_env"), systemProperties);

        assertEquals("mist_property", config.mongoDatabase());
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
