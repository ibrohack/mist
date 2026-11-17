package com.mist.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Invariant checks shared by the model. Each failure throws an exception whose message is a {@link ModelKeys} key.
 */
final class Checks {

    private Checks() {
    }

    static void require(boolean condition, String key) {
        if (!condition) {
            throw new IllegalArgumentException(key);
        }
    }

    static void requireState(boolean condition, String key) {
        if (!condition) {
            throw new IllegalStateException(key);
        }
    }

    static <T> T present(T value, String key) {
        require(value != null, key);
        return value;
    }

    /** Returns the stripped text, which must have between {@code min} and {@code max} characters. */
    static String text(String value, int min, int max, String key) {
        String stripped = value == null ? null : value.strip();
        require(stripped != null && stripped.length() >= min && stripped.length() <= max, key);
        return stripped;
    }

    /** Returns the stripped text, or {@code null} when it is missing or blank. */
    static String optionalText(String value, int max, String key) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String stripped = value.strip();
        require(stripped.length() <= max, key);
        return stripped;
    }

    static String matching(String value, Pattern pattern, String key) {
        require(value != null && pattern.matcher(value).matches(), key);
        return value;
    }

    /** Returns the amount at scale 2, which must be zero or positive. */
    static BigDecimal money(BigDecimal amount, String key) {
        require(amount != null && amount.signum() >= 0, key);
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
