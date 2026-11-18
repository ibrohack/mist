package com.mist.ui;

import com.mist.exception.DataAccessException;

/**
 * Where the UI gets its services from. The application connects to MongoDB through it; UI tests replace it with an
 * in-memory backend.
 *
 * <p>The UI layer only knows this interface, so it never depends on the data access layer.</p>
 */
public interface Backend extends AutoCloseable {

    /**
     * Connects to the database, if not connected yet, and returns the services built on it. Calling it again after a
     * success returns services on the same connection.
     *
     * @return the services
     * @throws DataAccessException if the database cannot be reached
     */
    AppServices connect() throws DataAccessException;

    /** Releases the connection. Safe to call when not connected. */
    @Override
    void close();
}
