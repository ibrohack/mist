package com.mist.dao.hibernate;

import com.mist.exception.DataAccessException;
import com.mist.logging.DbOperationLogger;
import org.bson.types.ObjectId;
import org.hibernate.Session;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Shared plumbing of the Hibernate DAOs: runs each operation on the right session, writes it to the operation log, and
 * turns persistence failures into {@link DataAccessException}.
 *
 * <p>Writes are flushed inside the DAO call, so a broken unique index fails that call, and is logged against it, rather
 * than the commit at the end of the transaction.</p>
 */
abstract class HibernateDao {

    /** Gives the DAO its session. */
    private final HibernateTransactions transactions;
    /** Writes the operation log. */
    private final DbOperationLogger log;
    /** The collection this DAO works on, for the log. */
    private final String collection;

    /**
     * Creates a DAO for one collection.
     *
     * @param transactions the transaction runner whose sessions the DAO uses
     * @param collection   the collection name written to the operation log
     */
    HibernateDao(HibernateTransactions transactions, String collection) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.log = transactions.log();
        this.collection = collection;
    }

    /**
     * Runs one database operation and logs it.
     *
     * @param operation what is done, in upper case, such as {@code FIND} or {@code UPDATE}
     * @param details   the ids or filters involved; never a password or a hash
     * @param work      the work on the session
     * @param <T>       the type of the result
     * @return what the work returned
     * @throws DataAccessException if the database fails, or {@code DuplicateEntityException} if a unique index breaks
     */
    final <T> T execute(String operation, String details, HibernateTransactions.SessionWork<T> work)
            throws DataAccessException {
        try {
            T result = transactions.inSession(work);
            log.ok(operation, collection, details);
            return result;
        } catch (RuntimeException failure) {
            log.failed(operation, collection, details + " (" + PersistenceErrors.describe(failure) + ")");
            if (PersistenceErrors.isPersistence(failure)) {
                throw PersistenceErrors.translate(failure);
            }
            throw failure;
        }
    }

    /**
     * Inserts a new entity or writes the changes of an existing one, and flushes.
     *
     * @param session the session
     * @param entity  the entity
     * @param isNew   {@code true} if it has no id yet
     * @param <T>     the entity type
     * @return the managed entity, with its id
     */
    static <T> T persistOrMerge(Session session, T entity, boolean isNew) {
        T saved;
        if (isNew) {
            session.persist(entity);
            saved = entity;
        } else {
            saved = session.merge(entity);
        }
        session.flush();
        return saved;
    }

    /**
     * Formats ids for the operation log.
     *
     * @param ids the ids
     * @return the ids in brackets, or the count if there are many
     */
    static String ids(Collection<ObjectId> ids) {
        if (ids.size() > 10) {
            return ids.size() + " ids";
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    }
}
