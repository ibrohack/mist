package com.mist.model;

/**
 * Publication state of a game.
 */
public enum GameStatus {

    /** Being edited by its developer; not visible in the store. */
    DRAFT,

    /** Visible in the store and available to buy. */
    PUBLISHED,

    /** Removed from the store; owners keep it in their library. */
    DELISTED
}
