package com.mist.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Writes the database operation log through logger {@code com.mist.db}.
 *
 * <p>Each line has the format {@code date | user | OPERATION | collection | details | OK/FAILED}. Logback adds the
 * date and the user; this class writes the rest. The user is the one set at login with {@link #setUser(String)}. It is
 * kept for the whole app rather than per thread, and copied into the logging context ({@code MDC} key
 * {@value #USER_KEY}) around each line, so operations run on background threads are logged with the right user.</p>
 *
 * <p>Details never include passwords or password hashes.</p>
 */
public final class DbOperationLogger {

    /** Name of the logger that {@code logback.xml} routes to {@code mist-db.log}. */
    public static final String LOGGER_NAME = "com.mist.db";
    /** Logging-context key that {@code logback.xml} reads the user from. */
    public static final String USER_KEY = "user";

    /** The logger that writes {@code mist-db.log}. */
    private static final Logger LOG = LoggerFactory.getLogger(LOGGER_NAME);
    /** The outcome of an operation that worked. */
    private static final String OK = "OK";
    /** The outcome of an operation that failed. */
    private static final String FAILED = "FAILED";

    /** The logged-in user written on each line, or {@code null} for anonymous. */
    private static volatile String currentUser;

    /** Creates a logger; every instance writes to the same log, with the same current user. */
    public DbOperationLogger() {
        // Stateless: the user is kept app-wide.
    }

    /**
     * Sets the user written on every following line, after a successful login.
     *
     * @param user the logged-in user's email, or {@code null} to log as anonymous
     */
    public static void setUser(String user) {
        currentUser = user;
    }

    /** Logs the following lines as anonymous again, after logout. */
    public static void clearUser() {
        currentUser = null;
    }

    /**
     * Records an operation that succeeded.
     *
     * @param operation  what was done, in upper case, such as {@code FIND} or {@code INSERT}
     * @param collection the collection it touched
     * @param details    the ids or filters involved
     */
    public void ok(String operation, String collection, String details) {
        write(operation, collection, details, OK);
    }

    /**
     * Records an operation that failed.
     *
     * @param operation  what was attempted, in upper case
     * @param collection the collection it touched
     * @param details    the ids or filters involved, and the reason when known
     */
    public void failed(String operation, String collection, String details) {
        write(operation, collection, details, FAILED);
    }

    /**
     * Writes one line with the current user.
     *
     * @param operation  what was done
     * @param collection the collection it touched
     * @param details    the ids or filters involved
     * @param outcome    {@code OK} or {@code FAILED}
     */
    private static void write(String operation, String collection, String details, String outcome) {
        if (!LOG.isInfoEnabled()) {
            return;
        }
        String user = currentUser;
        String previous = MDC.get(USER_KEY);
        if (user != null) {
            MDC.put(USER_KEY, user);
        }
        try {
            LOG.info("{} | {} | {} | {}", operation, collection, clean(details), outcome);
        } finally {
            if (previous != null) {
                MDC.put(USER_KEY, previous);
            } else {
                MDC.remove(USER_KEY);
            }
        }
    }

    /**
     * Keeps one operation on one line and the column separator unambiguous.
     *
     * @param details the details, possibly {@code null}
     * @return the details on one line, with {@code |} replaced, or {@code -} when there are none
     */
    private static String clean(String details) {
        if (details == null || details.isBlank()) {
            return "-";
        }
        return details.replaceAll("[\\r\\n]+", " ").replace('|', '/');
    }
}
