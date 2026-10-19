package com.mist.exception;

/**
 * The current user may not perform the action, or the login credentials are wrong.
 */
public class UnauthorizedException extends MistException {

    /**
     * Creates an exception without a cause.
     *
     * @param messageKey bundle key of the message shown to the user
     */
    public UnauthorizedException(String messageKey) {
        super(messageKey);
    }

    /**
     * Creates an exception that wraps a lower-level failure.
     *
     * @param messageKey bundle key of the message shown to the user
     * @param cause      the failure that caused this one
     */
    public UnauthorizedException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}
