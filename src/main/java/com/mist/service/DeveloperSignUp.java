package com.mist.service;

/**
 * The registration form of a new developer.
 *
 * <p>The service wipes {@code password} once it has hashed it.</p>
 *
 * @param firstName   first name of the account holder
 * @param lastName    last name of the account holder
 * @param email       email address, which becomes the login
 * @param password    the chosen password
 * @param phone       phone, or {@code null}
 * @param address     postal address
 * @param studioName  name shown on the developer's games
 * @param bankAccount IBAN the revenue is paid into
 */
public record DeveloperSignUp(String firstName, String lastName, String email, char[] password, PhoneForm phone,
                              AddressForm address, String studioName, String bankAccount) {

    /** Leaves the password and bank account out, so they never reach a log or a stack trace. */
    @Override
    public String toString() {
        return "DeveloperSignUp[email=" + email + ", studioName=" + studioName + "]";
    }
}
