package com.mist.model;

/**
 * Brand of a payment card, detected from its number.
 */
public enum CardBrand {

    /** Visa: numbers start with 4. */
    VISA,

    /** Mastercard: numbers start with 51 to 55 or 2221 to 2720. */
    MASTERCARD,

    /** American Express: numbers start with 34 or 37. */
    AMEX,

    /** Any other brand. */
    OTHER
}
