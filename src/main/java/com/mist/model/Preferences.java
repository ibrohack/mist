package com.mist.model;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Struct;

import java.util.Objects;

/**
 * A user's interface preferences, chosen on the Profile view. Embedded in {@code users}.
 *
 * @param language language of the interface
 * @param theme    color theme of the interface
 */
@Embeddable
@Struct(name = "Preferences")
public record Preferences(Language language, Theme theme) {

    /** Preferences of a new user: Spanish and the dark theme. */
    public static final Preferences DEFAULTS = new Preferences(Language.ES, Theme.DARK);

    /**
     * Checks that both preferences are set.
     *
     * @throws NullPointerException if a preference is missing
     */
    public Preferences {
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(theme, "theme");
    }

    /**
     * Returns these preferences with another language.
     *
     * @param language the new language
     * @return the changed preferences
     */
    public Preferences withLanguage(Language language) {
        return new Preferences(language, theme);
    }

    /**
     * Returns these preferences with another theme.
     *
     * @param theme the new theme
     * @return the changed preferences
     */
    public Preferences withTheme(Theme theme) {
        return new Preferences(language, theme);
    }
}
