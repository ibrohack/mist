package com.mist.exception;

/**
 * Root of the MIST exception hierarchy. Every failure that travels from the services up to the UI extends it, so a
 * controller catches this one type and shows a themed alert.
 *
 * <p>The message is an i18n bundle key, not display text, so the alert follows the selected language.</p>
 */
public class MistException extends Exception {

    /**
     * Creates an exception without a cause.
     *
     * @param messageKey bundle key of the message shown to the user
     */
    public MistException(String messageKey) {
        super(messageKey);
    }

    /**
     * Creates an exception that wraps a lower-level failure.
     *
     * @param messageKey bundle key of the message shown to the user
     * @param cause      the failure that caused this one
     */
    public MistException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}
