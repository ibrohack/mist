package com.mist.ui.support;

import com.mist.exception.DataAccessException;
import com.mist.exception.DuplicateEntityException;
import com.mist.exception.MistException;
import com.mist.exception.NotFoundException;
import com.mist.exception.UnauthorizedException;
import com.mist.exception.ValidationException;
import com.mist.service.ServiceKeys;
import com.mist.ui.I18n;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Turns exceptions into what the user sees (behavior C-2 of {@code docs/ui-design.md}).
 *
 * <p>The services throw checked {@link MistException}s whose message is a bundle key, so the dialog shows the message
 * in the current language. Rule violations get a warning; database failures an error with a Retry button; anything
 * else, which would be a bug, is logged with its stack trace and gets a generic message. A deactivated account ends
 * the session. Every report is written to {@code mist-app.log} through the logger {@code com.mist.ui}.</p>
 */
public final class ErrorReporter {

    /** The UI's own log: user actions and the errors shown to the user. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");

    /** Ends the session with a message, when the account was deactivated meanwhile. */
    private Consumer<String> accountClosed = key -> { };

    /**
     * Sets what ends the session when a service reports that the account was deactivated.
     *
     * @param handler receives the bundle key of the message for the login screen
     */
    public void onAccountClosed(Consumer<String> handler) {
        this.accountClosed = Objects.requireNonNull(handler, "handler");
    }

    /**
     * Shows an error that has no retry.
     *
     * @param owner the window the dialog belongs to
     * @param error what went wrong
     */
    public void report(Window owner, Throwable error) {
        report(owner, error, null);
    }

    /**
     * Shows an error with the reaction its type calls for.
     *
     * @param owner the window the dialog belongs to
     * @param error what went wrong
     * @param retry what a Retry button runs for database failures, or {@code null}
     */
    public void report(Window owner, Throwable error, Runnable retry) {
        switch (error) {
            case ValidationException e -> warn(owner, e);
            // A duplicate is a rule the user broke, even though it is also a DataAccessException.
            case DuplicateEntityException e -> warn(owner, e);
            case NotFoundException e -> warn(owner, e);
            case UnauthorizedException e when ServiceKeys.ACCOUNT_DEACTIVATED.equals(e.getMessage()) -> {
                LOG.info("The account was deactivated during the session; signing out");
                accountClosed.accept(e.getMessage());
            }
            case UnauthorizedException e -> warn(owner, e);
            case DataAccessException e -> {
                LOG.warn("Database failure shown to the user: {}", e.getMessage(), e);
                Dialogs.error(owner, I18n.textOrKey(e.getMessage()), retry);
            }
            default -> unexpected(owner, error);
        }
    }

    /**
     * Shows an error nobody expected: logs it with its stack trace and shows a generic message.
     *
     * @param owner the window the dialog belongs to, or {@code null}
     * @param error the error
     */
    public void unexpected(Window owner, Throwable error) {
        LOG.error("Unexpected error", error);
        Dialogs.error(owner, I18n.text("error.unexpected"), null);
    }

    /**
     * Logs a rule violation and shows it as a warning.
     *
     * @param owner the window the dialog belongs to
     * @param error the violation, whose message is a bundle key
     */
    private static void warn(Window owner, MistException error) {
        LOG.info("{} shown to the user: {}", error.getClass().getSimpleName(), error.getMessage());
        Dialogs.warning(owner, I18n.textOrKey(error.getMessage()));
    }
}
