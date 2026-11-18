package com.mist.ui;

import java.util.Locale;

/**
 * The screens of MIST, each backed by one FXML file.
 *
 * <p>A screen either fills the window (splash, login...) or lives inside the shell, under its header. Its name in
 * lower case is its help topic.</p>
 */
public enum View {

    /** First screen: the brand art, the tagline and a loading bar while MIST connects. */
    SPLASH("view/splash.fxml", Placement.WINDOW);

    /** Where a screen is shown. */
    public enum Placement {
        /** The screen replaces the whole window content. */
        WINDOW,
        /** The screen is shown inside the shell, below the header. */
        SHELL
    }

    /** FXML path, relative to {@code com/mist/ui/}. */
    private final String fxml;
    /** Where the screen is shown. */
    private final Placement placement;

    View(String fxml, Placement placement) {
        this.fxml = fxml;
        this.placement = placement;
    }

    /**
     * Returns where the view's FXML file lives.
     *
     * @return the FXML path, relative to {@code com/mist/ui/}
     */
    public String fxml() {
        return fxml;
    }

    /**
     * Tells whether the screen lives inside the shell.
     *
     * @return {@code true} for screens shown under the header
     */
    public boolean inShell() {
        return placement == Placement.SHELL;
    }

    /**
     * Returns the help topic of the screen, the prefix of its {@code help.<topic>.*} bundle keys.
     *
     * @return the topic, such as {@code login}
     */
    public String helpTopic() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
