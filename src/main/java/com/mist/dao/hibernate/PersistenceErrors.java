package com.mist.dao.hibernate;

import com.mist.dao.DaoKeys;
import com.mist.exception.DataAccessException;
import com.mist.exception.DuplicateEntityException;
import com.mist.exception.InvalidDocumentException;
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
 * {@link DuplicateEntityException}, and a document the collection's {@code $jsonSchema} rejected (code 121) an
 * {@link InvalidDocumentException}.</p>
 */
final class PersistenceErrors {

    /** Server error code when a document fails the collection's {@code $jsonSchema} validator. */
    static final int DOCUMENT_VALIDATION_FAILURE = 121;
    /** Server error code when two transactions write the same document at the same time. */
    static final int WRITE_CONFLICT = 112;
    /** Error label MongoDB puts on a failure after which the whole transaction can be run again. */
    static final String TRANSIENT_TRANSACTION_ERROR = "TransientTransactionError";

    /** Not instantiable: a holder of error translation helpers. */
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
     * Tells whether a failure only means that the transaction met another one and can be run again.
     *
     * @param failure the failure, possibly already wrapped in a {@code MistException}
     * @return {@code true} for a write conflict or a failure labelled {@value #TRANSIENT_TRANSACTION_ERROR}
     */
    static boolean isTransient(Throwable failure) {
        for (Throwable cause : chain(failure)) {
            if (cause instanceof MongoException mongo
                    && (mongo.hasErrorLabel(TRANSIENT_TRANSACTION_ERROR) || mongo.getCode() == WRITE_CONFLICT)
                    || cause instanceof SQLException sql && sql.getErrorCode() == WRITE_CONFLICT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wraps a persistence failure in the matching checked exception.
     *
     * @param failure a failure for which {@link #isPersistence(Throwable)} is {@code true}
     * @return a {@link DuplicateEntityException} for a broken unique index, an {@link InvalidDocumentException} for a
     *         document the validator rejected, otherwise a {@link DataAccessException}
     */
    static DataAccessException translate(RuntimeException failure) {
        if (isDuplicateKey(failure)) {
            return new DuplicateEntityException(DaoKeys.DUPLICATE, failure);
        }
        if (hasCode(failure, DOCUMENT_VALIDATION_FAILURE)) {
            return new InvalidDocumentException(DaoKeys.INVALID_DOCUMENT, failure);
        }
        return new DataAccessException(DaoKeys.FAILURE, failure);
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

    /**
     * Tells whether a failure is a broken unique index.
     *
     * @param failure the failure
     * @return {@code true} for a duplicate key
     */
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

    /**
     * Tells whether the driver or the extension reported a given MongoDB error code anywhere in a failure.
     *
     * @param failure the failure
     * @param code    the MongoDB error code
     * @return {@code true} if a cause carries that code
     */
    private static boolean hasCode(Throwable failure, int code) {
        for (Throwable cause : chain(failure)) {
            if (cause instanceof MongoException mongo && mongo.getCode() == code
                    || cause instanceof JDBCException jdbc && jdbc.getErrorCode() == code
                    || cause instanceof SQLException sql && sql.getErrorCode() == code) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a MongoDB error code means a duplicate key.
     *
     * @param code the error code
     * @return {@code true} for 11000, 11001 and 12582
     */
    private static boolean isDuplicateKey(int code) {
        return code != 0 && ErrorCategory.fromErrorCode(code) == ErrorCategory.DUPLICATE_KEY;
    }

    /**
     * Lists a failure and its causes, outermost first, stopping at a cycle.
     *
     * @param failure the failure
     * @return the failure followed by its causes
     */
    private static Iterable<Throwable> chain(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Throwable> causes = new ArrayList<>();
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            causes.add(cause);
        }
        return causes;
    }
}
