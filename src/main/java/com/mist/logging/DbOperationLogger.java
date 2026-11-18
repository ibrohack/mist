package com.mist.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Writes the database operation log through logger {@code com.mist.db}.
 *
 * <p>Each line has the format {@code date | user | tx | OPERATION | collection | details | OK/FAILED}. Logback adds
 * the date, the user and the transaction; this class writes the rest. The user is the one set at login with
 * {@link #setUser(String)}. It is kept for the whole app rather than per thread, and copied into the logging context
 * ({@code MDC} key {@value #USER_KEY}) around each line, so operations run on background threads are logged with the
 * right user. Before anyone is logged in, {@link #actingAs(String)} names the email being tried for the lines of the
 * current thread, so a login or a registration is not logged as anonymous.</p>
 *
 * <p>Every transaction gets a number. {@link #begin()} writes a {@code BEGIN} line and tags the following lines of the
 * thread with it ({@code MDC} key {@value #TX_KEY}), until {@link #commit()} or {@link #rollback(String)} closes it.
 * So the lines a rollback undid can be told apart from those that were kept. Lines outside a transaction show
 * {@code tx=-}.</p>
 *
 * <p>Details never include passwords or password hashes.</p>
 */
public final class DbOperationLogger {

    /** Name of the logger that {@code logback.xml} routes to {@code mist-db.log}. */
    public static final String LOGGER_NAME = "com.mist.db";
    /** Logging-context key that {@code logback.xml} reads the user from. */
    public static final String USER_KEY = "user";
    /** Logging-context key that {@code logback.xml} reads the transaction number from. */
    public static final String TX_KEY = "tx";

    /** The logger that writes {@code mist-db.log}. */
    private static final Logger LOG = LoggerFactory.getLogger(LOGGER_NAME);
    /** The outcome of an operation that worked. */
    private static final String OK = "OK";
    /** The outcome of an operation that failed. */
    private static final String FAILED = "FAILED";
    /** The collection column of a line that is about a transaction rather than one collection. */
    private static final String NO_COLLECTION = "-";

    /** The last transaction number handed out; numbers restart at 1 on each start of the app. */
    private static final AtomicLong LAST_TRANSACTION = new AtomicLong();
    /** The transaction open on each thread, or {@code null} outside a transaction. */
    private static final ThreadLocal<String> TRANSACTION = new ThreadLocal<>();
    /** The email being tried on each thread before login, or {@code null}. */
    private static final ThreadLocal<String> ACTING_AS = new ThreadLocal<>();

    /** The logged-in user written on each line, or {@code null} for anonymous. */
    private static volatile String currentUser;

    /** Creates a logger; every instance writes to the same log, with the same current user. */
    public DbOperationLogger() {
        // Stateless: the user is kept app-wide and the transaction per thread.
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
     * Writes an email as the user of the lines this thread logs until the returned scope is closed. Used while
     * nobody is logged in yet: a login names the email typed and a registration the new account's email.
     *
     * @param email the email being tried, or {@code null} to leave the user unchanged
     * @return the scope to close, best in a try-with-resources block
     */
    public static Scope actingAs(String email) {
        String previous = ACTING_AS.get();
        if (email != null && !email.isBlank()) {
            ACTING_AS.set(email.strip());
        }
        return () -> {
            if (previous == null) {
                ACTING_AS.remove();
            } else {
                ACTING_AS.set(previous);
            }
        };
    }

    /**
     * Opens a transaction on this thread: writes a {@code BEGIN} line and tags the following lines with its number.
     *
     * @return the transaction number, as written in the log
     */
    public String begin() {
        String transaction = String.valueOf(LAST_TRANSACTION.incrementAndGet());
        TRANSACTION.set(transaction);
        write("BEGIN", NO_COLLECTION, null, OK);
        return transaction;
    }

    /** Writes a {@code COMMIT} line for the transaction open on this thread and closes it. */
    public void commit() {
        write("COMMIT", NO_COLLECTION, null, OK);
        TRANSACTION.remove();
    }

    /**
     * Writes a {@code ROLLBACK} line for the transaction open on this thread and closes it.
     *
     * @param reason why it was rolled back
     */
    public void rollback(String reason) {
        write("ROLLBACK", NO_COLLECTION, reason, FAILED);
        TRANSACTION.remove();
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
     * Writes one line with the current user and transaction.
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
        // An email being tried on this thread wins over the app-wide user: nobody is logged in yet.
        String actingAs = ACTING_AS.get();
        String user = actingAs != null ? actingAs : currentUser;
        String previousUser = MDC.get(USER_KEY);
        String previousTransaction = MDC.get(TX_KEY);
        put(USER_KEY, user);
        put(TX_KEY, TRANSACTION.get());
        try {
            LOG.info("{} | {} | {} | {}", operation, collection, clean(details), outcome);
        } finally {
            restore(USER_KEY, previousUser);
            restore(TX_KEY, previousTransaction);
        }
    }

    /**
     * Puts a value into the logging context, or leaves the key alone when there is no value.
     *
     * @param key   the key
     * @param value the value, possibly {@code null}
     */
    private static void put(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }

    /**
     * Puts back the value a key had before a line was written.
     *
     * @param key      the key
     * @param previous the earlier value, or {@code null} if the key was not set
     */
    private static void restore(String key, String previous) {
        if (previous != null) {
            MDC.put(key, previous);
        } else {
            MDC.remove(key);
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

    /** What {@link #actingAs(String)} returns: closing it ends the scope, and never throws. */
    @FunctionalInterface
    public interface Scope extends AutoCloseable {

        /** Ends the scope: the thread's lines go back to the user they had before. */
        @Override
        void close();
    }
}
