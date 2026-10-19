package com.mist.exception;

/**
 * A write broke a unique index, such as a username or email that is already taken.
 */
public class DuplicateEntityException extends DataAccessException {

    /**
     * Creates an exception without a cause.
     *
     * @param messageKey bundle key of the message shown to the user
     */
    public DuplicateEntityException(String messageKey) {
        super(messageKey);
    }

    /**
     * Creates an exception that wraps a lower-level failure.
     *
     * @param messageKey bundle key of the message shown to the user
     * @param cause      the failure that caused this one
     */
    public DuplicateEntityException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}
