package com.mist.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * MIST's settings: the MongoDB connection string and database name.
 *
 * <p>{@link #load()} reads the defaults from {@code mist.properties} on the classpath. An environment variable named
 * after the key ({@code MIST_MONGO_URI} for {@code mist.mongo.uri}) overrides a default, and a system property with
 * the key itself ({@code -Dmist.mongo.uri=...}) overrides both.</p>
 */
public final class AppConfig {

    private static final String RESOURCE = "/mist.properties";
    private static final String MONGO_URI = "mist.mongo.uri";
    private static final String MONGO_DATABASE = "mist.mongo.database";
    private static final List<String> KEYS = List.of(MONGO_URI, MONGO_DATABASE);

    private final String mongoUri;
    private final String mongoDatabase;

    private AppConfig(String mongoUri, String mongoDatabase) {
        this.mongoUri = mongoUri;
        this.mongoDatabase = mongoDatabase;
    }

    /**
     * Loads the settings from {@code mist.properties}, then applies environment variable and system property
     * overrides.
     *
     * @return the effective settings
     * @throws IllegalStateException if {@code mist.properties} is missing or a setting is blank
     */
    public static AppConfig load() {
        Properties properties = readDefaults();
        for (String key : KEYS) {
            override(properties, key, System.getenv(key.toUpperCase(Locale.ROOT).replace('.', '_')));
        }
        for (String key : KEYS) {
            override(properties, key, System.getProperty(key));
        }
        return from(properties);
    }

    /**
     * Builds the settings from exactly the given properties, without overrides.
     *
     * @param properties properties holding {@code mist.mongo.uri} and {@code mist.mongo.database}
     * @return the settings
     * @throws IllegalStateException if a setting is missing or blank
     */
    public static AppConfig from(Properties properties) {
        return new AppConfig(require(properties, MONGO_URI), require(properties, MONGO_DATABASE));
    }

    /**
     * Returns the MongoDB connection string.
     *
     * @return the connection string, such as {@code mongodb://localhost:27017/?replicaSet=rs0}
     */
    public String mongoUri() {
        return mongoUri;
    }

    /**
     * Returns the name of the database MIST stores its collections in.
     *
     * @return the database name
     */
    public String mongoDatabase() {
        return mongoDatabase;
    }

    private static Properties readDefaults() {
        Properties properties = new Properties();
        try (InputStream in = AppConfig.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(RESOURCE + " is missing from the classpath");
            }
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + RESOURCE, e);
        }
        return properties;
    }

    private static void override(Properties properties, String key, String value) {
        if (value != null && !value.isBlank()) {
            properties.setProperty(key, value);
        }
    }

    private static String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing setting " + key);
        }
        return value.strip();
    }
}
