package com.mist.ui;

import com.mist.model.Language;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Loads the message bundles that hold every text of the UI, in Spanish or English, and keeps the language the UI is
 * shown in.
 *
 * <p>Bundles never fall back to the machine's default locale, so the language on screen depends only on the locale
 * MIST asks for.</p>
 */
public final class I18n {

    /** Language MIST starts in. */
    public static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("es");
    /** The English locale, the other language MIST speaks. */
    public static final Locale ENGLISH = Locale.ENGLISH;

    /** Base name of the bundles, {@code i18n/messages_{es,en}.properties}. */
    private static final String BASE_NAME = "i18n.messages";
    /** Bundle lookup without falling back to the machine's locale. */
    private static final ResourceBundle.Control NO_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    /** The language the UI is shown in. */
    private static volatile Locale locale = DEFAULT_LOCALE;

    /** Not instantiable: a holder of the language helper. */
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
     * Switches the UI to another language. Screens loaded afterwards use it; {@link Navigator#reload()} rebuilds the
     * one on screen. The JVM's default locale follows, so JavaFX's own texts (date pickers, tables) match.
     *
     * @param newLocale Spanish or English
     * @throws IllegalArgumentException if MIST has no bundle for that language
     */
    public static void use(Locale newLocale) {
        Locale supported = switch (newLocale.getLanguage()) {
            case "es" -> DEFAULT_LOCALE;
            case "en" -> ENGLISH;
            default -> throw new IllegalArgumentException("MIST does not speak " + newLocale);
        };
        locale = supported;
        Locale.setDefault(supported);
    }

    /**
     * Returns the locale of a language saved in a user's preferences.
     *
     * @param language the saved language
     * @return the matching locale
     */
    public static Locale localeOf(Language language) {
        return language == Language.EN ? ENGLISH : DEFAULT_LOCALE;
    }

    /**
     * Returns the current language as the model stores it in preferences.
     *
     * @return {@link Language#EN} or {@link Language#ES}
     */
    public static Language language() {
        return "en".equals(locale.getLanguage()) ? Language.EN : Language.ES;
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

    /**
     * Returns one message in the current language.
     *
     * @param key the bundle key
     * @return the message
     * @throws java.util.MissingResourceException if the key is not in the bundle
     */
    public static String text(String key) {
        return bundle().getString(key);
    }

    /**
     * Returns one message in the current language with its {@code {0}}, {@code {1}}... placeholders filled.
     *
     * @param key       the bundle key
     * @param arguments the values for the placeholders
     * @return the formatted message
     */
    public static String format(String key, Object... arguments) {
        return new MessageFormat(text(key), locale).format(arguments);
    }

    /**
     * Returns the message for a key if the bundle has it, otherwise the key itself. Used for exception messages,
     * which are bundle keys but could come from a library.
     *
     * @param key a bundle key, or any text
     * @return the message, or {@code key}
     */
    public static String textOrKey(String key) {
        ResourceBundle bundle = bundle();
        return key != null && bundle.containsKey(key) ? bundle.getString(key) : String.valueOf(key);
    }
}
