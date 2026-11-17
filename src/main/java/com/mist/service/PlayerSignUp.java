package com.mist.service;

/**
 * The registration form of a new player.
 *
 * <p>The service wipes {@code password} once it has hashed it.</p>
 *
 * @param firstName first name
 * @param lastName  last name
 * @param email     email address, which becomes the login
 * @param password  the chosen password
 * @param phone     phone, or {@code null}
 * @param address   postal address
 * @param nickname  public nickname shown on reviews
 * @param card      payment card used at checkout
 */
public record PlayerSignUp(String firstName, String lastName, String email, char[] password, PhoneForm phone,
                           AddressForm address, String nickname, CardForm card) {

    /** Leaves the password out, so it never reaches a log or a stack trace. */
    @Override
    public String toString() {
        return "PlayerSignUp[email=" + email + ", nickname=" + nickname + "]";
    }
}
