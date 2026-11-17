package com.mist.model;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Struct;

import java.time.YearMonth;
import java.util.regex.Pattern;

/**
 * A payment card, stored without its number: only the holder, the brand, the last four digits and the expiry.
 *
 * <p>MIST simulates the charge, so the full number and the CVV are never needed after {@link #of} has checked them,
 * and they are never stored. Embedded in {@code users} (a player's saved card) and in {@code orders} (the card an
 * order was paid with).</p>
 *
 * @param holderName  name printed on the card
 * @param brand       brand detected from the number
 * @param last4       last four digits of the number
 * @param expiryMonth expiry month, 1 to 12
 * @param expiryYear  four-digit expiry year
 */
@Embeddable
@Struct(name = "PaymentCard")
public record PaymentCard(String holderName, CardBrand brand, String last4, int expiryMonth, int expiryYear) {

    /** Longest holder name accepted. */
    public static final int MAX_HOLDER_LENGTH = 80;
    /** Earliest expiry year accepted. */
    public static final int MIN_EXPIRY_YEAR = 2000;
    /** Latest expiry year accepted. */
    public static final int MAX_EXPIRY_YEAR = 2099;

    private static final Pattern NUMBER = Pattern.compile("[0-9]{12,19}");
    private static final Pattern LAST4 = Pattern.compile("[0-9]{4}");

    /**
     * Checks the stored parts of a card.
     *
     * @throws IllegalArgumentException with a {@link ModelKeys} key if a part is invalid
     */
    public PaymentCard {
        holderName = Checks.text(holderName, 1, MAX_HOLDER_LENGTH, ModelKeys.CARD_HOLDER);
        Checks.present(brand, ModelKeys.CARD_NUMBER);
        Checks.matching(last4, LAST4, ModelKeys.CARD_NUMBER);
        Checks.require(expiryMonth >= 1 && expiryMonth <= 12
                && expiryYear >= MIN_EXPIRY_YEAR && expiryYear <= MAX_EXPIRY_YEAR, ModelKeys.CARD_EXPIRY);
    }

    /**
     * Checks a card as the player typed it and keeps only what MIST stores.
     *
     * @param holderName  name printed on the card
     * @param number      card number; spaces and dashes are ignored
     * @param expiryMonth expiry month, 1 to 12
     * @param expiryYear  four-digit expiry year
     * @param now         the current month, to reject expired cards
     * @return the card, without its number
     * @throws IllegalArgumentException with {@link ModelKeys#CARD_NUMBER}, {@link ModelKeys#CARD_EXPIRY},
     *                                  {@link ModelKeys#CARD_EXPIRED} or {@link ModelKeys#CARD_HOLDER}
     */
    public static PaymentCard of(String holderName, String number, int expiryMonth, int expiryYear, YearMonth now) {
        String digits = number == null ? "" : number.replaceAll("[\\s-]", "");
        Checks.require(NUMBER.matcher(digits).matches() && passesLuhn(digits), ModelKeys.CARD_NUMBER);
        PaymentCard card = new PaymentCard(holderName, brandOf(digits), digits.substring(digits.length() - 4),
                expiryMonth, expiryYear);
        Checks.require(!card.isExpired(now), ModelKeys.CARD_EXPIRED);
        return card;
    }

    /**
     * Tells whether the card expired before the given month.
     *
     * @param now the current month
     * @return {@code true} if the expiry month is before {@code now}
     */
    public boolean isExpired(YearMonth now) {
        return YearMonth.of(expiryYear, expiryMonth).isBefore(now);
    }

    /**
     * Returns the card as it may be shown on screen or on a receipt.
     *
     * @return the last four digits behind a mask, such as {@code •••• 4242}
     */
    public String masked() {
        return "•••• " + last4;
    }

    private static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubled = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubled) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubled = !doubled;
        }
        return sum % 10 == 0;
    }

    private static CardBrand brandOf(String digits) {
        if (digits.startsWith("34") || digits.startsWith("37")) {
            return CardBrand.AMEX;
        }
        if (digits.startsWith("4")) {
            return CardBrand.VISA;
        }
        int firstTwo = Integer.parseInt(digits.substring(0, 2));
        int firstFour = Integer.parseInt(digits.substring(0, 4));
        if ((firstTwo >= 51 && firstTwo <= 55) || (firstFour >= 2221 && firstFour <= 2720)) {
            return CardBrand.MASTERCARD;
        }
        return CardBrand.OTHER;
    }
}
