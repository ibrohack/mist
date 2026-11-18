package com.mist.model;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Struct;

import java.util.regex.Pattern;

/**
 * A phone number split into its country code and national number. Embedded in {@code users}.
 *
 * @param countryCode a {@code +} followed by 1 to 3 digits, such as {@code +34}
 * @param number      the national number, 4 to 14 digits
 */
@Embeddable
@Struct(name = "Phone")
public record Phone(String countryCode, String number) {

    /** The shape of a country calling code, such as {@code +34}; the {@code users} validator uses it too. */
    public static final String COUNTRY_CODE_REGEX = "\\+[1-9][0-9]{0,2}";
    /** The shape of a national number, 4 to 14 digits; the {@code users} validator uses it too. */
    public static final String NUMBER_REGEX = "[0-9]{4,14}";

    /** {@link #COUNTRY_CODE_REGEX}, compiled. */
    private static final Pattern COUNTRY_CODE = Pattern.compile(COUNTRY_CODE_REGEX);
    /** {@link #NUMBER_REGEX}, compiled. */
    private static final Pattern NUMBER = Pattern.compile(NUMBER_REGEX);

    /**
     * Checks both parts.
     *
     * @throws IllegalArgumentException with {@link ModelKeys#PHONE_COUNTRY_CODE} or {@link ModelKeys#PHONE_NUMBER}
     */
    public Phone {
        Checks.matching(countryCode, COUNTRY_CODE, ModelKeys.PHONE_COUNTRY_CODE);
        Checks.matching(number, NUMBER, ModelKeys.PHONE_NUMBER);
    }

    /**
     * Normalizes a phone as the user typed it: a leading {@code 00} or a missing {@code +} becomes {@code +}, and
     * spaces, dots, dashes and parentheses are dropped from the number.
     *
     * @param countryCode country code, such as {@code +34}, {@code 34} or {@code 0034}
     * @param number      national number, such as {@code 600 11 12 22}
     * @return the normalized phone
     * @throws IllegalArgumentException with {@link ModelKeys#PHONE_COUNTRY_CODE} or {@link ModelKeys#PHONE_NUMBER}
     */
    public static Phone of(String countryCode, String number) {
        String code = countryCode == null ? null : countryCode.replaceAll("\\s", "");
        if (code != null && code.startsWith("00")) {
            code = "+" + code.substring(2);
        } else if (code != null && !code.isEmpty() && !code.startsWith("+")) {
            code = "+" + code;
        }
        String digits = number == null ? null : number.replaceAll("[\\s().-]", "");
        return new Phone(code, digits);
    }

    /**
     * Returns the phone as it is shown on screen.
     *
     * @return the country code and the number, such as {@code +34 600111222}
     */
    public String formatted() {
        return countryCode + " " + number;
    }
}
