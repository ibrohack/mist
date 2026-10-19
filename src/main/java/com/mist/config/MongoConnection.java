package com.mist.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import java.util.concurrent.TimeUnit;

/**
 * Owns the single {@link MongoClient} that MIST shares between Hibernate and the direct driver calls (schema setup,
 * special indexes, the connection check).
 *
 * <p>Close it when the app stops, after closing the {@code SessionFactory} built on it.</p>
 */
public final class MongoConnection implements AutoCloseable {

    /** Short enough for the splash to report an unreachable server quickly. */
    private static final long SERVER_SELECTION_TIMEOUT_SECONDS = 5;

    private final MongoClient client;
    private final String databaseName;

    /**
     * Creates the shared client from the configured connection string.
     *
     * @param config the settings holding the connection string and database name
     */
    public MongoConnection(AppConfig config) {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(config.mongoUri()))
                .applyToClusterSettings(cluster ->
                        cluster.serverSelectionTimeout(SERVER_SELECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .build();
        this.client = MongoClients.create(settings);
        this.databaseName = config.mongoDatabase();
    }

    /**
     * Returns the shared client.
     *
     * @return the client, open until {@link #close()}
     */
    public MongoClient client() {
        return client;
    }

    /**
     * Returns the name of the database MIST uses.
     *
     * @return the database name
     */
    public String databaseName() {
        return databaseName;
    }

    /** Closes the shared client and its connection pool. */
    @Override
    public void close() {
        client.close();
    }
}
