package com.mist.service;

import com.mist.model.Game;

import java.time.Instant;

/**
 * A game in a player's library, as the Library view shows it.
 *
 * @param game       the game, which may have been delisted since
 * @param acquiredAt when the player bought it
 * @param returnable whether it can still be returned for a refund
 */
public record OwnedGame(Game game, Instant acquiredAt, boolean returnable) {
}
