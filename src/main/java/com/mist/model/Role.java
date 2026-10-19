package com.mist.model;

/**
 * What a user may do in MIST; the sidebar shows only the areas the role allows.
 */
public enum Role {

    /** Buys games, builds a library and reviews the games in it. */
    CLIENT,

    /** Publishes and manages games for a developer, and reads their sales statistics. */
    DEVELOPER,

    /** Moderates users and reviews, and reads the database operation log. */
    ADMIN
}
