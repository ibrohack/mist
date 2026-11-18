package com.mist.ui;

/**
 * What the login screen shows when another screen sends the user there: an email to fill in and a message, such as
 * "Account created" after registering or "This account has been deactivated" after a forced sign-out.
 *
 * @param email      the email to fill in, or {@code null}
 * @param messageKey the bundle key of the message, or {@code null}
 * @param success    {@code true} to show the message as a success, {@code false} as an error
 */
public record LoginNotice(String email, String messageKey, boolean success) {
}
