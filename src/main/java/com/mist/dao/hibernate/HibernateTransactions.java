package com.mist.dao.hibernate;

import com.mist.dao.TransactionRunner;
import com.mist.exception.MistException;
import com.mist.logging.DbOperationLogger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * {@link TransactionRunner} on Hibernate sessions, which the MongoDB extension maps to MongoDB transactions.
 *
 * <p>The outermost {@link #call(Work)} opens a session, begins a transaction and binds the session to the current
 * thread; every DAO call on that thread then uses it, and nested calls join it. Outside a transaction, each DAO call
 * runs in a short transaction of its own.</p>
 *
 * <p>The outermost transaction writes {@code BEGIN}, then {@code COMMIT} or {@code ROLLBACK}, to the operation log, and
 * the DAO lines in between carry its number. When MongoDB reports a transient conflict (another transaction wrote the
 * same document first), the whole work runs again in a new transaction after a short random pause, up to
 * {@value #MAX_ATTEMPTS} times; that is why the work must load what it changes inside the transaction.</p>
 */
public final class HibernateTransactions implements TransactionRunner {

    /** How many times a transaction runs at most when MongoDB reports a transient conflict. */
    static final int MAX_ATTEMPTS = 5;
    /** Longest pause before the second attempt, in milliseconds; it grows with each attempt. */
    private static final int MAX_PAUSE_MILLIS = 40;

    /** Opens the sessions. */
    private final SessionFactory sessionFactory;
    /** Writes the transaction boundaries, retries and rollbacks to the operation log. */
    private final DbOperationLogger log;
    /** The session of the transaction running on this thread. */
    private final ThreadLocal<Session> current = new ThreadLocal<>();

    /**
     * Creates a runner on a session factory.
     *
     * @param sessionFactory the factory built by {@code HibernateBootstrap}
     * @param log            the database operation log
     */
    public HibernateTransactions(SessionFactory sessionFactory, DbOperationLogger log) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory");
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public <T> T call(Work<T> work) throws MistException {
        Objects.requireNonNull(work, "work");
        if (current.get() != null) {
            return work.run();
        }
        for (int attempt = 1; ; attempt++) {
            try {
                return attempt(work);
            } catch (MistException | RuntimeException failure) {
                // Another transaction changed the same documents first: MongoDB asks to run the whole work again.
                if (attempt >= MAX_ATTEMPTS || !PersistenceErrors.isTransient(failure)) {
                    throw failure;
                }
                log.failed("RETRY", "-", "attempt " + (attempt + 1) + " of " + MAX_ATTEMPTS + " after "
                        + PersistenceErrors.describe(failure));
                if (!pause(attempt)) {
                    // Interrupted while waiting: give up and report the conflict.
                    throw failure;
                }
            }
        }
    }

    /**
     * Waits a random moment before the next attempt, so transactions that met do not meet again at once.
     *
     * @param attempt the attempt that just failed, from 1
     * @return {@code false} if the thread was interrupted while waiting; its interrupt flag is set again
     */
    private static boolean pause(int attempt) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(1, MAX_PAUSE_MILLIS * attempt + 1));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Runs work once in a new session and transaction, bound to this thread, and commits it.
     *
     * @param work the work
     * @param <T>  the type of the result
     * @return what the work returned, after the commit
     * @throws MistException what the work threw, or a {@code DataAccessException} if the database failed
     */
    private <T> T attempt(Work<T> work) throws MistException {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            current.set(session);
            log.begin();
            try {
                T result = work.run();
                transaction.commit();
                log.commit();
                return result;
            } catch (Throwable failure) {
                rollback(transaction, failure);
                throw failure;
            } finally {
                current.remove();
            }
        } catch (RuntimeException failure) {
            if (PersistenceErrors.isPersistence(failure)) {
                throw PersistenceErrors.translate(failure);
            }
            throw failure;
        }
    }

    /**
     * Runs DAO work on the session of the current transaction, or in a short transaction of its own when none is open.
     *
     * @param work the work
     * @param <T>  the type of the result
     * @return what the work returned
     */
    <T> T inSession(SessionWork<T> work) {
        Session bound = current.get();
        if (bound != null) {
            return work.apply(bound);
        }
        return sessionFactory.fromTransaction(work::apply);
    }

    /**
     * Returns the operation log the DAOs write to.
     *
     * @return the log
     */
    DbOperationLogger log() {
        return log;
    }

    /**
     * Rolls a transaction back and logs why.
     *
     * @param transaction the transaction
     * @param failure     what made it fail
     */
    private void rollback(Transaction transaction, Throwable failure) {
        try {
            if (transaction.getStatus().canRollback()) {
                transaction.rollback();
            }
        } catch (RuntimeException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
        // A MistException carries a bundle key; add what the database said when it wraps a driver failure.
        String reason = failure instanceof MistException mist
                ? mist.getMessage() + (mist.getCause() == null ? "" : " (" + PersistenceErrors.describe(mist) + ")")
                : PersistenceErrors.describe(failure);
        log.rollback(reason);
    }

    /**
     * DAO work on an open session.
     *
     * @param <T> the type of the result
     */
    @FunctionalInterface
    interface SessionWork<T> {

        /**
         * Does the work.
         *
         * @param session the session of the current transaction
         * @return the result
         */
        T apply(Session session);
    }
}
