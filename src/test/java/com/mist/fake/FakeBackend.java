package com.mist.fake;

import com.mist.dao.DaoKeys;
import com.mist.exception.DataAccessException;
import com.mist.service.AuthService;
import com.mist.service.LibraryService;
import com.mist.service.MediaService;
import com.mist.service.PublishingService;
import com.mist.service.ReviewService;
import com.mist.service.StoreService;
import com.mist.service.UserService;
import com.mist.ui.AppServices;
import com.mist.ui.Backend;

import java.time.Clock;

/**
 * The backend of the UI tests: the real services on the in-memory DAOs, with the test's clock.
 *
 * <p>Connecting fails while the database is offline, so tests reach the offline screen the way a stopped MongoDB
 * would.</p>
 */
public final class FakeBackend implements Backend {

    /** The in-memory database. */
    private final FakeDatabase db;
    /** The users DAO. */
    private final FakeUserDao users;
    /** The games DAO. */
    private final FakeGameDao games;
    /** The orders DAO. */
    private final FakeOrderDao orders;
    /** The reviews DAO. */
    private final FakeReviewDao reviews;
    /** The media DAO. */
    private final FakeMediaDao media;
    /** The transaction runner. */
    private final FakeTransactions transactions;
    /** The services, built on the DAOs. */
    private final AppServices services;

    /**
     * Creates the backend on an in-memory database.
     *
     * @param db    the in-memory database
     * @param clock the clock the services date their changes with
     */
    public FakeBackend(FakeDatabase db, Clock clock) {
        this.db = db;
        this.users = new FakeUserDao(db);
        this.games = new FakeGameDao(db);
        this.orders = new FakeOrderDao(db);
        this.reviews = new FakeReviewDao(db);
        this.media = new FakeMediaDao(db);
        this.transactions = new FakeTransactions(db);
        this.services = new AppServices(
                new AuthService(users, clock),
                new UserService(users, games, media, transactions, clock),
                new StoreService(games, users, orders, transactions, clock),
                new LibraryService(users, games, orders, transactions, clock),
                new ReviewService(reviews, users, games, transactions, clock),
                new PublishingService(games, users, media, transactions, clock),
                new MediaService(media));
    }

    @Override
    public AppServices connect() throws DataAccessException {
        if (db.isOffline()) {
            throw new DataAccessException(DaoKeys.FAILURE);
        }
        return services;
    }

    @Override
    public void close() {
        // Nothing to release in memory.
    }

    /**
     * Returns the services, for tests that prepare data through them.
     *
     * @return the services
     */
    public AppServices services() {
        return services;
    }

    /**
     * Returns the users DAO, for seeding.
     *
     * @return the users DAO
     */
    public FakeUserDao users() {
        return users;
    }

    /**
     * Returns the games DAO, for seeding.
     *
     * @return the games DAO
     */
    public FakeGameDao games() {
        return games;
    }

    /**
     * Returns the orders DAO, for seeding.
     *
     * @return the orders DAO
     */
    public FakeOrderDao orders() {
        return orders;
    }

    /**
     * Returns the reviews DAO, for seeding.
     *
     * @return the reviews DAO
     */
    public FakeReviewDao reviews() {
        return reviews;
    }

    /**
     * Returns the media DAO, for seeding.
     *
     * @return the media DAO
     */
    public FakeMediaDao media() {
        return media;
    }

    /**
     * Returns the transaction runner, for seeding.
     *
     * @return the transaction runner
     */
    public FakeTransactions transactions() {
        return transactions;
    }
}
