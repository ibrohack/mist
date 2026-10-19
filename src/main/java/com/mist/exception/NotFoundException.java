package com.mist.exception;

/**
 * The requested user, developer, game, review or order does not exist.
 */
public class NotFoundException extends MistException {

    /**
     * Creates an exception without a cause.
     *
     * @param messageKey bundle key of the message shown to the user
     */
    public NotFoundException(String messageKey) {
        super(messageKey);
    }

    /**
     * Creates an exception that wraps a lower-level failure.
     *
     * @param messageKey bundle key of the message shown to the user
     * @param cause      the failure that caused this one
     */
    public NotFoundException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}
