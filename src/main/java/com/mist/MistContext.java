package com.mist;

import com.mist.config.AppConfig;
import com.mist.dao.hibernate.HibernateDataAccess;
import com.mist.exception.DataAccessException;
import com.mist.service.AuthService;
import com.mist.service.LibraryService;
import com.mist.service.MediaService;
import com.mist.service.PublishingService;
import com.mist.service.ReviewService;
import com.mist.service.StoreService;
import com.mist.service.UserService;

import java.time.Clock;
import java.util.Objects;

/**
 * Wires MIST together: opens the data access layer and builds every service on it. The UI gets its services from here.
 *
 * <p>Open it once, when the splash checks the database, and close it when the app stops. Opening fails with a
 * {@link DataAccessException} if MongoDB cannot be reached, so the splash can show the offline state.</p>
 */
public final class MistContext implements AutoCloseable {

    private final HibernateDataAccess data;
    private final AuthService auth;
    private final UserService users;
    private final StoreService store;
    private final LibraryService library;
    private final ReviewService reviews;
    private final PublishingService publishing;
    private final MediaService media;

    private MistContext(HibernateDataAccess data, Clock clock) {
        this.data = data;
        this.auth = new AuthService(data.users(), clock);
        this.users = new UserService(data.users(), data.games(), data.media(), data.transactions(), clock);
        this.store = new StoreService(data.games(), data.users(), data.orders(), data.transactions(), clock);
        this.library = new LibraryService(data.users(), data.games(), data.orders(), data.transactions(), clock);
        this.reviews = new ReviewService(data.reviews(), data.users(), data.games(), data.transactions(), clock);
        this.publishing = new PublishingService(data.games(), data.users(), data.media(), data.transactions(), clock);
        this.media = new MediaService(data.media());
    }

    /**
     * Connects to MongoDB with the given settings and builds the services.
     *
     * @param config the connection settings, usually {@code AppConfig.load()}
     * @return the open context
     * @throws DataAccessException if MongoDB cannot be reached or rejects the schema setup
     */
    public static MistContext open(AppConfig config) throws DataAccessException {
        return open(config, Clock.systemUTC());
    }

    /**
     * Connects to MongoDB and builds the services on a given clock, so a check can move time forward.
     *
     * @param config the connection settings
     * @param clock  the clock the services date their changes with
     * @return the open context
     * @throws DataAccessException if MongoDB cannot be reached or rejects the schema setup
     */
    static MistContext open(AppConfig config, Clock clock) throws DataAccessException {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(clock, "clock");
        HibernateDataAccess data = HibernateDataAccess.open(config);
        try {
            return new MistContext(data, clock);
        } catch (RuntimeException e) {
            data.close();
            throw e;
        }
    }

    /**
     * Returns the login and registration service.
     *
     * @return the auth service
     */
    public AuthService auth() {
        return auth;
    }

    /**
     * Returns the profile and account service.
     *
     * @return the user service
     */
    public UserService users() {
        return users;
    }

    /**
     * Returns the catalog, cart, wishlist and checkout service.
     *
     * @return the store service
     */
    public StoreService store() {
        return store;
    }

    /**
     * Returns the library and returns service.
     *
     * @return the library service
     */
    public LibraryService library() {
        return library;
    }

    /**
     * Returns the rating and review service.
     *
     * @return the review service
     */
    public ReviewService reviews() {
        return reviews;
    }

    /**
     * Returns the developers' publishing service.
     *
     * @return the publishing service
     */
    public PublishingService publishing() {
        return publishing;
    }

    /**
     * Returns the image service.
     *
     * @return the media service
     */
    public MediaService media() {
        return media;
    }

    /** Closes the data access layer and its connection. */
    @Override
    public void close() {
        data.close();
    }
}
