package com.mist.dao.hibernate;

import com.mist.dao.TransactionRunner;
import com.mist.exception.MistException;
import com.mist.logging.DbOperationLogger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.Objects;

/**
 * {@link TransactionRunner} on Hibernate sessions, which the MongoDB extension maps to MongoDB transactions.
 *
 * <p>The outermost {@link #call(Work)} opens a session, begins a transaction and binds the session to the current
 * thread; every DAO call on that thread then uses it, and nested calls join it. Outside a transaction, each DAO call
 * runs in a short transaction of its own.</p>
 */
public final class HibernateTransactions implements TransactionRunner {

    private final SessionFactory sessionFactory;
    private final DbOperationLogger log;
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
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            current.set(session);
            try {
                T result = work.run();
                transaction.commit();
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

    private void rollback(Transaction transaction, Throwable failure) {
        try {
            if (transaction.getStatus().canRollback()) {
                transaction.rollback();
            }
        } catch (RuntimeException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
        String reason = failure instanceof MistException ? failure.getMessage() : PersistenceErrors.describe(failure);
        log.failed("ROLLBACK", "-", reason);
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
