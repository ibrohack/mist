package com.mist.service;

import com.mist.model.PaymentCard;

import java.time.YearMonth;

/**
 * A payment card as typed in a form. Only the holder, brand, last four digits and expiry are kept; the full number is
 * used to check the card and then dropped.
 *
 * @param holderName  the name on the card
 * @param number      the card number, spaces and dashes allowed
 * @param expiryMonth expiry month, 1 to 12
 * @param expiryYear  expiry year, four digits
 */
public record CardForm(String holderName, String number, int expiryMonth, int expiryYear) {

    /**
     * Builds the model value.
     *
     * @param now the current month, to reject an expired card
     * @return the card, without its number
     * @throws IllegalArgumentException with a model key if a value is invalid or the card has expired
     */
    PaymentCard toCard(YearMonth now) {
        return PaymentCard.of(holderName, number, expiryMonth, expiryYear, now);
    }

    /** Leaves the card number out, so it never reaches a log or a stack trace. */
    @Override
    public String toString() {
        return "CardForm[holderName=" + holderName + ", number=****, expiry=" + expiryMonth + "/" + expiryYear + "]";
    }
}
