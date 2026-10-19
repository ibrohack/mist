package com.mist.ui;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Checks that the Spanish and English bundles stay in step and load regardless of the machine's locale.
 */
class I18nTest {

    @Test
    void bundlesHaveTheSameKeys() {
        ResourceBundle es = I18n.bundle(I18n.DEFAULT_LOCALE);
        ResourceBundle en = I18n.bundle(Locale.ENGLISH);

        assertEquals(es.keySet(), en.keySet());
    }

    @Test
    void noValueIsBlank() {
        for (Locale locale : new Locale[] {I18n.DEFAULT_LOCALE, Locale.ENGLISH}) {
            ResourceBundle bundle = I18n.bundle(locale);
            for (String key : bundle.keySet()) {
                assertFalse(bundle.getString(key).isBlank(), () -> key + " is blank in " + locale);
            }
        }
    }

    @Test
    void spanishIsDefaultAndKeepsAccents() {
        assertEquals("PREPARANDO TU PRÓXIMO MUNDO", I18n.bundle(I18n.DEFAULT_LOCALE).getString("splash.tagline"));
    }

    @Test
    void englishResolves() {
        assertEquals("PREPARING YOUR NEXT WORLD", I18n.bundle(Locale.ENGLISH).getString("splash.tagline"));
    }

    @Test
    void currentLocaleStartsAsSpanish() {
        assertEquals(I18n.DEFAULT_LOCALE, I18n.locale());
        assertEquals("PREPARANDO TU PRÓXIMO MUNDO", I18n.bundle().getString("splash.tagline"));
    }
}
