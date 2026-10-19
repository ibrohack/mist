package com.mist.exception;

/**
 * Input breaks a business rule, such as an empty title or a score outside 1 to 5.
 */
public class ValidationException extends MistException {

    /**
     * Creates an exception without a cause.
     *
     * @param messageKey bundle key of the message shown to the user
     */
    public ValidationException(String messageKey) {
        super(messageKey);
    }

    /**
     * Creates an exception that wraps a lower-level failure.
     *
     * @param messageKey bundle key of the message shown to the user
     * @param cause      the failure that caused this one
     */
    public ValidationException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}
