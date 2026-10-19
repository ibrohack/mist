package com.mist.ui;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Loads the message bundles that hold every text of the UI, in Spanish or English.
 *
 * <p>Bundles never fall back to the machine's default locale, so the language on screen depends only on the locale
 * MIST asks for.</p>
 */
public final class I18n {

    /** Language MIST starts in. */
    public static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("es");

    private static final String BASE_NAME = "i18n.messages";
    private static final ResourceBundle.Control NO_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    private static Locale locale = DEFAULT_LOCALE;

    private I18n() {
    }

    /**
     * Returns the language the UI is shown in.
     *
     * @return the current locale
     */
    public static Locale locale() {
        return locale;
    }

    /**
     * Returns the messages in the current language.
     *
     * @return the bundle for {@link #locale()}
     */
    public static ResourceBundle bundle() {
        return bundle(locale);
    }

    /**
     * Returns the messages in the given language.
     *
     * @param locale the language to load
     * @return the bundle for {@code locale}
     */
    public static ResourceBundle bundle(Locale locale) {
        return ResourceBundle.getBundle(BASE_NAME, locale, NO_FALLBACK);
    }
}
