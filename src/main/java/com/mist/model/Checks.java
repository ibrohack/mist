package com.mist.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Invariant checks shared by the model. Each failure throws an exception whose message is a {@link ModelKeys} key.
 */
final class Checks {

    /** Not instantiable: a holder of the model checks. */
    private Checks() {
    }

    /**
     * Rejects an argument that breaks a rule.
     *
     * @param condition the rule
     * @param key       the bundle key of the violation
     * @throws IllegalArgumentException if the rule is broken
     */
    static void require(boolean condition, String key) {
        if (!condition) {
            throw new IllegalArgumentException(key);
        }
    }

    /**
     * Rejects an action the current state does not allow.
     *
     * @param condition the rule
     * @param key       the bundle key of the violation
     * @throws IllegalStateException if the rule is broken
     */
    static void requireState(boolean condition, String key) {
        if (!condition) {
            throw new IllegalStateException(key);
        }
    }

    /**
     * Requires a value.
     *
     * @param value the value
     * @param key   the bundle key of the violation
     * @param <T>   the value type
     * @return the value
     * @throws IllegalArgumentException if it is {@code null}
     */
    static <T> T present(T value, String key) {
        require(value != null, key);
        return value;
    }

    /**
     * Returns the stripped text, which must have between {@code min} and {@code max} characters.
     *
     * @param value the text
     * @param min   the fewest characters
     * @param max   the most characters
     * @param key   the bundle key of the violation
     * @return the text without surrounding spaces
     * @throws IllegalArgumentException if it is missing or its length is out of range
     */
    static String text(String value, int min, int max, String key) {
        String stripped = value == null ? null : value.strip();
        require(stripped != null && stripped.length() >= min && stripped.length() <= max, key);
        return stripped;
    }

    /**
     * Returns the stripped text, or {@code null} when it is missing or blank.
     *
     * @param value the text, possibly {@code null}
     * @param max   the most characters
     * @param key   the bundle key of the violation
     * @return the text without surrounding spaces, or {@code null}
     * @throws IllegalArgumentException if it is too long
     */
    static String optionalText(String value, int max, String key) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String stripped = value.strip();
        require(stripped.length() <= max, key);
        return stripped;
    }

    /**
     * Requires a text that matches a pattern.
     *
     * @param value   the text
     * @param pattern the pattern
     * @param key     the bundle key of the violation
     * @return the text
     * @throws IllegalArgumentException if it does not match
     */
    static String matching(String value, Pattern pattern, String key) {
        require(value != null && pattern.matcher(value).matches(), key);
        return value;
    }

    /**
     * Returns the amount at scale 2, which must be zero or positive.
     *
     * @param amount the amount
     * @param key    the bundle key of the violation
     * @return the amount rounded half up to cents
     * @throws IllegalArgumentException if it is missing or negative
     */
    static BigDecimal money(BigDecimal amount, String key) {
        require(amount != null && amount.signum() >= 0, key);
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
