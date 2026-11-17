package com.mist.service;

/**
 * The personal details shown on the Profile view.
 *
 * @param firstName first name
 * @param lastName  last name
 * @param email     email address, which is also the login
 * @param phone     phone, or {@code null}
 * @param address   postal address; may be {@code null} only for an admin
 */
public record ProfileForm(String firstName, String lastName, String email, PhoneForm phone, AddressForm address) {
}
