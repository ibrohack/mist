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
    SPLASH("view/splash.fxml", Placement.WINDOW),

    /** Shown when MongoDB cannot be reached, with a way to retry. */
    OFFLINE("view/offline.fxml", Placement.WINDOW),

    /** The frame of a session: the header with the user menu, around the in-shell screens. */
    SHELL("view/shell.fxml", Placement.WINDOW),

    /** Login, with the language toggle. */
    LOGIN("view/login.fxml", Placement.WINDOW),

    /** Registration of a player or a developer. */
    REGISTER("view/register.fxml", Placement.WINDOW),

    /** The user's profile: personal data, password, avatar, card or studio, and deactivation. */
    PROFILE("view/profile.fxml", Placement.SHELL),

    /** The admins' viewer of the database operation log. */
    ADMIN_LOG("view/admin-log.fxml", Placement.SHELL),

    /** The store: featured games, new releases, top sellers, free games and the search. */
    STORE("view/store.fxml", Placement.SHELL),

    /** A game's page, opened with the game's id. */
    GAME("view/game.fxml", Placement.SHELL),

    /** The player's cart and the checkout. */
    CART("view/cart.fxml", Placement.SHELL),

    /** The player's wishlist. */
    WISHLIST("view/wishlist.fxml", Placement.SHELL),

    /** The games the player owns. */
    LIBRARY("view/library.fxml", Placement.SHELL),

    /** The player's orders, with returns and receipts. */
    ORDERS("view/orders.fxml", Placement.SHELL),

    /** The developer's dashboard: sales and the editable table of games. */
    STUDIO("view/studio.fxml", Placement.SHELL);

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

    /**
     * Declares a screen.
     *
     * @param fxml      the FXML path, relative to {@code com/mist/ui/}
     * @param placement where the screen is shown
     */
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
