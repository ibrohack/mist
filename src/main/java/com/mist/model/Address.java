package com.mist.model;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Struct;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A postal address split into its parts. Embedded in {@code users}.
 *
 * @param street     street and number
 * @param details    floor, door or other details; {@code null} when there are none
 * @param postalCode postal code, 3 to 10 letters, digits, spaces or dashes
 * @param city       city or town
 * @param province   province, state or region; {@code null} when not needed
 * @param country    two-letter ISO 3166-1 country code, such as {@code ES}
 */
@Embeddable
@Struct(name = "Address")
public record Address(String street, String details, String postalCode, String city, String province,
                      String country) {

    /** Longest street accepted. */
    public static final int MAX_STREET_LENGTH = 120;
    /** Longest details accepted. */
    public static final int MAX_DETAILS_LENGTH = 60;
    /** Longest city accepted. */
    public static final int MAX_CITY_LENGTH = 80;
    /** Longest province accepted. */
    public static final int MAX_PROVINCE_LENGTH = 80;

    private static final Pattern POSTAL_CODE = Pattern.compile("[A-Z0-9][A-Z0-9 -]{1,8}[A-Z0-9]");
    private static final Pattern COUNTRY = Pattern.compile("[A-Z]{2}");

    /**
     * Checks every part; blank optional parts become {@code null}.
     *
     * @throws IllegalArgumentException with one of the {@code ModelKeys.ADDRESS_*} keys
     */
    public Address {
        street = Checks.text(street, 1, MAX_STREET_LENGTH, ModelKeys.ADDRESS_STREET);
        details = Checks.optionalText(details, MAX_DETAILS_LENGTH, ModelKeys.ADDRESS_DETAILS);
        Checks.matching(postalCode, POSTAL_CODE, ModelKeys.ADDRESS_POSTAL_CODE);
        city = Checks.text(city, 1, MAX_CITY_LENGTH, ModelKeys.ADDRESS_CITY);
        province = Checks.optionalText(province, MAX_PROVINCE_LENGTH, ModelKeys.ADDRESS_PROVINCE);
        Checks.matching(country, COUNTRY, ModelKeys.ADDRESS_COUNTRY);
    }

    /**
     * Normalizes an address as the user typed it: trims every part and upper-cases the postal code and country.
     *
     * @param street     street and number
     * @param details    floor, door or other details; may be blank
     * @param postalCode postal code
     * @param city       city or town
     * @param province   province, state or region; may be blank
     * @param country    two-letter ISO 3166-1 country code, in any case
     * @return the normalized address
     * @throws IllegalArgumentException with one of the {@code ModelKeys.ADDRESS_*} keys
     */
    public static Address of(String street, String details, String postalCode, String city, String province,
                             String country) {
        return new Address(street, details, upper(postalCode), city, province, upper(country));
    }

    private static String upper(String value) {
        return value == null ? null : value.strip().toUpperCase(Locale.ROOT);
    }
}
