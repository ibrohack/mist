package com.mist.dao.hibernate;

import com.mist.config.AppConfig;
import com.mist.config.HibernateBootstrap;
import com.mist.config.MongoConnection;
import com.mist.config.SchemaSetup;
import com.mist.dao.GameDao;
import com.mist.dao.MediaDao;
import com.mist.dao.OrderDao;
import com.mist.dao.ReviewDao;
import com.mist.dao.TransactionRunner;
import com.mist.dao.UserDao;
import com.mist.exception.DataAccessException;
import com.mist.logging.DbOperationLogger;
import com.mist.model.MistModel;
import org.hibernate.SessionFactory;

/**
 * The whole data access layer, opened at startup and closed when the app stops: the shared {@code MongoClient}, the
 * collections and indexes, the Hibernate {@code SessionFactory}, the transaction runner and one DAO per collection.
 *
 * <p>It exposes only the {@code dao} interfaces, so the code that wires the app together never touches Hibernate or
 * the driver.</p>
 */
public final class HibernateDataAccess implements AutoCloseable {

    private final MongoConnection connection;
    private final SessionFactory sessionFactory;
    private final HibernateTransactions transactions;
    private final UserDao users;
    private final GameDao games;
    private final OrderDao orders;
    private final ReviewDao reviews;
    private final MediaDao media;

    private HibernateDataAccess(MongoConnection connection, SessionFactory sessionFactory, DbOperationLogger log) {
        this.connection = connection;
        this.sessionFactory = sessionFactory;
        this.transactions = new HibernateTransactions(sessionFactory, log);
        this.users = new HibernateUserDao(transactions);
        this.games = new HibernateGameDao(transactions, connection);
        this.orders = new HibernateOrderDao(transactions);
        this.reviews = new HibernateReviewDao(transactions);
        this.media = new HibernateMediaDao(transactions);
    }

    /**
     * Connects to MongoDB, creates or updates the collections, validators and indexes, and builds the DAOs.
     *
     * @param config the connection settings
     * @return the open data access layer
     * @throws DataAccessException if MongoDB cannot be reached within the server selection timeout, or rejects the
     *                             schema setup
     */
    public static HibernateDataAccess open(AppConfig config) throws DataAccessException {
        DbOperationLogger log = new DbOperationLogger();
        MongoConnection connection = new MongoConnection(config);
        SessionFactory sessionFactory = null;
        try {
            SchemaSetup.apply(connection);
            sessionFactory = HibernateBootstrap.buildSessionFactory(connection, MistModel.ANNOTATED_CLASSES);
            log.ok("CONNECT", "-", "database=" + connection.databaseName());
            return new HibernateDataAccess(connection, sessionFactory, log);
        } catch (RuntimeException failure) {
            if (sessionFactory != null) {
                sessionFactory.close();
            }
            connection.close();
            log.failed("CONNECT", "-", "database=" + connection.databaseName() + " ("
                    + PersistenceErrors.describe(failure) + ")");
            if (PersistenceErrors.isPersistence(failure)) {
                throw PersistenceErrors.translate(failure);
            }
            throw failure;
        }
    }

    /**
     * Returns the runner the services use for multi-document transactions.
     *
     * @return the transaction runner
     */
    public TransactionRunner transactions() {
        return transactions;
    }

    /**
     * Returns the DAO of the {@code users} collection.
     *
     * @return the user DAO
     */
    public UserDao users() {
        return users;
    }

    /**
     * Returns the DAO of the {@code games} collection.
     *
     * @return the game DAO
     */
    public GameDao games() {
        return games;
    }

    /**
     * Returns the DAO of the {@code orders} collection.
     *
     * @return the order DAO
     */
    public OrderDao orders() {
        return orders;
    }

    /**
     * Returns the DAO of the {@code reviews} collection.
     *
     * @return the review DAO
     */
    public ReviewDao reviews() {
        return reviews;
    }

    /**
     * Returns the DAO of the {@code media} collection.
     *
     * @return the media DAO
     */
    public MediaDao media() {
        return media;
    }

    /** Closes the session factory, then the shared client it runs on. */
    @Override
    public void close() {
        try {
            sessionFactory.close();
        } finally {
            connection.close();
        }
    }
}
