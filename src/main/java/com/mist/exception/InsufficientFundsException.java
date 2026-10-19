package com.mist.exception;

/**
 * The wallet balance is lower than the price of the purchase.
 */
public class InsufficientFundsException extends MistException {

    /**
     * Creates an exception without a cause.
     *
     * @param messageKey bundle key of the message shown to the user
     */
    public InsufficientFundsException(String messageKey) {
        super(messageKey);
    }

    /**
     * Creates an exception that wraps a lower-level failure.
     *
     * @param messageKey bundle key of the message shown to the user
     * @param cause      the failure that caused this one
     */
    public InsufficientFundsException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}
