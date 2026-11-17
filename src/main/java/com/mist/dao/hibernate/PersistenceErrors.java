package com.mist.dao.hibernate;

import com.mist.dao.DaoKeys;
import com.mist.exception.DataAccessException;
import com.mist.exception.DuplicateEntityException;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import jakarta.persistence.PersistenceException;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.exception.ConstraintViolationException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Turns Hibernate and driver failures into the checked {@link DataAccessException}, so no persistence type reaches
 * the services.
 *
 * <p>The MongoDB extension reports a server error as an {@link SQLException} whose vendor code is the MongoDB error
 * code, with the driver's {@link MongoException} as its cause. A duplicate key (codes 11000, 11001 and 12582) becomes a
 * {@link DuplicateEntityException}.</p>
 */
final class PersistenceErrors {

    private PersistenceErrors() {
    }

    /**
     * Tells whether a failure came from Hibernate, JPA, JDBC or the driver, rather than from MIST's own code.
     *
     * @param failure the failure
     * @return {@code true} if a persistence exception is anywhere in its cause chain
     */
    static boolean isPersistence(Throwable failure) {
        for (Throwable cause : chain(failure)) {
            if (cause instanceof HibernateException || cause instanceof PersistenceException
                    || cause instanceof MongoException || cause instanceof SQLException) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wraps a persistence failure in the matching checked exception.
     *
     * @param failure a failure for which {@link #isPersistence(Throwable)} is {@code true}
     * @return a {@link DuplicateEntityException} for a broken unique index, otherwise a {@link DataAccessException}
     */
    static DataAccessException translate(RuntimeException failure) {
        return isDuplicateKey(failure)
                ? new DuplicateEntityException(DaoKeys.DUPLICATE, failure)
                : new DataAccessException(DaoKeys.FAILURE, failure);
    }

    /**
     * Describes a failure in a few words for the operation log, without the stack trace.
     *
     * @param failure the failure
     * @return the class name of the deepest cause and its first message line
     */
    static String describe(Throwable failure) {
        Throwable root = failure;
        for (Throwable cause : chain(failure)) {
            root = cause;
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            return root.getClass().getSimpleName();
        }
        String firstLine = message.lines().findFirst().orElse("");
        return root.getClass().getSimpleName() + ": "
                + (firstLine.length() > 160 ? firstLine.substring(0, 160) + "..." : firstLine);
    }

    private static boolean isDuplicateKey(Throwable failure) {
        for (Throwable cause : chain(failure)) {
            if (cause instanceof ConstraintViolationException
                    || cause instanceof MongoException mongo && isDuplicateKey(mongo.getCode())
                    || cause instanceof JDBCException jdbc && isDuplicateKey(jdbc.getErrorCode())
                    || cause instanceof SQLException sql && isDuplicateKey(sql.getErrorCode())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDuplicateKey(int code) {
        return code != 0 && ErrorCategory.fromErrorCode(code) == ErrorCategory.DUPLICATE_KEY;
    }

    /** The failure and its causes, outermost first, stopping at a cycle. */
    private static Iterable<Throwable> chain(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Throwable> causes = new ArrayList<>();
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            causes.add(cause);
        }
        return causes;
    }
}
