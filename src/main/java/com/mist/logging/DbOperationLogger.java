package com.mist.logging;

/**
 * Writes the database operation log through logger {@code com.mist.db}.
 *
 * <p>Each line has the format {@code date | user | OPERATION | collection | details | OK/FAILED}. The user comes from the
 * logging context set at login.</p>
 */
public class DbOperationLogger {
}
