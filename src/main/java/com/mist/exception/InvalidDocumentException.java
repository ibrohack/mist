package com.mist.exception;

/**
 * MongoDB refused a document because it breaks the collection's {@code $jsonSchema} validator.
 *
 * <p>It is a {@link DataAccessException} because the database reported it, but retrying cannot help: a value does not
 * meet a rule of the data model. The UI shows it as a warning, without a Retry button.</p>
 */
public class InvalidDocumentException extends DataAccessException {

    /**
     * Creates an exception without a cause.
     *
     * @param messageKey bundle key of the message shown to the user
     */
    public InvalidDocumentException(String messageKey) {
        super(messageKey);
    }

    /**
     * Creates an exception that wraps the driver's failure.
     *
     * @param messageKey bundle key of the message shown to the user
     * @param cause      the failure that caused this one
     */
    public InvalidDocumentException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}
