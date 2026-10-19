package com.mist.exception;

/**
 * A DAO could not complete a database operation.
 *
 * <p>DAOs wrap driver and Hibernate failures in it, so no persistence type reaches the services.</p>
 */
public class DataAccessException extends MistException {

    /**
     * Creates an exception without a cause.
     *
     * @param messageKey bundle key of the message shown to the user
     */
    public DataAccessException(String messageKey) {
        super(messageKey);
    }

    /**
     * Creates an exception that wraps a lower-level failure.
     *
     * @param messageKey bundle key of the message shown to the user
     * @param cause      the failure that caused this one
     */
    public DataAccessException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}
