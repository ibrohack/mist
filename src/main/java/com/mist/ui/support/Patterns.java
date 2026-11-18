package com.mist.ui.support;

import java.util.regex.Pattern;

/**
 * The text formats the forms check before calling a service. They match the model's own checks, so a value that
 * passes here is only rejected by the service for reasons the UI cannot see (a taken email, a card number that fails
 * its checksum).
 */
public final class Patterns {

    /** A phone country code, with or without the plus sign: {@code +34}, {@code 34}. */
    public static final Pattern PHONE_CODE = Pattern.compile("\\+?[1-9][0-9]{0,2}");
    /** A phone number of 4 to 14 digits, once spaces and dashes are removed. */
    public static final Pattern PHONE_NUMBER = Pattern.compile("[0-9]{4,14}");
    /** A postal code of 3 to 10 letters, digits, spaces or dashes. */
    public static final Pattern POSTAL_CODE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 -]{1,8}[A-Za-z0-9]");
    /** A player's nickname: 3 to 24 letters, digits, dots, dashes or underscores. */
    public static final Pattern NICKNAME = Pattern.compile("[A-Za-z0-9_.-]{3,24}");
    /** A card number of 12 to 19 digits, once spaces and dashes are removed. */
    public static final Pattern CARD_NUMBER = Pattern.compile("[0-9]{12,19}");
    /** An IBAN shape, once spaces are removed; the service checks its checksum. */
    public static final Pattern IBAN = Pattern.compile("[A-Za-z]{2}[0-9]{2}[A-Za-z0-9]{11,30}");

    private Patterns() {
    }
}
