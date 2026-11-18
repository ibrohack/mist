package com.mist.ui.support;

import com.mist.model.GameStats;
import com.mist.ui.I18n;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Currency;
import java.util.Locale;

/**
 * Formats values for the screen in the current language: money in euros, dates, ratings and enum labels.
 *
 * <p>Every method reads {@link I18n#locale()} when called, so a language switch changes the formats at once.</p>
 */
public final class Formats {

    /** The currency every price in MIST is in. */
    private static final Currency EURO = Currency.getInstance("EUR");

    /** Not instantiable: a holder of formatters. */
    private Formats() {
    }

    /**
     * Formats an amount of money, such as {@code 19,99 €} or {@code €19.99}.
     *
     * @param amount the amount, or {@code null}
     * @return the formatted amount, or an empty text for {@code null}
     */
    public static String money(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(I18n.locale());
        format.setCurrency(EURO);
        return format.format(amount);
    }

    /**
     * Formats a game's price, showing "Free" for zero.
     *
     * @param price the price
     * @return the formatted price
     */
    public static String price(BigDecimal price) {
        return price != null && price.signum() == 0 ? I18n.text("price.free") : money(price);
    }

    /**
     * Formats an instant as a date in the machine's time zone.
     *
     * @param instant the instant, or {@code null}
     * @return the date, such as {@code 18 nov 2026}, or an empty text
     */
    public static String date(Instant instant) {
        return instant == null ? "" : date(instant.atZone(ZoneId.systemDefault()).toLocalDate());
    }

    /**
     * Formats a date.
     *
     * @param day the date, or {@code null}
     * @return the formatted date, or an empty text
     */
    public static String date(LocalDate day) {
        return day == null ? "" : DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale()).format(day);
    }

    /**
     * Formats an instant as a date and time in the machine's time zone.
     *
     * @param instant the instant, or {@code null}
     * @return the date and time, or an empty text
     */
    public static String dateTime(Instant instant) {
        return instant == null ? "" : DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale()).format(instant.atZone(ZoneId.systemDefault()));
    }

    /**
     * Formats the time of a log entry.
     *
     * @param time the local date and time
     * @return the time, as {@code HH:mm:ss}
     */
    public static String time(LocalDateTime time) {
        return time == null ? "" : DateTimeFormatter.ofPattern("HH:mm:ss").format(time);
    }

    /**
     * Formats a game's average rating out of five.
     *
     * @param stats the game's stats
     * @return the average with one decimal, such as {@code 4,3}, or a dash when nobody rated it
     */
    public static String rating(GameStats stats) {
        if (stats == null || stats.ratingCount() == 0) {
            return "—";
        }
        NumberFormat format = NumberFormat.getNumberInstance(I18n.locale());
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        return format.format((double) stats.ratingSum() / stats.ratingCount());
    }

    /**
     * Formats a whole number with the language's grouping, such as {@code 1.204} or {@code 1,204}.
     *
     * @param number the number
     * @return the formatted number
     */
    public static String count(long number) {
        return NumberFormat.getIntegerInstance(I18n.locale()).format(number);
    }

    /**
     * Returns the label of an enum constant from the bundle key {@code enum.<Type>.<CONSTANT>}.
     *
     * @param value the constant, or {@code null}
     * @return its label, or an empty text
     */
    public static String label(Enum<?> value) {
        if (value == null) {
            return "";
        }
        return I18n.textOrKey("enum." + value.getDeclaringClass().getSimpleName() + "." + value.name());
    }

    /**
     * Returns the locale formats use.
     *
     * @return the current UI locale
     */
    private static Locale locale() {
        return I18n.locale();
    }
}
