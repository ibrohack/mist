package com.mist.ui.report;

import java.io.IOException;

/**
 * A report could not be built or written, for instance because the chosen file cannot be written. The UI reports it
 * as a file error with its own message.
 */
public class ReportException extends IOException {

    /**
     * Creates the exception.
     *
     * @param message what failed, for the log
     * @param cause   the JasperReports or file error
     */
    public ReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
