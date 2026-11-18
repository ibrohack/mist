package com.mist.ui.support;

import com.mist.ui.I18n;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import java.text.Collator;
import java.time.Year;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Fills the combo boxes the forms share: countries by their name in the current language, and card expiry months
 * and years.
 */
public final class Choices {

    /** How many years ahead a card expiry can be chosen. */
    private static final int EXPIRY_YEARS = 16;

    /** Not instantiable: a holder of combo box fillers. */
    private Choices() {
    }

    /**
     * Lists every country, stored as its ISO code and shown by its localized name, sorted by that name.
     *
     * @param box     the combo box
     * @param initial the ISO code to select, or {@code null}
     */
    public static void countries(ComboBox<String> box, String initial) {
        Locale locale = I18n.locale();
        Collator collator = Collator.getInstance(locale);
        List<String> codes = Arrays.stream(Locale.getISOCountries())
                .sorted(Comparator.comparing(code -> countryName(code), collator))
                .toList();
        box.getItems().setAll(codes);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(String code) {
                return code == null ? "" : countryName(code);
            }

            @Override
            public String fromString(String text) {
                return codes.stream().filter(code -> countryName(code).equals(text)).findFirst().orElse(null);
            }
        });
        box.setValue(initial);
    }

    /**
     * Returns a country's name in the current language.
     *
     * @param code the ISO 3166 code, such as {@code ES}
     * @return the name, such as {@code España} or {@code Spain}
     */
    public static String countryName(String code) {
        return Locale.of("", code).getDisplayCountry(I18n.locale());
    }

    /**
     * Lists the months 1 to 12, shown with two digits.
     *
     * @param box the combo box
     */
    public static void months(ComboBox<Integer> box) {
        box.getItems().setAll(IntStream.rangeClosed(1, 12).boxed().toList());
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer month) {
                return month == null ? "" : String.format("%02d", month);
            }

            @Override
            public Integer fromString(String text) {
                return text == null || text.isBlank() ? null : Integer.valueOf(text.strip());
            }
        });
    }

    /**
     * Lists the years a card can expire in, from this year on.
     *
     * @param box the combo box
     */
    public static void expiryYears(ComboBox<Integer> box) {
        int now = Year.now().getValue();
        box.getItems().setAll(IntStream.range(now, now + EXPIRY_YEARS).boxed().toList());
    }
}
