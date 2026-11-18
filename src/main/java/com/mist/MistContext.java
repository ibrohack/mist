package com.mist;

import com.mist.config.AppConfig;
import com.mist.dao.hibernate.HibernateDataAccess;
import com.mist.exception.DataAccessException;
import com.mist.exception.MistException;
import com.mist.service.AuthService;
import com.mist.service.DemoData;
import com.mist.service.LibraryService;
import com.mist.service.LogService;
import com.mist.service.MediaService;
import com.mist.service.PublishingService;
import com.mist.service.ReviewService;
import com.mist.service.StoreService;
import com.mist.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

/**
 * Wires MIST together: opens the data access layer and builds every service on it. The UI gets its services from here.
 *
 * <p>Open it once, when the splash checks the database, and close it when the app stops. Opening fails with a
 * {@link DataAccessException} if MongoDB cannot be reached, so the splash can show the offline state. Unless
 * {@code mist.seed.demo} is {@code false}, opening also fills an empty database with {@link DemoData}.</p>
 */
public final class MistContext implements AutoCloseable {

    /** The startup log. */
    private static final Logger LOG = LoggerFactory.getLogger(MistContext.class);
    /** The folder {@code logback.xml} writes the logs to; the operation log viewer reads it. */
    private static final Path LOG_DIRECTORY = Path.of(System.getProperty("user.home"), ".mist", "logs");

    /** The data access layer. */
    private final HibernateDataAccess data;
    /** Login and registration. */
    private final AuthService auth;
    /** Profiles and accounts. */
    private final UserService users;
    /** Catalog, cart and checkout. */
    private final StoreService store;
    /** Library and returns. */
    private final LibraryService library;
    /** Ratings and reviews. */
    private final ReviewService reviews;
    /** Developers' games and sales. */
    private final PublishingService publishing;
    /** Images. */
    private final MediaService media;
    /** The operation log reader. */
    private final LogService logs;

    /**
     * Builds the services on the data access layer.
     *
     * @param data  the open data access layer
     * @param clock the clock the services date their changes with
     */
    private MistContext(HibernateDataAccess data, Clock clock) {
        this.data = data;
        this.auth = new AuthService(data.users(), clock);
        this.users = new UserService(data.users(), data.games(), data.media(), data.transactions(), clock);
        this.store = new StoreService(data.games(), data.users(), data.orders(), data.transactions(), clock);
        this.library = new LibraryService(data.users(), data.games(), data.orders(), data.transactions(), clock);
        this.reviews = new ReviewService(data.reviews(), data.users(), data.games(), data.transactions(), clock);
        this.publishing = new PublishingService(data.games(), data.users(), data.media(), data.transactions(), clock);
        this.media = new MediaService(data.media());
        this.logs = new LogService(data.users(), LOG_DIRECTORY);
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
            MistContext context = new MistContext(data, clock);
            if (config.seedDemoData()) {
                seed(data, clock);
            }
            return context;
        } catch (DataAccessException | RuntimeException e) {
            data.close();
            throw e;
        }
    }

    /**
     * Fills an empty database with the demo content. A demo value that breaks a rule is a bug in the demo data, not a
     * reason to keep MIST closed, so it is only logged; a database failure still fails the opening.
     *
     * @param data  the open data access layer
     * @param clock the clock the content is dated with
     * @throws DataAccessException if the database fails while seeding
     */
    private static void seed(HibernateDataAccess data, Clock clock) throws DataAccessException {
        try {
            if (new DemoData(data.users(), data.games(), data.orders(), data.reviews(), data.media(),
                    data.transactions(), clock).seedIfEmpty()) {
                LOG.info("Seeded the demo data into an empty database");
            }
        } catch (DataAccessException e) {
            throw e;
        } catch (MistException e) {
            LOG.error("The demo data broke a rule and was not fully seeded: {}", e.getMessage(), e);
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

    /**
     * Returns the admins' reader of the database operation log.
     *
     * @return the log service
     */
    public LogService logs() {
        return logs;
    }

    /** Closes the data access layer and its connection. */
    @Override
    public void close() {
        data.close();
    }
}
