package com.mist.ui;

import com.mist.exception.DataAccessException;

import java.util.Objects;

/**
 * Everything a controller needs, handed to its constructor by {@link SceneRouter}: the services, the session, the
 * navigator and the file picker.
 *
 * <p>The services exist only after {@link #connect()} succeeds, which the splash does first; until then
 * {@link #services()} fails.</p>
 */
public final class UiContext {

    /** Where the services come from. */
    private final Backend backend;
    /** The file dialogs. */
    private final FilePicker files;
    /** Who is using the app. */
    private final UserSession session = new UserSession();
    /** The navigator of the main window, attached once it exists. */
    private Navigator navigator;
    /** The services, once connected; written by a worker thread, read by the JavaFX thread. */
    private volatile AppServices services;

    /**
     * Creates the context of the main window.
     *
     * @param backend where the services come from
     * @param files   the file dialogs
     */
    public UiContext(Backend backend, FilePicker files) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.files = Objects.requireNonNull(files, "files");
    }

    /**
     * Attaches the navigator of the main window; {@link Navigator}'s constructor calls it.
     *
     * @param navigator the navigator
     * @throws IllegalStateException if a navigator is already attached
     */
    void attach(Navigator navigator) {
        if (this.navigator != null) {
            throw new IllegalStateException("A navigator is already attached");
        }
        this.navigator = Objects.requireNonNull(navigator, "navigator");
    }

    /**
     * Connects to the database through the backend and keeps the services. Runs on a worker thread.
     *
     * @return the services
     * @throws DataAccessException if the database cannot be reached
     */
    public AppServices connect() throws DataAccessException {
        AppServices connected = backend.connect();
        this.services = connected;
        return connected;
    }

    /**
     * Tells whether the services are available.
     *
     * @return {@code true} after a successful {@link #connect()}
     */
    public boolean isConnected() {
        return services != null;
    }

    /**
     * Returns the services.
     *
     * @return the services
     * @throws IllegalStateException before the first successful {@link #connect()}
     */
    public AppServices services() {
        AppServices connected = services;
        if (connected == null) {
            throw new IllegalStateException("MIST is not connected to the database yet");
        }
        return connected;
    }

    /**
     * Returns who is using the app.
     *
     * @return the session
     */
    public UserSession session() {
        return session;
    }

    /**
     * Returns the navigator of the main window.
     *
     * @return the navigator
     * @throws IllegalStateException if no navigator is attached yet
     */
    public Navigator navigator() {
        if (navigator == null) {
            throw new IllegalStateException("No navigator is attached");
        }
        return navigator;
    }

    /**
     * Returns the file dialogs.
     *
     * @return the file picker
     */
    public FilePicker files() {
        return files;
    }

    /** Releases the database connection, when the app stops. */
    public void close() {
        backend.close();
    }
}
