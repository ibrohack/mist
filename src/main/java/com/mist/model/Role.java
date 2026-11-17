package com.mist.model;

/**
 * What a user may do in MIST; the sidebar shows only the areas the role allows.
 *
 * <p>The names are also the discriminator values of the {@code users} collection.</p>
 */
public enum Role {

    /** Buys games, builds a library and reviews the games in it. */
    PLAYER,

    /** Publishes and manages games for a studio, and reads their sales statistics. */
    DEVELOPER,

    /** Moderates users and reviews, and reads the database operation log. */
    ADMIN
}
